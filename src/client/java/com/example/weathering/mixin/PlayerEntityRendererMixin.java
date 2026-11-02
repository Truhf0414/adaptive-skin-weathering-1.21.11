package com.example.weathering.mixin;

import com.example.weathering.WeatheringConfig;
import com.example.weathering.WeatheringDataManager;
import com.example.weathering.api.SkinEffect;
import com.example.weathering.api.SkinEffectRegistry;
import com.example.weathering.client.SkinColorCache;
import com.example.weathering.client.WeatheringFeatureRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {
   @Inject(method = "renderRightHand", at = @At("HEAD"))
   private void hideRightSleeve(PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, Identifier skinTexture, boolean isSlim, CallbackInfo ci) {
      this.hideSleeveIfNeeded();
   }

   @Inject(method = "renderRightHand", at = @At("TAIL"))
   private void onRenderRightArm(
      PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, Identifier skinTexture, boolean isSlim, CallbackInfo ci
   ) {
      AvatarRenderer<?> renderer = (AvatarRenderer<?>)this;
      PlayerModel model = (PlayerModel)renderer.getModel();
      this.renderWeatheringArm(matrices, submitNodeCollector, light, skinTexture, isSlim, model.rightArm, model.rightSleeve, true);
   }

   @Inject(method = "renderLeftHand", at = @At("HEAD"))
   private void hideLeftSleeve(PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, Identifier skinTexture, boolean isSlim, CallbackInfo ci) {
      this.hideSleeveIfNeeded();
   }

   @Inject(method = "renderLeftHand", at = @At("TAIL"))
   private void onRenderLeftArm(PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, Identifier skinTexture, boolean isSlim, CallbackInfo ci) {
      AvatarRenderer<?> renderer = (AvatarRenderer<?>)this;
      PlayerModel model = (PlayerModel)renderer.getModel();
      this.renderWeatheringArm(matrices, submitNodeCollector, light, skinTexture, isSlim, model.leftArm, model.leftSleeve, false);
   }

   @Unique
   private void hideSleeveIfNeeded() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         UUID uuid = client.player.getUUID();
         float maxLevel = 0.0F;

         for (SkinEffect effect : SkinEffectRegistry.getAll()) {
            if (effect.isHideOuterLayers()) {
               float level = WeatheringDataManager.getDynamicEffectLevel(uuid, effect.getId());
               if (level > maxLevel) {
                  maxLevel = level;
               }
            }
         }

         if (maxLevel > 0.05F) {
            AvatarRenderer<?> renderer = (AvatarRenderer<?>)this;
            PlayerModel model = (PlayerModel)renderer.getModel();
            model.rightSleeve.visible = false;
            model.leftSleeve.visible = false;
         }
      }
   }

   @Unique
   private void renderWeatheringArm(
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light,
      Identifier skinTexture,
      boolean isSlim,
      ModelPart arm,
      ModelPart sleeve,
      boolean isRightArm
   ) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         AbstractClientPlayer player = client.player;
         UUID uuid = player.getUUID();
         float bloodLevel = WeatheringDataManager.getBlood(uuid);
         float wetLevel = WeatheringDataManager.getWetness(uuid);
         float charLevel = WeatheringDataManager.getCharred(uuid);
         float frostLevel = WeatheringDataManager.getFrost(uuid);
         SkinColorCache.ColorData colors = SkinColorCache.getColors(skinTexture);
         WeatheringConfig config = WeatheringConfig.get();
         if (charLevel > 0.01F && config.showCharred && !WeatheringFeatureRenderer.DIRT_MASKS.isEmpty()) {
            int charSeed = WeatheringDataManager.getCharMaskIndex(uuid);
            int charIndex = Math.abs(charSeed) % WeatheringFeatureRenderer.DIRT_MASKS.size();
            Identifier selectedChar = WeatheringFeatureRenderer.DIRT_MASKS.get(charIndex);
            float r = 0.06F;
            float g = 0.06F;
            float b = 0.06F;
            arm.xScale = 1.001F;
            arm.yScale = 1.001F;
            arm.zScale = 1.001F;
            sleeve.xScale = 1.001F;
            sleeve.yScale = 1.001F;
            sleeve.zScale = 1.001F;
            int argbColor = (int)(charLevel * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
            RenderType rt = RenderTypes.entityTranslucent(selectedChar);
            submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            arm.xScale = 1.0F;
            arm.yScale = 1.0F;
            arm.zScale = 1.0F;
            sleeve.xScale = 1.0F;
            sleeve.yScale = 1.0F;
            sleeve.zScale = 1.0F;
         }

         if (wetLevel > 0.01F && config.showWetness && !WeatheringFeatureRenderer.DIRT_MASKS.isEmpty()) {
            int wetSeed = WeatheringDataManager.getWetMaskIndex(uuid);
            int wetIndex = Math.abs(wetSeed) % WeatheringFeatureRenderer.DIRT_MASKS.size();
            Identifier selectedWet = WeatheringFeatureRenderer.DIRT_MASKS.get(wetIndex);
            float r = 0.2F;
            float g = 0.22F;
            float b = 0.26F;
            float alpha = wetLevel * 0.45F;
            arm.xScale = 1.002F;
            arm.yScale = 1.002F;
            arm.zScale = 1.002F;
            sleeve.xScale = 1.002F;
            sleeve.yScale = 1.002F;
            sleeve.zScale = 1.002F;
            int argbColor = (int)(alpha * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
            RenderType rt = RenderTypes.entityTranslucent(selectedWet);
            submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            arm.xScale = 1.0F;
            arm.yScale = 1.0F;
            arm.zScale = 1.0F;
            sleeve.xScale = 1.0F;
            sleeve.yScale = 1.0F;
            sleeve.zScale = 1.0F;
         }

         if (frostLevel > 0.01F && config.showFrost && !WeatheringFeatureRenderer.DIRT_MASKS.isEmpty()) {
            int frostSeed = WeatheringDataManager.getFrostMaskIndex(uuid);
            int frostIndex = Math.abs(frostSeed) % WeatheringFeatureRenderer.DIRT_MASKS.size();
            Identifier selectedFrost = WeatheringFeatureRenderer.DIRT_MASKS.get(frostIndex);
            float r = 0.88F;
            float g = 0.96F;
            float b = 1.0F;
            float alpha = frostLevel * 0.85F;
            arm.xScale = 1.003F;
            arm.yScale = 1.003F;
            arm.zScale = 1.003F;
            sleeve.xScale = 1.003F;
            sleeve.yScale = 1.003F;
            sleeve.zScale = 1.003F;
            int argbColor = (int)(alpha * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
            RenderType rt = RenderTypes.entityTranslucent(selectedFrost);
            submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            arm.xScale = 1.0F;
            arm.yScale = 1.0F;
            arm.zScale = 1.0F;
            sleeve.xScale = 1.0F;
            sleeve.yScale = 1.0F;
            sleeve.zScale = 1.0F;
         }

         if (config.showDirt) {
            if (!config.enableLayering) {
               int activeType = WeatheringDataManager.getDirtType(uuid);
               float totalLevel = 0.0F;

               for (int i = 0; i < 4; i++) {
                  totalLevel += WeatheringDataManager.getDirtLayer(uuid, i);
               }

               totalLevel = Math.min(1.0F, totalLevel);
               if (totalLevel > 0.01F) {
                  this.renderArmLayerSingle(activeType, totalLevel, uuid, arm, sleeve, matrices, submitNodeCollector, light, config, colors, isRightArm);
               }
            } else {
               for (int i = 0; i < 4; i++) {
                  int layerType = WeatheringDataManager.getLayerOrder(uuid, i);
                  this.renderArmLayer(layerType, i, uuid, arm, sleeve, matrices, submitNodeCollector, light, config, colors, isRightArm);
               }
            }
         }

         if (bloodLevel > 0.01F && config.showBlood && !WeatheringFeatureRenderer.BLOOD_MASKS.isEmpty()) {
            int bloodSeed = WeatheringDataManager.getBloodMaskIndex(uuid);
            int bloodIndex = Math.abs(bloodSeed) % WeatheringFeatureRenderer.BLOOD_MASKS.size();
            Identifier selectedBlood = WeatheringFeatureRenderer.BLOOD_MASKS.get(bloodIndex);
            int hex = config.customBloodColor & 16777215;
            float customR = (hex >> 16 & 0xFF) / 255.0F;
            float customG = (hex >> 8 & 0xFF) / 255.0F;
            float customB = (hex & 0xFF) / 255.0F;
            float bR;
            float bG;
            float bB;
            if (config.useAdaptiveBlood) {
               float skinBrightnessFactor = 0.299F * colors.r + 0.587F * colors.g + 0.114F * colors.b;
               bR = Math.min(1.0F, customR * (skinBrightnessFactor * 1.5F));
               bG = Math.min(1.0F, customG * (skinBrightnessFactor * 1.5F));
               bB = Math.min(1.0F, customB * (skinBrightnessFactor * 1.5F));
            } else {
               bR = customR;
               bG = customG;
               bB = customB;
            }

            arm.xScale = 1.005F;
            arm.yScale = 1.005F;
            arm.zScale = 1.005F;
            sleeve.xScale = 1.005F;
            sleeve.yScale = 1.005F;
            sleeve.zScale = 1.005F;
            int argbColor = (int)(bloodLevel * 255.0F) << 24 | (int)(bR * 255.0F) << 16 | (int)(bG * 255.0F) << 8 | (int)(bB * 255.0F);
            RenderType rt = RenderTypes.entityTranslucent(selectedBlood);
            submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            arm.xScale = 1.0F;
            arm.yScale = 1.0F;
            arm.zScale = 1.0F;
            sleeve.xScale = 1.0F;
            sleeve.yScale = 1.0F;
            sleeve.zScale = 1.0F;
         }

         for (SkinEffect effect : SkinEffectRegistry.getAll()) {
            float level = WeatheringDataManager.getDynamicEffectLevel(uuid, effect.getId());
            if (level > 0.01F) {
               int seed = WeatheringDataManager.getDynamicEffectMask(uuid, effect.getId());
               List<Identifier> textures = isSlim && effect.getSlimMaskTextures() != null && !effect.getSlimMaskTextures().isEmpty()
                  ? effect.getSlimMaskTextures()
                  : effect.getMaskTextures();
               Identifier texture = Identifier.fromNamespaceAndPath("skin_weathering", "textures/entity/textures_dirt/dirt_mask.png");
               if (textures != null && !textures.isEmpty()) {
                  int index = Math.abs(seed) % textures.size();
                  texture = textures.get(index);
               }

               float r;
               float g;
               float b;
               if (effect.isUseAdaptiveColor()) {
                  float skinBrightnessFactor = 0.299F * colors.r + 0.587F * colors.g + 0.114F * colors.b;
                  int hex = effect.getDefaultColor() & 16777215;
                  float customR = (hex >> 16 & 0xFF) / 255.0F;
                  float customG = (hex >> 8 & 0xFF) / 255.0F;
                  float customB = (hex & 0xFF) / 255.0F;
                  r = Math.min(1.0F, customR * (skinBrightnessFactor * 1.5F));
                  g = Math.min(1.0F, customG * (skinBrightnessFactor * 1.5F));
                  b = Math.min(1.0F, customB * (skinBrightnessFactor * 1.5F));
               } else {
                  int hex = effect.getDefaultColor() & 16777215;
                  r = (hex >> 16 & 0xFF) / 255.0F;
                  g = (hex >> 8 & 0xFF) / 255.0F;
                  b = (hex & 0xFF) / 255.0F;
               }

               RenderType layer;
               if (effect.getBlendMode() == SkinEffect.BlendMode.GLOW) {
                  layer = RenderTypes.eyes(texture);
               } else {
                  layer = RenderTypes.entityTranslucent(texture);
               }

               float sc = effect.getScale();
               arm.xScale = sc;
               arm.yScale = sc;
               arm.zScale = sc;
               sleeve.xScale = sc;
               sleeve.yScale = sc;
               sleeve.zScale = sc;
               int argbColor = (int)(level * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
               submitNodeCollector.submitModelPart(arm, matrices, layer, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
               submitNodeCollector.submitModelPart(sleeve, matrices, layer, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
               arm.xScale = 1.0F;
               arm.yScale = 1.0F;
               arm.zScale = 1.0F;
               sleeve.xScale = 1.0F;
               sleeve.yScale = 1.0F;
               sleeve.zScale = 1.0F;
            }
         }
      }
   }

   @Unique
   private void renderArmLayer(
      int layerType,
      int renderOrderIndex,
      UUID uuid,
      ModelPart arm,
      ModelPart sleeve,
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light,
      WeatheringConfig config,
      SkinColorCache.ColorData colors,
      boolean isRightArm
   ) {
      float layerLevel = WeatheringDataManager.getDirtLayer(uuid, config.enableLayering ? layerType : 0);
      if (!(layerLevel <= 0.01F)) {
         int layerSeed = WeatheringDataManager.getDirtLayerMask(uuid, config.enableLayering ? layerType : 0);
         String maxLevelKey = uuid.toString() + "_" + (config.enableLayering ? layerType : 0);
         float maxLevel = WeatheringFeatureRenderer.MAX_DIRT_LEVELS
            .compute(maxLevelKey, (k, currentMax) -> (Float)(currentMax != null && !(layerLevel > currentMax) ? currentMax : layerLevel));
         float washRatio = 1.0F;
         if (maxLevel > 0.01F) {
            washRatio = layerLevel / maxLevel;
         }

         WeatheringFeatureRenderer.AdaptiveColor finalColors = WeatheringFeatureRenderer.calculateAdaptiveDirtColor(
            layerType, WeatheringDataManager.getLayerBiomeType(uuid, layerType), colors.skinLuminance, colors.r, colors.g, colors.b
         );
         float r = finalColors.r;
         float g = finalColors.g;
         float b = finalColors.b;
         String transitionKey = uuid.toString() + "_" + (config.enableLayering ? layerType : 0) + (config.enableLayering ? "_multi" : "_single");
         WeatheringFeatureRenderer.ColorTransitionState transition = WeatheringFeatureRenderer.LAYER_TRANSITIONS.get(transitionKey);
         if (transition != null) {
            r = transition.currentR;
            g = transition.currentG;
            b = transition.currentB;
         }

         int offset = isRightArm ? 53 : 41;
         float factor = WeatheringFeatureRenderer.getPartFactor(layerSeed, offset);
         float vr = Math.max(0.0F, Math.min(1.0F, r * factor));
         float vg = Math.max(0.0F, Math.min(1.0F, g * factor));
         float vb = Math.max(0.0F, Math.min(1.0F, b * factor));
         float layerScale = 1.004F + renderOrderIndex * 3.0E-4F + 1.0E-4F;
         arm.xScale = layerScale;
         arm.yScale = layerScale;
         arm.zScale = layerScale;
         sleeve.xScale = layerScale;
         sleeve.yScale = layerScale;
         sleeve.zScale = layerScale;
         if (config.sequentialDirt && !WeatheringFeatureRenderer.DIRT_MASK_GROUPS.isEmpty()) {
            int groupIndex = Math.abs(layerSeed) % WeatheringFeatureRenderer.DIRT_MASK_GROUPS.size();
            WeatheringFeatureRenderer.DirtMaskGroup group = WeatheringFeatureRenderer.DIRT_MASK_GROUPS.get(groupIndex);

            for (int i = 0; i < 4; i++) {
               float start = i * 0.25F;
               float end = start + 0.25F;
               float stageAlpha;
               if (maxLevel <= start) {
                  stageAlpha = 0.0F;
               } else if (maxLevel >= end) {
                  stageAlpha = 1.0F;
               } else {
                  stageAlpha = (maxLevel - start) / 0.25F;
               }

               stageAlpha *= washRatio;
               if (stageAlpha > 0.01F) {
                  Identifier stageTex = group.getStage(i + 1);
                  int argbColor = (int)(stageAlpha * 255.0F) << 24 | (int)(vr * 255.0F) << 16 | (int)(vg * 255.0F) << 8 | (int)(vb * 255.0F);
                  RenderType rt = RenderTypes.entityTranslucent(stageTex);
                  submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
                  submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
               }
            }
         } else if (!WeatheringFeatureRenderer.DIRT_MASKS.isEmpty()) {
            int dirtIndex = Math.abs(layerSeed) % WeatheringFeatureRenderer.DIRT_MASKS.size();
            Identifier selectedDirt = WeatheringFeatureRenderer.DIRT_MASKS.get(dirtIndex);
            int argbColor = (int)(layerLevel * 255.0F) << 24 | (int)(vr * 255.0F) << 16 | (int)(vg * 255.0F) << 8 | (int)(vb * 255.0F);
            RenderType rt = RenderTypes.entityTranslucent(selectedDirt);
            submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
         }

         arm.xScale = 1.0F;
         arm.yScale = 1.0F;
         arm.zScale = 1.0F;
         sleeve.xScale = 1.0F;
         sleeve.yScale = 1.0F;
         sleeve.zScale = 1.0F;
      }
   }

   @Unique
   private void renderArmLayerSingle(
      int activeType,
      float totalLevel,
      UUID uuid,
      ModelPart arm,
      ModelPart sleeve,
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light,
      WeatheringConfig config,
      SkinColorCache.ColorData colors,
      boolean isRightArm
   ) {
      int layerSeed = WeatheringDataManager.getDirtLayerMask(uuid, 0);
      String maxLevelKey = uuid.toString() + "_0_single_max";
      float maxLevel = WeatheringFeatureRenderer.MAX_DIRT_LEVELS
         .compute(maxLevelKey, (k, currentMax) -> (Float)(currentMax != null && !(totalLevel > currentMax) ? currentMax : totalLevel));
      float washRatio = 1.0F;
      if (maxLevel > 0.01F) {
         washRatio = totalLevel / maxLevel;
      }

      WeatheringFeatureRenderer.AdaptiveColor finalColors = WeatheringFeatureRenderer.calculateAdaptiveDirtColor(
         activeType, WeatheringDataManager.getLayerBiomeType(uuid, activeType), colors.skinLuminance, colors.r, colors.g, colors.b
      );
      float r = finalColors.r;
      float g = finalColors.g;
      float b = finalColors.b;
      String transitionKey = uuid.toString() + "_0_single";
      WeatheringFeatureRenderer.ColorTransitionState transition = WeatheringFeatureRenderer.LAYER_TRANSITIONS.get(transitionKey);
      if (transition != null) {
         r = transition.currentR;
         g = transition.currentG;
         b = transition.currentB;
      }

      int offset = isRightArm ? 53 : 41;
      float factor = WeatheringFeatureRenderer.getPartFactor(layerSeed, offset);
      float vr = Math.max(0.0F, Math.min(1.0F, r * factor));
      float vg = Math.max(0.0F, Math.min(1.0F, g * factor));
      float vb = Math.max(0.0F, Math.min(1.0F, b * factor));
      float layerScale = 1.0041F;
      arm.xScale = layerScale;
      arm.yScale = layerScale;
      arm.zScale = layerScale;
      sleeve.xScale = layerScale;
      sleeve.yScale = layerScale;
      sleeve.zScale = layerScale;
      if (config.sequentialDirt && !WeatheringFeatureRenderer.DIRT_MASK_GROUPS.isEmpty()) {
         int groupIndex = Math.abs(layerSeed) % WeatheringFeatureRenderer.DIRT_MASK_GROUPS.size();
         WeatheringFeatureRenderer.DirtMaskGroup group = WeatheringFeatureRenderer.DIRT_MASK_GROUPS.get(groupIndex);

         for (int i = 0; i < 4; i++) {
            float start = i * 0.25F;
            float end = start + 0.25F;
            float stageAlpha;
            if (maxLevel <= start) {
               stageAlpha = 0.0F;
            } else if (maxLevel >= end) {
               stageAlpha = 1.0F;
            } else {
               stageAlpha = (maxLevel - start) / 0.25F;
            }

            stageAlpha *= washRatio;
            if (stageAlpha > 0.01F) {
               Identifier stageTex = group.getStage(i + 1);
               int argbColor = (int)(stageAlpha * 255.0F) << 24 | (int)(vr * 255.0F) << 16 | (int)(vg * 255.0F) << 8 | (int)(vb * 255.0F);
               RenderType rt = RenderTypes.entityTranslucent(stageTex);
               submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
               submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
            }
         }
      } else if (!WeatheringFeatureRenderer.DIRT_MASKS.isEmpty()) {
         int dirtIndex = Math.abs(layerSeed) % WeatheringFeatureRenderer.DIRT_MASKS.size();
         Identifier selectedDirt = WeatheringFeatureRenderer.DIRT_MASKS.get(dirtIndex);
         int argbColor = (int)(totalLevel * 255.0F) << 24 | (int)(vr * 255.0F) << 16 | (int)(vg * 255.0F) << 8 | (int)(vb * 255.0F);
         RenderType rt = RenderTypes.entityTranslucent(selectedDirt);
         submitNodeCollector.submitModelPart(arm, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
         submitNodeCollector.submitModelPart(sleeve, matrices, rt, light, OverlayTexture.NO_OVERLAY, null, argbColor, null, 0);
      }

      arm.xScale = 1.0F;
      arm.yScale = 1.0F;
      arm.zScale = 1.0F;
      sleeve.xScale = 1.0F;
      sleeve.yScale = 1.0F;
      sleeve.zScale = 1.0F;
   }
}
