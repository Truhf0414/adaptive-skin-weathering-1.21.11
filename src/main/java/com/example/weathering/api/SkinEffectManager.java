package com.example.weathering.api;

import com.example.weathering.WeatheringDataManager;
import com.example.weathering.network.WeatheringNetworking;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public class SkinEffectManager {
   public static void setIntensity(LivingEntity entity, Identifier effectId, float intensity) {
      if (entity != null && effectId != null) {
         if (SkinEffectRegistry.get(effectId) != null) {
            UUID uuid = entity.getUUID();
            float oldIntensity = WeatheringDataManager.getDynamicEffectLevel(uuid, effectId);
            float newIntensity = Math.max(0.0F, Math.min(1.0F, intensity));
            if (oldIntensity != newIntensity) {
               WeatheringDataManager.setDynamicEffectLevel(uuid, effectId, newIntensity);
               if (newIntensity > 0.01F) {
                  WeatheringDataManager.getDynamicEffectMask(uuid, effectId);
               } else {
                  WeatheringDataManager.setDynamicEffectMask(uuid, effectId, 0);
               }

               if (!entity.level().isClientSide()) {
                  WeatheringNetworking.syncToClients(entity);
               }
            }
         }
      }
   }

   public static float getIntensity(LivingEntity entity, Identifier effectId) {
      return entity != null && effectId != null ? WeatheringDataManager.getDynamicEffectLevel(entity.getUUID(), effectId) : 0.0F;
   }

   public static void setMaskIndex(LivingEntity entity, Identifier effectId, int index) {
      if (entity != null && effectId != null) {
         WeatheringDataManager.setDynamicEffectMask(entity.getUUID(), effectId, index);
         if (!entity.level().isClientSide()) {
            WeatheringNetworking.syncToClients(entity);
         }
      }
   }

   public static int getMaskIndex(LivingEntity entity, Identifier effectId) {
      return entity != null && effectId != null ? WeatheringDataManager.getDynamicEffectMask(entity.getUUID(), effectId) : 0;
   }
}
