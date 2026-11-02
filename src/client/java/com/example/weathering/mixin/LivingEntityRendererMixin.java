package com.example.weathering.mixin;

import com.example.weathering.WeatheringDataManager;
import com.example.weathering.api.SkinEffect;
import com.example.weathering.api.SkinEffectRegistry;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class LivingEntityRendererMixin {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("TAIL")
   )
   private void onExtractRenderState(Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
      if (avatar instanceof LivingEntity) {
         LivingEntity entity = avatar;
         float maxLevel = 0.0F;

         for (SkinEffect effect : SkinEffectRegistry.getAll()) {
            if (effect.isHideOuterLayers()) {
               float level = WeatheringDataManager.getDynamicEffectLevel(entity.getUUID(), effect.getId());
               if (level > maxLevel) {
                  maxLevel = level;
               }
            }
         }

         if (maxLevel > 0.05F) {
            state.showHat = false;
            state.showJacket = false;
            state.showLeftPants = false;
            state.showRightPants = false;
            state.showLeftSleeve = false;
            state.showRightSleeve = false;
         }
      }
   }
}
