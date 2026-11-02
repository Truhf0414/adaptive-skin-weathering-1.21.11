package com.example.weathering.network;

import com.example.weathering.WeatheringDataManager;
import com.example.weathering.api.SkinEffect;
import com.example.weathering.api.SkinEffectRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class WeatheringNetworking {
   public static void sendRegistrySync(ServerPlayer player) {
      Collection<SkinEffect> registryEffects = SkinEffectRegistry.getAll();
      List<RegistrySyncPayload.EffectData> effects = new ArrayList<>();

      for (SkinEffect effect : registryEffects) {
         effects.add(
            new RegistrySyncPayload.EffectData(
               effect.getId(),
               effect.getMaskTextures(),
               effect.getDefaultColor(),
               effect.getScale(),
               effect.isUseAdaptiveColor(),
               effect.getBlendMode().name(),
               effect.isHideOuterLayers()
            )
         );
      }

      RegistrySyncPayload payload = new RegistrySyncPayload(effects);
      ServerPlayNetworking.send(player, payload);
   }

   public static void syncToClients(LivingEntity entity) {
      if (!entity.level().isClientSide()) {
         UUID uuid = entity.getUUID();
         float[] dirtLayers = new float[4];

         for (int i = 0; i < 4; i++) {
            dirtLayers[i] = WeatheringDataManager.getDirtLayer(uuid, i);
         }

         int[] dirtMasks = new int[4];

         for (int i = 0; i < 4; i++) {
            dirtMasks[i] = WeatheringDataManager.getDirtLayerMask(uuid, i);
         }

         int[] layerBiomes = new int[4];

         for (int i = 0; i < 4; i++) {
            layerBiomes[i] = WeatheringDataManager.getLayerBiomeType(uuid, i);
         }

         int[] layerOrder = new int[4];

         for (int i = 0; i < 4; i++) {
            layerOrder[i] = WeatheringDataManager.getLayerOrder(uuid, i);
         }

         Collection<SkinEffect> registeredEffects = SkinEffectRegistry.getAll();
         List<WeatheringSyncPayload.DynamicEffectData> dynamicEffects = new ArrayList<>();

         for (SkinEffect effect : registeredEffects) {
            Identifier id = effect.getId();
            dynamicEffects.add(
               new WeatheringSyncPayload.DynamicEffectData(
                  id, WeatheringDataManager.getDynamicEffectLevel(uuid, id), WeatheringDataManager.getDynamicEffectMask(uuid, id)
               )
            );
         }

         WeatheringSyncPayload payload = new WeatheringSyncPayload(
            entity.getId(),
            dirtLayers,
            WeatheringDataManager.getBlood(uuid),
            WeatheringDataManager.getWetness(uuid),
            WeatheringDataManager.getCharred(uuid),
            WeatheringDataManager.getFrost(uuid),
            WeatheringDataManager.getDirtType(uuid),
            dirtMasks,
            layerBiomes,
            layerOrder,
            WeatheringDataManager.getBloodMaskIndex(uuid),
            WeatheringDataManager.getWetMaskIndex(uuid),
            WeatheringDataManager.getCharMaskIndex(uuid),
            WeatheringDataManager.getFrostMaskIndex(uuid),
            WeatheringDataManager.getShakeIntensity(uuid),
            WeatheringDataManager.isBreathingDisabled(uuid),
            WeatheringDataManager.isShakeDisabled(uuid),
            dynamicEffects
         );

         for (ServerPlayer trackingPlayer : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(trackingPlayer, payload);
         }

         if (entity instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, payload);
         }
      }
   }
}
