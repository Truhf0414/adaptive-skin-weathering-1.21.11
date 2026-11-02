package com.example.weathering;

import com.example.weathering.api.SkinEffect;
import com.example.weathering.api.SkinEffectRegistry;
import com.example.weathering.client.BloodDripParticle;
import com.example.weathering.client.WeatheringFeatureRenderer;
import com.example.weathering.network.RegistrySyncPayload;
import com.example.weathering.network.WeatheringSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringModClient implements ClientModInitializer {
   public void onInitializeClient() {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         {
            Objects.requireNonNull(WeatheringModClient.this);
         }

         public Identifier getFabricId() {
            return Identifier.fromNamespaceAndPath("skin_weathering", "masks_loader");
         }

         public void onResourceManagerReload(ResourceManager manager) {
            WeatheringFeatureRenderer.loadMasks(manager);
         }
      });
      ParticleFactoryRegistry.getInstance().register(WeatheringMod.BLOOD_DRIP_PARTICLE, BloodDripParticle.Factory::new);
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (client.level != null) {
            WeatheringConfig config = WeatheringConfig.get();

            for (Player rawPlayer : client.level.players()) {
               if (rawPlayer instanceof AbstractClientPlayer player) {
                  UUID uuid = player.getUUID();
                  boolean isLocalPlayer = client.player != null && client.player.getUUID().equals(uuid);
                  boolean isFirstPerson = client.options != null && client.options.getCameraType().isFirstPerson();
                  boolean shouldHideParticles = isLocalPlayer && isFirstPerson && !config.showFirstPersonParticles;
                  if (!shouldHideParticles) {
                     float wetness = WeatheringDataManager.getWetness(uuid);
                     float dirt = WeatheringDataManager.getDirt(uuid);
                     float blood = WeatheringDataManager.getBlood(uuid);
                     float frost = WeatheringDataManager.getFrost(uuid);
                     float firstPersonDampener = isLocalPlayer && isFirstPerson ? 0.25F : 1.0F;
                     double x;
                     double y;
                     double z;
                     if (isLocalPlayer && isFirstPerson) {
                        float yawRad = (float)Math.toRadians(player.getYRot());
                        double sin = Math.sin(yawRad);
                        double cos = Math.cos(yawRad);
                        double fwdX = -sin;
                        double side = player.getRandom().nextBoolean() ? 1.0 : -1.0;
                        double offsetRight = side * (0.58 + player.getRandom().nextDouble() * 0.2);
                        double offsetForward = 0.22 + player.getRandom().nextDouble() * 0.15;
                        x = player.getX() + cos * offsetRight + fwdX * offsetForward;
                        y = player.getEyeY() - 0.25 - player.getRandom().nextDouble() * 0.3;
                        z = player.getZ() + sin * offsetRight + cos * offsetForward;
                     } else {
                        x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.55;
                        y = player.getEyeY() - 0.2 - player.getRandom().nextDouble() * 0.7;
                        z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.55;
                     }

                     if (wetness > 0.35F && player.getRandom().nextFloat() < wetness * config.wetnessDripChance * firstPersonDampener) {
                        client.level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.0, 0.0, 0.0);
                     }

                     if (dirt > 0.35F && player.getRandom().nextFloat() < dirt * config.dirtDripChance * firstPersonDampener) {
                        int dirtType = WeatheringDataManager.getDirtType(uuid);
                        BlockState state;
                        if (dirtType == 1) {
                           state = Blocks.SAND.defaultBlockState();
                        } else if (dirtType == 2) {
                           state = Blocks.GRAVEL.defaultBlockState();
                        } else if (dirtType == 3) {
                           state = Blocks.SNOW_BLOCK.defaultBlockState();
                        } else {
                           state = Blocks.DIRT.defaultBlockState();
                        }

                        client.level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), x, y, z, 0.0, 0.0, 0.0);
                     }

                     boolean isLowHp = player.getHealth() < player.getMaxHealth() * config.lowHpBleedingThreshold;
                     if (blood > 0.35F || isLowHp) {
                        float dripChance = isLowHp ? config.bloodDripChanceLowHp : blood * blood * config.bloodDripChanceNormal;
                        dripChance *= firstPersonDampener;
                        if (player.getRandom().nextFloat() < dripChance) {
                           client.level.addParticle(WeatheringMod.BLOOD_DRIP_PARTICLE, x, y, z, 0.0, 0.0, 0.0);
                        }
                     }

                     if (frost > 0.35F && player.getRandom().nextFloat() < frost * config.frostDripChance * firstPersonDampener) {
                        client.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, 0.0, 0.0);
                     }
                  }
               }
            }
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(
         WeatheringSyncPayload.TYPE,
         (payload, context) -> {
            int entityId = payload.entityId();
            float[] dirtLayers = payload.dirtLayers();
            float blood = payload.blood();
            float wetness = payload.wetness();
            float charred = payload.charred();
            float frost = payload.frost();
            int dirtType = payload.dirtType();
            int[] dirtMasks = payload.dirtMasks();
            int[] layerBiomes = payload.layerBiomes();
            int[] layerOrder = payload.layerOrder();
            int bloodMaskIndex = payload.bloodMaskIndex();
            int wetMaskIndex = payload.wetMaskIndex();
            int charMaskIndex = payload.charMaskIndex();
            int frostMaskIndex = payload.frostMaskIndex();
            float shakeIntensity = payload.shakeIntensity();
            Map<Identifier, Float> dynamicLevels = new HashMap<>();
            Map<Identifier, Integer> dynamicMasks = new HashMap<>();

            for (WeatheringSyncPayload.DynamicEffectData eff : payload.dynamicEffects()) {
               Identifier effectId = eff.id();
               float val = eff.level();
               int mask = eff.mask();
               if (Float.isNaN(val) || Float.isInfinite(val)) {
                  val = 0.0F;
               }

               val = Math.max(0.0F, Math.min(1.0F, val));
               dynamicLevels.put(effectId, val);
               dynamicMasks.put(effectId, mask);
            }

            for (int i = 0; i < 4; i++) {
               if (Float.isNaN(dirtLayers[i]) || Float.isInfinite(dirtLayers[i])) {
                  dirtLayers[i] = 0.0F;
               }

               dirtLayers[i] = Math.max(0.0F, Math.min(1.0F, dirtLayers[i]));
               if (dirtMasks[i] < 0 || dirtMasks[i] > 10000) {
                  dirtMasks[i] = 0;
               }

               if (layerBiomes[i] < 0 || layerBiomes[i] > 4) {
                  layerBiomes[i] = 0;
               }

               if (layerOrder[i] < 0 || layerOrder[i] > 3) {
                  layerOrder[i] = i;
               }
            }

            if (!Float.isNaN(blood)
               && !Float.isInfinite(blood)
               && !Float.isNaN(wetness)
               && !Float.isInfinite(wetness)
               && !Float.isNaN(charred)
               && !Float.isInfinite(charred)
               && !Float.isNaN(frost)
               && !Float.isInfinite(frost)) {
               if (dirtType < 0 || dirtType > 3) {
                  dirtType = 0;
               }

               if (bloodMaskIndex < 0 || bloodMaskIndex > 10000) {
                  bloodMaskIndex = 0;
               }

               if (wetMaskIndex < 0 || wetMaskIndex > 10000) {
                  wetMaskIndex = 0;
               }

               if (charMaskIndex < 0 || charMaskIndex > 10000) {
                  charMaskIndex = 0;
               }

               if (frostMaskIndex < 0 || frostMaskIndex > 10000) {
                  frostMaskIndex = 0;
               }

               float finalBlood = Math.max(0.0F, Math.min(1.0F, blood));
               float finalWetness = Math.max(0.0F, Math.min(1.0F, wetness));
               float finalCharred = Math.max(0.0F, Math.min(1.0F, charred));
               float finalFrost = Math.max(0.0F, Math.min(1.0F, frost));
               int finalDirtType = dirtType;
               int finalBloodMaskIndex = bloodMaskIndex;
               int finalWetMaskIndex = wetMaskIndex;
               int finalCharMaskIndex = charMaskIndex;
               int finalFrostMaskIndex = frostMaskIndex;
               boolean finalBreathingDisabled = payload.breathingDisabled();
               boolean finalShakeDisabled = payload.shakeDisabled();
               context.client().execute(() -> {
                  if (context.client().level != null) {
                     Entity entity = context.client().level.getEntity(entityId);
                     if (entity != null) {
                        UUID uuid = entity.getUUID();

                        for (int i = 0; i < 4; i++) {
                           WeatheringDataManager.setDirtLayer(uuid, i, dirtLayers[i]);
                           WeatheringDataManager.setDirtLayerMask(uuid, i, dirtMasks[i]);
                           WeatheringDataManager.setLayerBiomeType(uuid, i, layerBiomes[i]);
                           if (dirtLayers[i] <= 0.001F) {
                              String maxLevelKey = uuid.toString() + "_" + i;
                              WeatheringFeatureRenderer.MAX_DIRT_LEVELS.remove(maxLevelKey);
                           }
                        }

                        WeatheringDataManager.setLayerOrder(uuid, layerOrder);
                        WeatheringDataManager.setBlood(uuid, finalBlood);
                        WeatheringDataManager.setWetness(uuid, finalWetness);
                        WeatheringDataManager.setCharred(uuid, finalCharred);
                        WeatheringDataManager.setFrost(uuid, finalFrost);
                        WeatheringDataManager.setDirtType(uuid, finalDirtType);
                        WeatheringDataManager.setBloodMaskIndex(uuid, finalBloodMaskIndex);
                        WeatheringDataManager.setWetMaskIndex(uuid, finalWetMaskIndex);
                        WeatheringDataManager.setCharMaskIndex(uuid, finalCharMaskIndex);
                        WeatheringDataManager.setFrostMaskIndex(uuid, finalFrostMaskIndex);
                        WeatheringDataManager.setShakeIntensity(uuid, shakeIntensity);
                        WeatheringDataManager.setBreathingDisabled(uuid, finalBreathingDisabled);
                        WeatheringDataManager.setShakeDisabled(uuid, finalShakeDisabled);
                        dynamicLevels.forEach((effectId, val) -> WeatheringDataManager.setDynamicEffectLevel(uuid, effectId, val));
                        dynamicMasks.forEach((effectId, mask) -> WeatheringDataManager.setDynamicEffectMask(uuid, effectId, mask));
                     }
                  }
               });
            }
         }
      );
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         WeatheringFeatureRenderer.MAX_DIRT_LEVELS.clear();
         WeatheringFeatureRenderer.LAYER_TRANSITIONS.clear();
         WeatheringDataManager.DIRT_LAYERS.clear();
         WeatheringDataManager.DIRT_LAYER_MASKS.clear();
         WeatheringDataManager.LAYER_BIOME_TYPES.clear();
         WeatheringDataManager.ACTIVE_DIRT_TYPES.clear();
         WeatheringDataManager.LAYER_RENDER_ORDER.clear();
         WeatheringDataManager.BLOOD_LEVELS.clear();
         WeatheringDataManager.WET_LEVELS.clear();
         WeatheringDataManager.CHAR_LEVELS.clear();
         WeatheringDataManager.FROST_LEVELS.clear();
         WeatheringDataManager.SHAKE_INTENSITIES.clear();
         WeatheringDataManager.BLOOD_MASKS.clear();
         WeatheringDataManager.WET_MASKS.clear();
         WeatheringDataManager.CHAR_MASKS.clear();
         WeatheringDataManager.FROST_MASKS.clear();
         WeatheringDataManager.DYNAMIC_EFFECT_LEVELS.clear();
         WeatheringDataManager.DYNAMIC_EFFECT_MASKS.clear();
      });
      ClientPlayNetworking.registerGlobalReceiver(
         RegistrySyncPayload.TYPE,
         (payload, context) -> {
            List<SkinEffect> receivedEffects = new ArrayList<>();

            for (RegistrySyncPayload.EffectData eff : payload.effects()) {
               SkinEffect.BlendMode blendMode = SkinEffect.BlendMode.valueOf(eff.blendMode());
               SkinEffect effect = new SkinEffect(
                  eff.id(), eff.textures(), eff.defaultColor(), eff.scale(), eff.useAdaptiveColor(), blendMode, eff.hideOuterLayers()
               );
               receivedEffects.add(effect);
            }

            context.client().execute(() -> {
               for (SkinEffect effectx : receivedEffects) {
                  SkinEffectRegistry.register(effectx);
               }
            });
         }
      );
      LivingEntityFeatureRendererRegistrationCallback.EVENT
         .register((LivingEntityFeatureRendererRegistrationCallback)(entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER) {
               registrationHelper.register(new WeatheringFeatureRenderer((AvatarRenderer)entityRenderer));
            }
         });
   }
}
