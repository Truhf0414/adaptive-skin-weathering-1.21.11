package com.example.weathering;

import com.example.weathering.network.WeatheringNetworking;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WeatheringServerTicker {
   private static final ConcurrentHashMap<UUID, Vec3> LAST_POSITIONS = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<UUID, Integer> SYNC_COOLDOWNS = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<UUID, Boolean> NEEDS_SYNC = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Integer> WASHING_TIMERS = new ConcurrentHashMap<>();

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            if (!LAST_POSITIONS.containsKey(uuid)) {
               LAST_POSITIONS.put(uuid, player.position());
               SYNC_COOLDOWNS.put(uuid, 0);
               NEEDS_SYNC.put(uuid, false);
            } else {
               updateWeathering(player, uuid);
               int cooldown = SYNC_COOLDOWNS.getOrDefault(uuid, 0);
               if (cooldown > 0) {
                  SYNC_COOLDOWNS.put(uuid, cooldown - 1);
               }

               boolean shouldSync = NEEDS_SYNC.getOrDefault(uuid, false);
               if (shouldSync && cooldown <= 0) {
                  WeatheringNetworking.syncToClients(player);
                  NEEDS_SYNC.put(uuid, false);
                  SYNC_COOLDOWNS.put(uuid, 20);
               }
            }
         }
      });
   }

   private static void updateWeathering(ServerPlayer player, UUID uuid) {
      WeatheringConfig config = WeatheringConfig.get();
      float[] oldLayers = new float[4];
      float[] newLayers = new float[4];

      for (int i = 0; i < 4; i++) {
         oldLayers[i] = WeatheringDataManager.getDirtLayer(uuid, i);
         newLayers[i] = oldLayers[i];
      }

      float oldBlood = WeatheringDataManager.getBlood(uuid);
      float oldWet = WeatheringDataManager.getWetness(uuid);
      float oldChar = WeatheringDataManager.getCharred(uuid);
      float oldFrost = WeatheringDataManager.getFrost(uuid);
      float newBlood = oldBlood;
      Vec3 lastPos = LAST_POSITIONS.get(uuid);
      Vec3 currentPos = player.position();
      if (lastPos == null) {
         LAST_POSITIONS.put(uuid, currentPos);
      } else {
         double deltaX = currentPos.x - lastPos.x;
         double deltaZ = currentPos.z - lastPos.z;
         double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
         boolean isMoving = distanceSquared > 1.0E-4 && !player.isSpectator() && !player.getAbilities().flying;
         BlockPos pos = player.blockPosition();
         Holder<Biome> biome = player.level().getBiome(pos);
         boolean isCold = ((Biome)biome.value()).coldEnoughToSnow(pos, player.level().getSeaLevel());
         boolean inWater = player.isInWater() || player.level().getFluidState(pos).is(FluidTags.WATER);
         int washTicks = WASHING_TIMERS.getOrDefault(uuid, 0);
         int resolvedType = -1;
         float newWet;
         float newChar;
         float newFrost;
         if (washTicks > 0) {
            WASHING_TIMERS.put(uuid, washTicks - 1);

            for (int i = 0; i < 4; i++) {
               newLayers[i] = Math.max(0.0F, oldLayers[i] - 0.017F);
            }

            newBlood = Math.max(0.0F, oldBlood - 0.017F);
            newChar = Math.max(0.0F, oldChar - 0.017F);
            newFrost = Math.max(0.0F, oldFrost - 0.03F);
            newWet = Math.min(1.0F, oldWet + 0.02F);
            ServerLevel steppingBlock = player.level();
            ServerLevel serverWorld = steppingBlock instanceof ServerLevel ? steppingBlock : null;
            if (serverWorld != null) {
               if (player.getRandom().nextFloat() < 0.35F) {
                  serverWorld.sendParticles(
                     ParticleTypes.WAX_ON,
                     player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                     player.getY() + 0.3 + player.getRandom().nextDouble() * 1.0,
                     player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                     1,
                     0.0,
                     0.0,
                     0.0,
                     0.0
                  );
               }

               if (player.getRandom().nextFloat() < 0.25F) {
                  serverWorld.sendParticles(
                     ParticleTypes.SPLASH,
                     player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.4,
                     player.getY() + 0.5 + player.getRandom().nextDouble() * 0.8,
                     player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.4,
                     1,
                     0.0,
                     0.0,
                     0.0,
                     0.0
                  );
               }

               if (player.getRandom().nextFloat() < 0.2F) {
                  serverWorld.sendParticles(
                     ParticleTypes.GLOW,
                     player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                     player.getY() + 0.2 + player.getRandom().nextDouble() * 1.2,
                     player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                     1,
                     0.0,
                     0.0,
                     0.0,
                     0.0
                  );
               }

               if (washTicks % 8 == 0) {
                  serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WET_GRASS_HIT, SoundSource.PLAYERS, 0.45F, 1.3F);
               }

               if (washTicks % 12 == 0) {
                  serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.25F, 1.5F);
               }
            }

            if (WeatheringDataManager.getDirt(uuid) <= 0.0F && newBlood <= 0.0F && newChar <= 0.0F && washTicks < 15) {
               WASHING_TIMERS.remove(uuid);
            }
         } else {
            if (player.isOnFire()) {
               newWet = 0.0F;
               newChar = Math.min(1.0F, oldChar + config.sootAccumulationRate);
               newFrost = 0.0F;
            } else {
               if (inWater) {
                  newChar = Math.max(0.0F, oldChar - config.sootWaterWashRate);
               } else if (player.level().isRaining() && player.level().canSeeSky(pos)) {
                  newChar = Math.max(0.0F, oldChar - config.sootRainWashRate);
               } else {
                  newChar = Math.max(0.0F, oldChar - config.sootNaturalDispersalRate / 1000000.0F);
               }

               if (inWater) {
                  newWet = Math.min(1.0F, oldWet + 0.05F);
               } else if (player.level().isRaining() && player.level().canSeeSky(pos)) {
                  newWet = Math.min(1.0F, oldWet + 0.02F);
               } else {
                  newWet = Math.max(0.0F, oldWet - config.wetnessDryRate / 1000000.0F);
               }

               if (inWater) {
                  newFrost = Math.max(0.0F, oldFrost - config.frostMeltingWaterRate / 1000000.0F);
               } else if (player.getTicksFrozen() > 0) {
                  newFrost = Math.min(1.0F, oldFrost + config.frostFreezeSnowRate / 1000000.0F);
               } else if (isCold) {
                  if (player.level().isRaining() && player.level().canSeeSky(pos)) {
                     newFrost = Math.min(1.0F, oldFrost + config.frostStormRate / 1000000.0F + 0.001F);
                  } else {
                     newFrost = Math.min(1.0F, oldFrost + 5.0E-4F);
                  }
               } else {
                  float meltRate = config.frostMeltingWarmRate / 1000000.0F;
                  newFrost = Math.max(0.0F, oldFrost - meltRate);
                  if (oldFrost > 0.0F) {
                     newWet = Math.min(1.0F, newWet + meltRate * 0.5F);
                  }

                  if (config.enableLayering) {
                     newLayers[3] = Math.max(0.0F, oldLayers[3] - 0.002F);
                  } else if (WeatheringDataManager.getDirtType(uuid) == 3) {
                     newLayers[0] = Math.max(0.0F, oldLayers[0] - 0.002F);
                  }
               }
            }

            if (inWater) {
               for (int i = 0; i < 4; i++) {
                  newLayers[i] = Math.max(0.0F, oldLayers[i] - config.waterDirtWashRate / 1000000.0F);
               }

               newBlood = Math.max(0.0F, oldBlood - config.waterBloodWashRate / 1000000.0F);
            } else if (player.level().isRaining() && player.level().canSeeSky(pos) && !isCold) {
               for (int i = 0; i < 4; i++) {
                  newLayers[i] = Math.max(0.0F, oldLayers[i] - config.rainDirtWashRate / 1000000.0F);
               }

               newBlood = Math.max(0.0F, oldBlood - config.rainBloodWashRate / 1000000.0F);
            }
         }

         if (isMoving && !inWater) {
            float speedMultiplier = config.dirtAccumulationRate;
            float factor = (player.isSprinting() ? config.dirtSprintingMultiplier : 1.0F) * speedMultiplier;
            BlockState steppingBlockx = player.getBlockStateOn();
            BlockState feetBlock = player.level().getBlockState(pos);
            resolvedType = getBlockDirtType(feetBlock);
            if (resolvedType == -1 || resolvedType == -2) {
               int stepType = getBlockDirtType(steppingBlockx);
               if (stepType != -1) {
                  resolvedType = stepType;
               }
            }

            if (resolvedType == -2) {
               resolvedType = getBiomeDirtType(player);
               if (player.blockPosition().getY() < 50) {
                  resolvedType = 2;
               }
            }

            if (resolvedType == -3) {
               for (int i = 0; i < 4; i++) {
                  newLayers[i] = Math.max(0.0F, newLayers[i] - config.carpetCleaningRate / 1000000.0F);
               }
            } else if (resolvedType >= 0 && resolvedType < 4) {
               int oldType = WeatheringDataManager.getDirtType(uuid);
               if (oldType != resolvedType) {
                  WeatheringDataManager.setDirtType(uuid, resolvedType);
               }

               Identifier bId = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
               String bPath = bId != null ? bId.getPath().toLowerCase() : "";
               int bType = 0;
               if (bPath.contains("swamp") || bPath.contains("mangrove")) {
                  bType = 1;
               } else if (bPath.contains("jungle")) {
                  bType = 2;
               } else if (bPath.contains("taiga") || bPath.contains("snowy") || bPath.contains("cold") || bPath.contains("frozen") || bPath.contains("ice")) {
                  bType = 3;
               } else if (bPath.contains("badlands") || bPath.contains("mesa")) {
                  bType = 4;
               }

               WeatheringDataManager.setLayerBiomeType(uuid, resolvedType, bType);
               float rate = 0.0F;
               if (resolvedType != 0) {
                  if (resolvedType == 1) {
                     rate = config.dirtSandRate;
                  } else if (resolvedType == 2) {
                     rate = config.dirtStoneDustRate;
                  } else if (resolvedType == 3) {
                     rate = config.dirtSnowIceRate;
                  }
               } else {
                  String blockId = steppingBlockx.getBlock().getDescriptionId().toLowerCase();
                  rate = !blockId.contains("mud") && !blockId.contains("clay") ? config.dirtBaseRate : config.dirtMudClayRate;
               }

               float tickIncrement = rate / 1000000.0F * factor;
               float oldVal = newLayers[resolvedType];
               newLayers[resolvedType] = Math.min(1.0F, oldVal + tickIncrement);
               if (oldVal <= 0.01F && newLayers[resolvedType] > 0.01F) {
                  WeatheringDataManager.promoteLayerToTop(uuid, resolvedType);
               }
            }
         }

         if (newWet < oldWet && newWet < 0.005F) {
            newWet = 0.0F;
         }

         if (newChar < oldChar && newChar < 0.005F) {
            newChar = 0.0F;
         }

         if (newFrost < oldFrost && newFrost < 0.005F) {
            newFrost = 0.0F;
         }

         if (newBlood < oldBlood && newBlood < 0.005F) {
            newBlood = 0.0F;
         }

         for (int i = 0; i < 4; i++) {
            if (newLayers[i] < oldLayers[i] && newLayers[i] < 0.005F) {
               newLayers[i] = 0.0F;
            }
         }

         for (int ix = 0; ix < 4; ix++) {
            if (newLayers[ix] > 0.01F) {
               WeatheringDataManager.getDirtLayerMask(uuid, ix);
            } else if (newLayers[ix] <= 0.0F) {
               WeatheringDataManager.setDirtLayerMask(uuid, ix, 0);
            }
         }

         if (newBlood > 0.01F && !WeatheringDataManager.BLOOD_MASKS.containsKey(uuid)) {
            WeatheringDataManager.getBloodMaskIndex(uuid);
         } else if (newBlood <= 0.0F) {
            WeatheringDataManager.setBloodMaskIndex(uuid, 0);
         }

         if (newWet > 0.01F && !WeatheringDataManager.WET_MASKS.containsKey(uuid)) {
            WeatheringDataManager.getWetMaskIndex(uuid);
         } else if (newWet <= 0.0F) {
            WeatheringDataManager.setWetMaskIndex(uuid, 0);
         }

         if (newChar > 0.01F && !WeatheringDataManager.CHAR_MASKS.containsKey(uuid)) {
            WeatheringDataManager.getCharMaskIndex(uuid);
         } else if (newChar <= 0.0F) {
            WeatheringDataManager.setCharMaskIndex(uuid, 0);
         }

         if (newFrost > 0.01F && !WeatheringDataManager.FROST_MASKS.containsKey(uuid)) {
            WeatheringDataManager.getFrostMaskIndex(uuid);
         } else if (newFrost <= 0.0F) {
            WeatheringDataManager.setFrostMaskIndex(uuid, 0);
         }

         LAST_POSITIONS.put(uuid, currentPos);
         boolean dirtChanged = false;

         for (int ixx = 0; ixx < 4; ixx++) {
            if (oldLayers[ixx] != newLayers[ixx]) {
               dirtChanged = true;
               WeatheringDataManager.setDirtLayer(uuid, ixx, newLayers[ixx]);
            }
         }

         if (dirtChanged || oldBlood != newBlood || oldWet != newWet || oldChar != newChar || oldFrost != newFrost) {
            WeatheringDataManager.setBlood(uuid, newBlood);
            WeatheringDataManager.setWetness(uuid, newWet);
            WeatheringDataManager.setCharred(uuid, newChar);
            WeatheringDataManager.setFrost(uuid, newFrost);
            NEEDS_SYNC.put(uuid, true);
         }
      }
   }

   private static int getBlockDirtType(BlockState state) {
      if (state.isAir()) {
         return -1;
      } else {
         String blockId = state.getBlock().getDescriptionId().toLowerCase();
         if (state.is(BlockTags.PLANKS)
            || state.is(BlockTags.WOOL_CARPETS)
            || blockId.contains("carpet")
            || blockId.contains("wool")
            || blockId.contains("brick")
            || blockId.contains("terracotta")
            || blockId.contains("concrete")
            || blockId.contains("smooth_stone")
            || blockId.contains("glass")
            || blockId.contains("quartz")) {
            return -3;
         } else if (state.is(BlockTags.SNOW) || blockId.contains("snow") || blockId.contains("ice")) {
            return 3;
         } else if (state.is(BlockTags.SAND) || blockId.contains("sand")) {
            return 1;
         } else if (state.is(BlockTags.BASE_STONE_OVERWORLD)
            || state.is(BlockTags.BASE_STONE_NETHER)
            || blockId.contains("stone")
            || blockId.contains("deepslate")
            || blockId.contains("andesite")
            || blockId.contains("diorite")
            || blockId.contains("granite")
            || blockId.contains("ore")
            || blockId.contains("tuff")
            || blockId.contains("basalt")
            || blockId.contains("blackstone")
            || blockId.contains("netherrack")
            || blockId.contains("end_stone")
            || blockId.contains("cobble")
            || blockId.contains("gravel")) {
            return 2;
         } else {
            return !blockId.contains("dirt")
                  && !blockId.contains("grass")
                  && !blockId.contains("mud")
                  && !blockId.contains("clay")
                  && !blockId.contains("podzol")
                  && !blockId.contains("mycelium")
                  && !blockId.contains("farmland")
                  && !blockId.contains("root")
                  && !blockId.contains("moss")
               ? -2
               : 0;
         }
      }
   }

   private static int getBiomeDirtType(ServerPlayer player) {
      Holder<Biome> biomeEntry = player.level().getBiome(player.blockPosition());
      Identifier id = biomeEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
      if (id == null) {
         return 0;
      } else {
         String path = id.getPath().toLowerCase();
         if (path.contains("snow")
            || path.contains("frozen")
            || path.contains("ice")
            || path.contains("cold")
            || path.contains("slopes")
            || path.contains("peaks")
            || path.contains("taiga")) {
            return 3;
         } else {
            return !path.contains("desert") && !path.contains("beach") && !path.contains("badlands") && !path.contains("sand") && !path.contains("savanna")
               ? 0
               : 1;
         }
      }
   }

   public static void cleanupPlayer(UUID uuid) {
      LAST_POSITIONS.remove(uuid);
      SYNC_COOLDOWNS.remove(uuid);
      NEEDS_SYNC.remove(uuid);
      WASHING_TIMERS.remove(uuid);
   }
}
