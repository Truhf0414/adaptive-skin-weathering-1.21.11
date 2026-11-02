package com.example.weathering.mixin;

import com.example.weathering.WeatheringConfig;
import com.example.weathering.WeatheringDataManager;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class BipedEntityModelMixin {
   @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
   private void onSetupAnim(AvatarRenderState state, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         Entity entity = client.level.getEntity(state.id);
         if (entity != null) {
            UUID uuid = entity.getUUID();
            float snow = 0.0F;
            boolean layering = WeatheringConfig.get().enableLayering;
            if (layering) {
               snow = WeatheringDataManager.getDirtLayer(uuid, 3);
            } else if (WeatheringDataManager.getDirtType(uuid) == 3) {
               snow = WeatheringDataManager.getDirtLayer(uuid, 0);
            }

            float customShake = WeatheringDataManager.getShakeIntensity(uuid);
            float intensity = 0.0F;
            if (customShake >= 0.0F) {
               intensity = customShake;
            } else if (snow > 0.05F) {
               intensity = Math.min(1.0F, (snow - 0.05F) / 0.45F);
            }

            boolean hasFullArmor = state.headEquipment != null
               && !state.headEquipment.isEmpty()
               && state.chestEquipment != null
               && !state.chestEquipment.isEmpty()
               && state.legsEquipment != null
               && !state.legsEquipment.isEmpty()
               && state.feetEquipment != null
               && !state.feetEquipment.isEmpty();
            if (hasFullArmor) {
               intensity = 0.0F;
            }

            if (WeatheringConfig.get().enableShaking && !WeatheringDataManager.isShakeDisabled(uuid) && intensity > 0.0F) {
               float speed = 15.0F;
               float animationProgress = state.ageInTicks;
               float shakeX = (float)Math.sin(animationProgress * speed) * 0.07F * intensity;
               float shakeZ = (float)Math.cos(animationProgress * speed * 0.87F) * 0.05F * intensity;
               float shakeY = (float)Math.sin(animationProgress * speed * 1.13F) * 0.035F * intensity;
               PlayerModel model = (PlayerModel)this;
               model.head.zRot = shakeX;
               model.head.xRot += shakeZ;
               model.hat.zRot = shakeX;
               model.hat.xRot += shakeZ;
               model.body.zRot = shakeX * 0.5F;
               model.body.xRot += shakeY * 0.3F;
               model.jacket.zRot = shakeX * 0.5F;
               model.jacket.xRot += shakeY * 0.3F;
               model.rightArm.zRot += shakeX * 0.7F;
               model.rightArm.xRot += shakeZ * 0.5F;
               model.rightSleeve.zRot += shakeX * 0.7F;
               model.rightSleeve.xRot += shakeZ * 0.5F;
               model.leftArm.zRot -= shakeX * 0.7F;
               model.leftArm.xRot -= shakeZ * 0.5F;
               model.leftSleeve.zRot -= shakeX * 0.7F;
               model.leftSleeve.xRot -= shakeZ * 0.5F;
               model.rightLeg.zRot += shakeY * 0.3F;
               model.rightPants.zRot += shakeY * 0.3F;
               model.leftLeg.zRot -= shakeY * 0.3F;
               model.leftPants.zRot -= shakeY * 0.3F;
            }
         }
      }
   }
}
