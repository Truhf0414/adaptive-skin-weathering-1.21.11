package com.example.weathering.mixin;

import com.example.weathering.WeatheringConfig;
import com.example.weathering.WeatheringDataManager;
import com.example.weathering.network.WeatheringNetworking;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$SplashingType", remap = false)
public class CreateSplashingTypeMixin {
   @Inject(method = "affectEntity", at = @At("TAIL"), remap = false, require = 0)
   private void onAffectEntity(Entity entity, Level world, CallbackInfo ci) {
      if (!world.isClientSide()) {
         if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            UUID uuid = livingEntity.getUUID();
            WeatheringConfig config = WeatheringConfig.get();
            float oldDirt = WeatheringDataManager.getDirt(uuid);
            float oldBlood = WeatheringDataManager.getBlood(uuid);
            float oldChar = WeatheringDataManager.getCharred(uuid);
            float oldWet = WeatheringDataManager.getWetness(uuid);
            if (oldDirt > 0.0F || oldBlood > 0.0F || oldChar > 0.0F || oldWet < 1.0F) {
               float newDirt = Math.max(0.0F, oldDirt - config.createDirtWashRate);
               float newBlood = Math.max(0.0F, oldBlood - config.createBloodWashRate);
               float newChar = Math.max(0.0F, oldChar - config.createCharredWashRate);
               float newWet = Math.min(1.0F, oldWet + config.createWetnessFillRate);
               WeatheringDataManager.setDirt(uuid, newDirt);
               WeatheringDataManager.setBlood(uuid, newBlood);
               WeatheringDataManager.setCharred(uuid, newChar);
               WeatheringDataManager.setWetness(uuid, newWet);
               if (newDirt <= 0.0F) {
                  WeatheringDataManager.setDirtMaskIndex(uuid, 0);
               }

               if (newBlood <= 0.0F) {
                  WeatheringDataManager.setBloodMaskIndex(uuid, 0);
               }

               if (newChar <= 0.0F) {
                  WeatheringDataManager.setCharMaskIndex(uuid, 0);
               }

               if (newWet <= 0.0F) {
                  WeatheringDataManager.setWetMaskIndex(uuid, 0);
               } else if (oldWet <= 0.0F && newWet > 0.0F) {
                  WeatheringDataManager.setWetMaskIndex(uuid, livingEntity.getRandom().nextInt(10000));
               }

               if (world instanceof ServerLevel serverWorld) {
                  serverWorld.sendParticles(
                     ParticleTypes.SPLASH, livingEntity.getX(), livingEntity.getY() + 0.7, livingEntity.getZ(), 14, 0.25, 0.45, 0.25, 0.15
                  );
                  if (world.getRandom().nextFloat() < 0.35F) {
                     serverWorld.sendParticles(
                        ParticleTypes.CLOUD, livingEntity.getX(), livingEntity.getY() + 0.8, livingEntity.getZ(), 2, 0.15, 0.3, 0.15, 0.1
                     );
                  }

                  if (world.getRandom().nextFloat() < 0.3F) {
                     serverWorld.sendParticles(
                        ParticleTypes.BUBBLE, livingEntity.getX(), livingEntity.getY() + 0.5, livingEntity.getZ(), 3, 0.2, 0.3, 0.2, 0.08
                     );
                  }

                  if (world.getRandom().nextFloat() < 0.12F) {
                     serverWorld.playSound(
                        null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.35F, 1.3F
                     );
                  }
               }

               WeatheringNetworking.syncToClients(livingEntity);
            }
         }
      }
   }
}
