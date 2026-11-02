package com.example.weathering;

import com.example.weathering.network.RegistrySyncPayload;
import com.example.weathering.network.WeatheringNetworking;
import com.example.weathering.network.WeatheringSyncPayload;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.StartTracking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatheringMod implements ModInitializer {
   public static final String MOD_ID = "skin_weathering";
   public static final Logger LOGGER = LoggerFactory.getLogger("skin_weathering");
   public static final SimpleParticleType BLOOD_DRIP_PARTICLE = FabricParticleTypes.simple();

   public void onInitialize() {
      LOGGER.info("Adaptive Skin Weathering initialized with Dynamic Settings (26.2).");
      PayloadTypeRegistry.playS2C().register(WeatheringSyncPayload.TYPE, WeatheringSyncPayload.STREAM_CODEC);
      PayloadTypeRegistry.playS2C().register(RegistrySyncPayload.TYPE, RegistrySyncPayload.STREAM_CODEC);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("skin_weathering", "blood_drip"), BLOOD_DRIP_PARTICLE);
      WeatheringServerTicker.register();
      ServerPlayerEvents.AFTER_RESPAWN.register((AfterRespawn)(oldPlayer, newPlayer, alive) -> {
         if (!alive) {
            UUID uuid = newPlayer.getUUID();
            WeatheringDataManager.clear(uuid);
            WeatheringServerTicker.cleanupPlayer(uuid);
            WeatheringNetworking.syncToClients(newPlayer);
         }
      });
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         UUID uuid = handler.getPlayer().getUUID();
         WeatheringDataManager.clear(uuid);
         WeatheringServerTicker.cleanupPlayer(uuid);
      });
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> WeatheringNetworking.sendRegistrySync(handler.getPlayer()));
      EntityTrackingEvents.START_TRACKING.register((StartTracking)(entity, player) -> {
         if (entity instanceof LivingEntity livingEntity) {
            WeatheringNetworking.syncToClients(livingEntity);
         }
      });
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, world) -> {
         WeatheringConfig config = WeatheringConfig.get();
         if (config.swampZombiesSpawnDirty && entity instanceof Zombie zombie) {
            BlockPos pos = zombie.blockPosition();
            Holder<Biome> biomeEntry = world.getBiome(pos);
            Identifier id = biomeEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (id != null) {
               String path = id.getPath().toLowerCase();
               if (path.contains("swamp") || path.contains("mangrove")) {
                  UUID uuid = zombie.getUUID();
                  WeatheringDataManager.setDirt(uuid, config.swampZombiesSpawnDirtLevel);
                  WeatheringDataManager.setDirtType(uuid, 0);
                  WeatheringDataManager.setDirtMaskIndex(uuid, ThreadLocalRandom.current().nextInt(9999) + 1);
                  WeatheringNetworking.syncToClients(zombie);
               }
            }
         }
      });
      UseItemCallback.EVENT
         .register(
            (UseItemCallback)(player, world, hand) -> {
               ItemStack stack = player.getItemInHand(hand);
               String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
               if (!itemId.equals("supplementaries:soap")) {
                  return InteractionResult.PASS;
               } else {
                  if (!world.isClientSide()) {
                     UUID uuid = player.getUUID();
                     boolean needsCleaning = WeatheringDataManager.getDirt(uuid) > 0.02F
                        || WeatheringDataManager.getBlood(uuid) > 0.02F
                        || WeatheringDataManager.getCharred(uuid) > 0.02F;
                     if (WeatheringServerTicker.WASHING_TIMERS.getOrDefault(uuid, 0) > 0) {
                        return InteractionResult.PASS;
                     }

                     if (needsCleaning) {
                        WeatheringServerTicker.WASHING_TIMERS.put(uuid, 60);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0F, 1.1F);
                        if (!player.getAbilities().instabuild && WeatheringConfig.get().consumeSoap) {
                           stack.shrink(1);
                        }
                     }
                  }

                  return InteractionResult.SUCCESS;
               }
            }
         );
      ServerLivingEntityEvents.ALLOW_DAMAGE.register((AllowDamage)(entity, source, amount) -> {
         WeatheringConfig config = WeatheringConfig.get();
         if (isPhysicalDamage(source)) {
            if (config.skeletonImmunity && entity instanceof AbstractSkeleton) {
               return true;
            }

            UUID uuid = entity.getUUID();
            float currentBlood = WeatheringDataManager.getBlood(uuid);
            float damageRatio = amount / entity.getMaxHealth();
            float bloodAdded = damageRatio * config.bloodDamageMultiplier;
            if (entity instanceof Zombie) {
               bloodAdded *= config.zombieBloodAccumulationMultiplier;
            }

            WeatheringDataManager.setBlood(uuid, currentBlood + bloodAdded);
            if (currentBlood <= 0.0F && bloodAdded > 0.0F) {
               int randomBloodIndex = ThreadLocalRandom.current().nextInt(9999) + 1;
               WeatheringDataManager.setBloodMaskIndex(uuid, randomBloodIndex);
            }

            WeatheringNetworking.syncToClients(entity);
         }

         return true;
      });
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                       "weathering"
                                    )
                                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                                 .then(
                                    Commands.literal("dirt")
                                       .executes(
                                          context -> {
                                             ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                                             if (player == null) {
                                                return 0;
                                             } else {
                                                UUID uuid = player.getUUID();
                                                WeatheringDataManager.setDirt(uuid, 1.0F);
                                                int randomDirtIndex = ThreadLocalRandom.current().nextInt(9999) + 1;
                                                WeatheringDataManager.setDirtMaskIndex(uuid, randomDirtIndex);
                                                WeatheringNetworking.syncToClients(player);
                                                ((CommandSourceStack)context.getSource())
                                                   .sendSuccess(
                                                      () -> Component.literal("Вы полностью покрылись грязью! (Выбран сид #" + randomDirtIndex + ")"), false
                                                   );
                                                return 1;
                                             }
                                          }
                                       )
                                 ))
                              .then(
                                 Commands.literal("blood")
                                    .executes(
                                       context -> {
                                          ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                                          if (player == null) {
                                             return 0;
                                          } else {
                                             UUID uuid = player.getUUID();
                                             WeatheringDataManager.setBlood(uuid, 1.0F);
                                             int randomBloodIndex = ThreadLocalRandom.current().nextInt(9999) + 1;
                                             WeatheringDataManager.setBloodMaskIndex(uuid, randomBloodIndex);
                                             WeatheringNetworking.syncToClients(player);
                                             ((CommandSourceStack)context.getSource())
                                                .sendSuccess(() -> Component.literal("Вы покрылись кровью! (Выбран сид #" + randomBloodIndex + ")"), false);
                                             return 1;
                                          }
                                       }
                                    )
                              ))
                           .then(Commands.literal("clear").executes(context -> {
                              ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                              if (player == null) {
                                 return 0;
                              } else {
                                 UUID uuid = player.getUUID();
                                 WeatheringDataManager.clear(uuid);
                                 WeatheringDataManager.setDirtMaskIndex(uuid, 0);
                                 WeatheringDataManager.setBloodMaskIndex(uuid, 0);
                                 WeatheringDataManager.setWetMaskIndex(uuid, 0);
                                 WeatheringDataManager.setCharMaskIndex(uuid, 0);
                                 WeatheringNetworking.syncToClients(player);
                                 ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Ваш персонаж очищен!"), false);
                                 return 1;
                              }
                           })))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("shake").then(Commands.literal("enable").executes(context -> {
                                 ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                                 if (player == null) {
                                    return 0;
                                 } else {
                                    UUID uuid = player.getUUID();
                                    WeatheringDataManager.setShakeDisabled(uuid, false);
                                    WeatheringNetworking.syncToClients(player);
                                    ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Тряска включена!"), false);
                                    return 1;
                                 }
                              }))).then(Commands.literal("disable").executes(context -> {
                                 ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                                 if (player == null) {
                                    return 0;
                                 } else {
                                    UUID uuid = player.getUUID();
                                    WeatheringDataManager.setShakeDisabled(uuid, true);
                                    WeatheringNetworking.syncToClients(player);
                                    ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Тряска отключена!"), false);
                                    return 1;
                                 }
                              })))
                              .then(
                                 Commands.argument("value", FloatArgumentType.floatArg(-1.0F, 1.0F))
                                    .executes(
                                       context -> {
                                          ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                                          if (player == null) {
                                             return 0;
                                          } else {
                                             float val = FloatArgumentType.getFloat(context, "value");
                                             UUID uuid = player.getUUID();
                                             WeatheringDataManager.setShakeIntensity(uuid, val);
                                             WeatheringNetworking.syncToClients(player);
                                             if (val < 0.0F) {
                                                ((CommandSourceStack)context.getSource())
                                                   .sendSuccess(() -> Component.literal("Тряска возвращена в автоматический режим (от холода)"), false);
                                             } else {
                                                ((CommandSourceStack)context.getSource())
                                                   .sendSuccess(
                                                      () -> Component.literal("Интенсивность тряски установлена на " + String.format("%.2f", val)), false
                                                   );
                                             }

                                             return 1;
                                          }
                                       }
                                    )
                              )
                        ))
                     .then(((LiteralArgumentBuilder)Commands.literal("breath").then(Commands.literal("enable").executes(context -> {
                        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                        if (player == null) {
                           return 0;
                        } else {
                           UUID uuid = player.getUUID();
                           WeatheringDataManager.setBreathingDisabled(uuid, false);
                           WeatheringNetworking.syncToClients(player);
                           ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Дыхание включено!"), false);
                           return 1;
                        }
                     }))).then(Commands.literal("disable").executes(context -> {
                        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
                        if (player == null) {
                           return 0;
                        } else {
                           UUID uuid = player.getUUID();
                           WeatheringDataManager.setBreathingDisabled(uuid, true);
                           WeatheringNetworking.syncToClients(player);
                           ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Дыхание отключено!"), false);
                           return 1;
                        }
                     }))))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("rate").executes(context -> {
                              float currentRate = WeatheringConfig.get().dirtAccumulationRate;
                              ((CommandSourceStack)context.getSource())
                                 .sendSuccess(() -> Component.literal("Текущий множитель скорости загрязнения: " + currentRate), false);
                              return 1;
                           }))
                           .then(
                              Commands.literal("default")
                                 .executes(
                                    context -> {
                                       WeatheringConfig.get().dirtAccumulationRate = 2.0F;
                                       WeatheringConfig.get().save();
                                       ((CommandSourceStack)context.getSource())
                                          .sendSuccess(() -> Component.literal("Множитель скорости загрязнения сброшен на стандартное значение (2.0)"), false);
                                       return 1;
                                    }
                                 )
                           ))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 1000.0F)).executes(context -> {
                           float val = FloatArgumentType.getFloat(context, "value");
                           WeatheringConfig.get().dirtAccumulationRate = val;
                           WeatheringConfig.get().save();
                           ((CommandSourceStack)context.getSource())
                              .sendSuccess(() -> Component.literal("Множитель скорости загрязнения установлен на: " + val), false);
                           return 1;
                        }))
                  )
            )
         );
   }

   private static boolean isPhysicalDamage(DamageSource source) {
      return !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.STARVE) && !source.is(DamageTypes.DROWN) && !source.is(DamageTypes.FREEZE);
   }
}
