package com.example.weathering.client;

import com.example.weathering.WeatheringConfig;
import com.example.weathering.WeatheringDataManager;
import com.example.weathering.api.SkinEffect;
import com.example.weathering.api.SkinEffectRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

public class WeatheringFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
   public static final List<Identifier> DIRT_MASKS = new ArrayList<>();
   public static final List<Identifier> BLOOD_MASKS = new ArrayList<>();
   private static final Identifier GENERIC_PARTICLE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/particle/generic_0.png");
   private static final Identifier DEFAULT_DIRT_MASK = Identifier.fromNamespaceAndPath("skin_weathering", "textures/entity/textures_dirt/dirt_mask.png");
   public static final List<WeatheringFeatureRenderer.DirtMaskGroup> DIRT_MASK_GROUPS = new ArrayList<>();
   public static final ConcurrentHashMap<String, WeatheringFeatureRenderer.ColorTransitionState> LAYER_TRANSITIONS = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<UUID, WeatheringFeatureRenderer.BreathState> BREATH_STATES = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<String, Float> MAX_DIRT_LEVELS = new ConcurrentHashMap<>();

   public WeatheringFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
      super(context);
   }

   public static void loadMasks(ResourceManager resourceManager) {
      DIRT_MASKS.clear();
      BLOOD_MASKS.clear();
      DIRT_MASK_GROUPS.clear();
      Map<Identifier, Resource> bloodResources = resourceManager.listResources(
         "textures/entity/textures_blood", id -> id.getPath().endsWith(".png") && id.getNamespace().equals("skin_weathering")
      );

      for (Identifier id : bloodResources.keySet()) {
         BLOOD_MASKS.add(id);
      }

      Map<Identifier, Resource> dirtResources = resourceManager.listResources(
         "textures/entity/textures_dirt", id -> id.getPath().endsWith(".png") && id.getNamespace().equals("skin_weathering")
      );
      Map<String, Map<Integer, Identifier>> tempGroups = new HashMap<>();

      for (Identifier id : dirtResources.keySet()) {
         DIRT_MASKS.add(id);
         String path = id.getPath();
         String prefix = "textures/entity/textures_dirt/";
         if (path.startsWith(prefix)) {
            String subPath = path.substring(prefix.length());
            int slashIndex = subPath.indexOf(47);
            if (slashIndex != -1) {
               String groupName = subPath.substring(0, slashIndex);
               String fileName = subPath.substring(slashIndex + 1);
               int stageNum = -1;
               if (fileName.contains("stage-1")) {
                  stageNum = 1;
               } else if (fileName.contains("stage-2")) {
                  stageNum = 2;
               } else if (fileName.contains("stage-3")) {
                  stageNum = 3;
               } else if (fileName.contains("stage-4")) {
                  stageNum = 4;
               }

               if (stageNum != -1) {
                  tempGroups.computeIfAbsent(groupName, k -> new HashMap<>()).put(stageNum, id);
               }
            }
         }
      }

      for (Entry<String, Map<Integer, Identifier>> entry : tempGroups.entrySet()) {
         Map<Integer, Identifier> stageMap = entry.getValue();
         Identifier s1 = stageMap.get(1);
         Identifier s2 = stageMap.get(2);
         Identifier s3 = stageMap.get(3);
         Identifier s4 = stageMap.get(4);
         if (s1 == null) {
            s1 = DEFAULT_DIRT_MASK;
         }

         if (s2 == null) {
            s2 = s1;
         }

         if (s3 == null) {
            s3 = s2;
         }

         if (s4 == null) {
            s4 = s3;
         }

         DIRT_MASK_GROUPS.add(new WeatheringFeatureRenderer.DirtMaskGroup(s1, s2, s3, s4));
      }

      if (DIRT_MASK_GROUPS.isEmpty() && !DIRT_MASKS.isEmpty()) {
         for (Identifier fallbackId : DIRT_MASKS) {
            DIRT_MASK_GROUPS.add(new WeatheringFeatureRenderer.DirtMaskGroup(fallbackId, fallbackId, fallbackId, fallbackId));
         }
      }

      if (DIRT_MASK_GROUPS.isEmpty()) {
         DIRT_MASK_GROUPS.add(new WeatheringFeatureRenderer.DirtMaskGroup(DEFAULT_DIRT_MASK, DEFAULT_DIRT_MASK, DEFAULT_DIRT_MASK, DEFAULT_DIRT_MASK));
      }

      if (DIRT_MASKS.isEmpty()) {
         DIRT_MASKS.add(DEFAULT_DIRT_MASK);
      }

      if (BLOOD_MASKS.isEmpty()) {
         BLOOD_MASKS.add(Identifier.fromNamespaceAndPath("skin_weathering", "textures/entity/textures_blood/blood_mask.png"));
      }

      System.out
         .println(
            "[Weathering Scanner] Loaded sequential dirt groups: "
               + DIRT_MASK_GROUPS.size()
               + " | dirt masks: "
               + DIRT_MASKS.size()
               + " | blood masks: "
               + BLOOD_MASKS.size()
         );
   }

   public static WeatheringFeatureRenderer.AdaptiveColor calculateAdaptiveDirtColor(
      int dirtType, int layerBiomeType, float skinLuminance, float skinR, float skinG, float skinB
   ) {
      float baseR;
      float baseG;
      float baseB;
      if (dirtType == 1) {
         if (layerBiomeType == 4) {
            baseR = 0.65F;
            baseG = 0.28F;
            baseB = 0.15F;
         } else {
            baseR = 0.78F;
            baseG = 0.6F;
            baseB = 0.36F;
         }
      } else if (dirtType == 2) {
         baseR = 0.42F;
         baseG = 0.43F;
         baseB = 0.45F;
      } else if (dirtType == 3) {
         baseR = 0.95F;
         baseG = 0.98F;
         baseB = 1.0F;
      } else if (layerBiomeType == 1) {
         baseR = 0.11F;
         baseG = 0.12F;
         baseB = 0.08F;
      } else if (layerBiomeType == 2) {
         baseR = 0.2F;
         baseG = 0.24F;
         baseB = 0.12F;
      } else if (layerBiomeType == 3) {
         baseR = 0.25F;
         baseG = 0.23F;
         baseB = 0.22F;
      } else {
         baseR = 0.35F;
         baseG = 0.22F;
         baseB = 0.12F;
      }

      if (dirtType != 3) {
         if (skinLuminance < 0.32F) {
            float boost = (0.32F - skinLuminance) * 0.85F;
            baseR = Math.min(1.0F, baseR + boost * 1.3F);
            baseG = Math.min(1.0F, baseG + boost * 1.1F);
            baseB = Math.min(1.0F, baseB + boost * 0.9F);
         } else {
            baseR = Math.max(0.1F, Math.min(0.95F, baseR * (skinR / 0.82F)));
            baseG = Math.max(0.08F, Math.min(0.95F, baseG * (skinG / 0.7F)));
            baseB = Math.max(0.04F, Math.min(0.95F, baseB * (skinB / 0.6F)));
         }
      }

      float shadowR = baseR * 0.38F;
      float shadowG = baseG * 0.32F;
      float shadowB = baseB * 0.28F;
      if (dirtType == 1) {
         shadowR = baseR * 0.55F;
         shadowG = baseG * 0.45F;
         shadowB = baseB * 0.35F;
      } else if (dirtType == 3) {
         shadowR = baseR * 0.7F;
         shadowG = baseG * 0.8F;
         shadowB = baseB * 0.92F;
      }

      return new WeatheringFeatureRenderer.AdaptiveColor(baseR, baseG, baseB, shadowR, shadowG, shadowB);
   }

   public static float getPartFactor(int seed, int offset) {
      int hash = seed ^ offset * 31;
      return 0.85F + Math.abs(hash % 31) / 100.0F;
   }

   private void setModelScale(PlayerModel model, float scale) {
      if (model != null) {
         this.setScale(model.head, scale);
         this.setScale(model.hat, scale);
         this.setScale(model.body, scale);
         this.setScale(model.rightArm, scale);
         this.setScale(model.leftArm, scale);
         this.setScale(model.rightLeg, scale);
         this.setScale(model.leftLeg, scale);
         this.setScale(model.leftSleeve, scale);
         this.setScale(model.rightSleeve, scale);
         this.setScale(model.leftPants, scale);
         this.setScale(model.rightPants, scale);
         this.setScale(model.jacket, scale);
      }
   }

   private void setScale(ModelPart part, float scale) {
      if (part != null) {
         part.xScale = scale;
         part.yScale = scale;
         part.zScale = scale;
      }
   }

   private void renderDirtLayer(
      int layerType,
      int renderOrderIndex,
      UUID uuid,
      PlayerModel model,
      AvatarRenderState state,
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light,
      WeatheringConfig config,
      float skinLum,
      float skinR,
      float skinG,
      float skinB,
      int dirtSeed
   ) {
      float layerLevel = WeatheringDataManager.getDirtLayer(uuid, config.enableLayering ? layerType : 0);
      if (!(layerLevel <= 0.01F)) {
         int layerSeed = WeatheringDataManager.getDirtLayerMask(uuid, config.enableLayering ? layerType : 0);
         String maxLevelKey = uuid.toString() + "_" + (config.enableLayering ? layerType : 0);
         float maxLevel = MAX_DIRT_LEVELS.compute(
            maxLevelKey, (k, currentMax) -> (Float)(currentMax != null && !(layerLevel > currentMax) ? currentMax : layerLevel)
         );
         float washRatio = 1.0F;
         if (maxLevel > 0.01F) {
            washRatio = layerLevel / maxLevel;
         }

         int layerBiomeType = WeatheringDataManager.getLayerBiomeType(uuid, layerType);
         WeatheringFeatureRenderer.AdaptiveColor finalColors = calculateAdaptiveDirtColor(layerType, layerBiomeType, skinLum, skinR, skinG, skinB);
         String transitionKey = uuid.toString() + "_" + (config.enableLayering ? layerType : 0) + (config.enableLayering ? "_multi" : "_single");
         WeatheringFeatureRenderer.ColorTransitionState transition = LAYER_TRANSITIONS.computeIfAbsent(
            transitionKey, k -> new WeatheringFeatureRenderer.ColorTransitionState(finalColors.r, finalColors.g, finalColors.b)
         );
         float lerpSpeed = 0.05F;
         transition.currentR = transition.currentR + (finalColors.r - transition.currentR) * lerpSpeed;
         transition.currentG = transition.currentG + (finalColors.g - transition.currentG) * lerpSpeed;
         transition.currentB = transition.currentB + (finalColors.b - transition.currentB) * lerpSpeed;
         float layerScale = 1.001F + renderOrderIndex * 2.0E-4F;
         this.setModelScale(model, layerScale);
         int order = 1 + renderOrderIndex;
         int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
         if (config.sequentialDirt && !DIRT_MASK_GROUPS.isEmpty()) {
            int groupIndex = Math.abs(layerSeed) % DIRT_MASK_GROUPS.size();
            WeatheringFeatureRenderer.DirtMaskGroup group = DIRT_MASK_GROUPS.get(groupIndex);

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
                  int argbColor = (int)(stageAlpha * 255.0F) << 24
                     | (int)(transition.currentR * 255.0F) << 16
                     | (int)(transition.currentG * 255.0F) << 8
                     | (int)(transition.currentB * 255.0F);
                  submitNodeCollector.order(order)
                     .submitModel(model, state, matrices, RenderTypes.entityTranslucent(stageTex), light, overlay, argbColor, null, 0, null);
               }
            }
         } else if (!DIRT_MASKS.isEmpty()) {
            int dirtIndex = Math.abs(layerSeed) % DIRT_MASKS.size();
            Identifier selectedDirt = DIRT_MASKS.get(dirtIndex);
            int argbColor = (int)(layerLevel * 255.0F) << 24
               | (int)(transition.currentR * 255.0F) << 16
               | (int)(transition.currentG * 255.0F) << 8
               | (int)(transition.currentB * 255.0F);
            submitNodeCollector.order(order)
               .submitModel(model, state, matrices, RenderTypes.entityTranslucent(selectedDirt), light, overlay, argbColor, null, 0, null);
         }

         this.setModelScale(model, 1.0F);
      }
   }

   private void renderDirtLayerSingle(
      int activeType,
      float totalLevel,
      UUID uuid,
      PlayerModel model,
      AvatarRenderState state,
      PoseStack matrices,
      SubmitNodeCollector submitNodeCollector,
      int light,
      WeatheringConfig config,
      float skinLum,
      float skinR,
      float skinG,
      float skinB,
      int dirtSeed
   ) {
      int layerSeed = WeatheringDataManager.getDirtLayerMask(uuid, 0);
      String maxLevelKey = uuid.toString() + "_0_single_max";
      float maxLevel = MAX_DIRT_LEVELS.compute(
         maxLevelKey, (k, currentMax) -> (Float)(currentMax != null && !(totalLevel > currentMax) ? currentMax : totalLevel)
      );
      float washRatio = 1.0F;
      if (maxLevel > 0.01F) {
         washRatio = totalLevel / maxLevel;
      }

      int activeBiomeType = WeatheringDataManager.getLayerBiomeType(uuid, activeType);
      WeatheringFeatureRenderer.AdaptiveColor finalColors = calculateAdaptiveDirtColor(activeType, activeBiomeType, skinLum, skinR, skinG, skinB);
      String transitionKey = uuid.toString() + "_0_single";
      WeatheringFeatureRenderer.ColorTransitionState transition = LAYER_TRANSITIONS.computeIfAbsent(
         transitionKey, k -> new WeatheringFeatureRenderer.ColorTransitionState(finalColors.r, finalColors.g, finalColors.b)
      );
      float lerpSpeed = 0.05F;
      transition.currentR = transition.currentR + (finalColors.r - transition.currentR) * lerpSpeed;
      transition.currentG = transition.currentG + (finalColors.g - transition.currentG) * lerpSpeed;
      transition.currentB = transition.currentB + (finalColors.b - transition.currentB) * lerpSpeed;
      this.setModelScale(model, 1.001F);
      int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
      if (config.sequentialDirt && !DIRT_MASK_GROUPS.isEmpty()) {
         int groupIndex = Math.abs(layerSeed) % DIRT_MASK_GROUPS.size();
         WeatheringFeatureRenderer.DirtMaskGroup group = DIRT_MASK_GROUPS.get(groupIndex);

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
               int argbColor = (int)(stageAlpha * 255.0F) << 24
                  | (int)(transition.currentR * 255.0F) << 16
                  | (int)(transition.currentG * 255.0F) << 8
                  | (int)(transition.currentB * 255.0F);
               submitNodeCollector.order(1)
                  .submitModel(model, state, matrices, RenderTypes.entityTranslucent(stageTex), light, overlay, argbColor, null, 0, null);
            }
         }
      } else if (!DIRT_MASKS.isEmpty()) {
         int dirtIndex = Math.abs(layerSeed) % DIRT_MASKS.size();
         Identifier selectedDirt = DIRT_MASKS.get(dirtIndex);
         int argbColor = (int)(totalLevel * 255.0F) << 24
            | (int)(transition.currentR * 255.0F) << 16
            | (int)(transition.currentG * 255.0F) << 8
            | (int)(transition.currentB * 255.0F);
         submitNodeCollector.order(1)
            .submitModel(model, state, matrices, RenderTypes.entityTranslucent(selectedDirt), light, overlay, argbColor, null, 0, null);
      }

      this.setModelScale(model, 1.0F);
   }

   public void submit(PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, AvatarRenderState state, float yRot, float xRot) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         Entity entity = client.level.getEntity(state.id);
         if (entity != null) {
            UUID uuid = entity.getUUID();
            PlayerModel model = (PlayerModel)this.getParentModel();
            WeatheringConfig config = WeatheringConfig.get();
            BlockPos pos = entity.blockPosition();
            Holder<Biome> biome = entity.level().getBiome(pos);
            Level world = entity.level();
            float baseTemp = ((Biome)biome.value()).getBaseTemperature();
            float dynamicTemp = baseTemp;
            if (pos.getY() > 64) {
               dynamicTemp = baseTemp - (pos.getY() - 64) / 600.0F;
            }

            if (world.getDayTime() % 24000L >= 13000L) {
               dynamicTemp -= 0.45F;
            }

            boolean isCold = dynamicTemp < 0.35F;
            boolean isSleeping = entity instanceof LivingEntity le && le.isSleeping();
            boolean shouldBreathe = !WeatheringDataManager.isBreathingDisabled(uuid) && (!config.breathOnlyInCold || isCold);
            if (shouldBreathe && !entity.isSpectator() && !entity.isInWater() && !isSleeping) {
               int breathCycle = (int)state.ageInTicks % config.breathCycleTicks;
               if (breathCycle < config.breathDurationTicks && !DIRT_MASKS.isEmpty()) {
                  float progress = (float)breathCycle / config.breathDurationTicks;
                  float size = 0.04F + progress * 0.16F;
                  float forwardOffset = progress * 0.28F;
                  float upwardOffset = progress * 0.08F;
                  float alpha = (1.0F - progress) * config.breathOpacity;
                  float currentBodyYaw = state.bodyRot;
                  WeatheringFeatureRenderer.BreathState bState = BREATH_STATES.computeIfAbsent(
                     uuid,
                     k -> new WeatheringFeatureRenderer.BreathState(
                        new WeatheringFeatureRenderer.HeadRotationState(
                           model.head.yRot, model.head.xRot, model.head.zRot, model.head.x, model.head.y, model.head.z
                        ),
                        new Vec3(state.x, state.y, state.z),
                        currentBodyYaw
                     )
                  );
                  Vec3 currentRenderPos = new Vec3(state.x, state.y, state.z);
                  Vec3 worldDelta = currentRenderPos.subtract(bState.startPos);
                  matrices.pushPose();
                  matrices.scale(-1.0F, -1.0F, 1.0F);
                  matrices.mulPose(Axis.YP.rotationDegrees(currentBodyYaw - 180.0F));
                  matrices.translate(-worldDelta.x, -worldDelta.y, -worldDelta.z);
                  matrices.mulPose(Axis.YP.rotationDegrees(180.0F - bState.bodyYaw));
                  matrices.scale(-1.0F, -1.0F, 1.0F);
                  WeatheringFeatureRenderer.HeadRotationState rot = bState.headRotation;
                  matrices.translate(rot.pivotX / 16.0F, rot.pivotY / 16.0F, rot.pivotZ / 16.0F);
                  if (rot.roll != 0.0F) {
                     matrices.mulPose(Axis.ZP.rotation(rot.roll));
                  }

                  if (rot.yaw != 0.0F) {
                     matrices.mulPose(Axis.YP.rotation(rot.yaw));
                  }

                  if (rot.pitch != 0.0F) {
                     matrices.mulPose(Axis.XP.rotation(rot.pitch));
                  }

                  matrices.translate(0.0F, -0.05F - upwardOffset, -0.25F - forwardOffset);
                  submitNodeCollector.submitCustomGeometry(
                     matrices,
                     RenderTypes.entityTranslucent(GENERIC_PARTICLE_TEXTURE),
                     (pose, steamBuffer) -> this.drawCube(steamBuffer, pose, size, 0.92F, 0.95F, 0.98F, alpha * 0.8F, light)
                  );
                  matrices.pushPose();
                  matrices.translate(0.015F * (1.0F - progress), 0.01F, 0.015F);
                  submitNodeCollector.submitCustomGeometry(
                     matrices,
                     RenderTypes.entityTranslucent(GENERIC_PARTICLE_TEXTURE),
                     (pose, steamBuffer) -> this.drawCube(steamBuffer, pose, size * 0.75F, 0.9F, 0.93F, 0.97F, alpha * 0.5F, light)
                  );
                  matrices.popPose();
                  matrices.pushPose();
                  matrices.translate(-0.015F * (1.0F - progress), -0.01F, -0.015F);
                  submitNodeCollector.submitCustomGeometry(
                     matrices,
                     RenderTypes.entityTranslucent(GENERIC_PARTICLE_TEXTURE),
                     (pose, steamBuffer) -> this.drawCube(steamBuffer, pose, size * 0.6F, 0.88F, 0.91F, 0.95F, alpha * 0.4F, light)
                  );
                  matrices.popPose();
                  matrices.popPose();
               } else {
                  BREATH_STATES.remove(uuid);
               }
            } else {
               BREATH_STATES.remove(uuid);
            }

            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            float bloodLevel = WeatheringDataManager.getBlood(uuid);
            float wetLevel = WeatheringDataManager.getWetness(uuid);
            float frostLevel = WeatheringDataManager.getFrost(uuid);
            if (wetLevel > 0.01F && config.showWetness && !DIRT_MASKS.isEmpty()) {
               int wetSeed = WeatheringDataManager.getWetMaskIndex(uuid);
               int wetIndex = Math.abs(wetSeed) % DIRT_MASKS.size();
               Identifier selectedWet = DIRT_MASKS.get(wetIndex);
               float r = 0.2F;
               float g = 0.22F;
               float b = 0.26F;
               float alphax = wetLevel * 0.45F;
               this.setModelScale(model, 1.001F);
               int argbColor = (int)(alphax * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
               submitNodeCollector.order(1)
                  .submitModel(model, state, matrices, RenderTypes.entityTranslucent(selectedWet), light, overlay, argbColor, null, 0, null);
               this.setModelScale(model, 1.0F);
            }

            if (config.showDirt) {
               float skinLum = 0.65F;
               float skinR = 0.85F;
               float skinG = 0.7F;
               float skinB = 0.6F;
               if (state.skin != null && state.skin.body() != null) {
                  SkinColorCache.ColorData colors = SkinColorCache.getColors(state.skin.body().id());
                  skinLum = colors.skinLuminance;
                  skinR = colors.r;
                  skinG = colors.g;
                  skinB = colors.b;
               }

               int dirtSeed = WeatheringDataManager.getDirtMaskIndex(uuid);
               if (!config.enableLayering) {
                  int activeType = WeatheringDataManager.getDirtType(uuid);
                  float totalLevel = 0.0F;

                  for (int i = 0; i < 4; i++) {
                     totalLevel += WeatheringDataManager.getDirtLayer(uuid, i);
                  }

                  totalLevel = Math.min(1.0F, totalLevel);
                  if (totalLevel > 0.01F) {
                     this.renderDirtLayerSingle(
                        activeType, totalLevel, uuid, model, state, matrices, submitNodeCollector, light, config, skinLum, skinR, skinG, skinB, dirtSeed
                     );
                  }
               } else {
                  for (int i = 0; i < 4; i++) {
                     int layerType = WeatheringDataManager.getLayerOrder(uuid, i);
                     this.renderDirtLayer(
                        layerType, i, uuid, model, state, matrices, submitNodeCollector, light, config, skinLum, skinR, skinG, skinB, dirtSeed
                     );
                  }
               }
            }

            if (bloodLevel > 0.01F && config.showBlood && !BLOOD_MASKS.isEmpty()) {
               int bloodSeed = WeatheringDataManager.getBloodMaskIndex(uuid);
               int bloodIndex = Math.abs(bloodSeed) % BLOOD_MASKS.size();
               Identifier selectedBlood = BLOOD_MASKS.get(bloodIndex);
               float bR;
               float bG;
               float bB;
               if (state.skin != null && state.skin.body() != null) {
                  int hex = config.customBloodColor & 16777215;
                  float customR = (hex >> 16 & 0xFF) / 255.0F;
                  float customG = (hex >> 8 & 0xFF) / 255.0F;
                  float customB = (hex & 0xFF) / 255.0F;
                  if (config.useAdaptiveBlood) {
                     SkinColorCache.ColorData colors = SkinColorCache.getColors(state.skin.body().id());
                     float skinBrightnessFactor = 0.299F * colors.r + 0.587F * colors.g + 0.114F * colors.b;
                     bR = Math.min(1.0F, customR * (skinBrightnessFactor * 1.5F));
                     bG = Math.min(1.0F, customG * (skinBrightnessFactor * 1.5F));
                     bB = Math.min(1.0F, customB * (skinBrightnessFactor * 1.5F));
                  } else {
                     bR = customR;
                     bG = customG;
                     bB = customB;
                  }
               } else {
                  bR = 0.35F;
                  bG = 0.02F;
                  bB = 0.02F;
               }

               this.setModelScale(model, 1.0015F);
               int argbColor = (int)(bloodLevel * 255.0F) << 24 | (int)(bR * 255.0F) << 16 | (int)(bG * 255.0F) << 8 | (int)(bB * 255.0F);
               submitNodeCollector.order(2)
                  .submitModel(model, state, matrices, RenderTypes.entityTranslucent(selectedBlood), light, overlay, argbColor, null, 0, null);
               this.setModelScale(model, 1.0F);
            }

            if (frostLevel > 0.01F && config.showFrost && !DIRT_MASKS.isEmpty()) {
               int frostSeed = WeatheringDataManager.getFrostMaskIndex(uuid);
               int frostIndex = Math.abs(frostSeed) % DIRT_MASKS.size();
               Identifier selectedFrost = DIRT_MASKS.get(frostIndex);
               float r = 0.88F;
               float g = 0.96F;
               float b = 1.0F;
               float alphax = frostLevel * 0.85F;
               this.setModelScale(model, 1.0012F);
               int argbColor = (int)(alphax * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
               submitNodeCollector.order(3)
                  .submitModel(model, state, matrices, RenderTypes.entityTranslucent(selectedFrost), light, overlay, argbColor, null, 0, null);
               this.setModelScale(model, 1.0F);
            }

            for (SkinEffect effect : SkinEffectRegistry.getAll()) {
               float level = WeatheringDataManager.getDynamicEffectLevel(uuid, effect.getId());
               if (level > 0.01F) {
                  int seed = WeatheringDataManager.getDynamicEffectMask(uuid, effect.getId());
                  List<Identifier> textures = effect.getMaskTextures();
                  if (state.skin != null
                     && state.skin.model() == PlayerModelType.SLIM
                     && effect.getSlimMaskTextures() != null
                     && !effect.getSlimMaskTextures().isEmpty()) {
                     textures = effect.getSlimMaskTextures();
                  }

                  Identifier texture = DEFAULT_DIRT_MASK;
                  if (textures != null && !textures.isEmpty()) {
                     int index = Math.abs(seed) % textures.size();
                     texture = textures.get(index);
                  }

                  float r;
                  float g;
                  float b;
                  if (effect.isUseAdaptiveColor()) {
                     float skinLumx = 0.65F;
                     float skinRx = 0.85F;
                     float skinGx = 0.7F;
                     float skinBx = 0.6F;
                     if (state.skin != null && state.skin.body() != null) {
                        SkinColorCache.ColorData colors = SkinColorCache.getColors(state.skin.body().id());
                        skinLumx = colors.skinLuminance;
                        skinRx = colors.r;
                        skinGx = colors.g;
                        skinBx = colors.b;
                     }

                     int hex = effect.getDefaultColor() & 16777215;
                     float customR = (hex >> 16 & 0xFF) / 255.0F;
                     float customG = (hex >> 8 & 0xFF) / 255.0F;
                     float customB = (hex & 0xFF) / 255.0F;
                     float skinBrightnessFactor = 0.299F * skinRx + 0.587F * skinGx + 0.114F * skinBx;
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

                  this.setModelScale(model, effect.getScale());
                  int argbColor = (int)(level * 255.0F) << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
                  submitNodeCollector.order(4).submitModel(model, state, matrices, layer, light, overlay, argbColor, null, 0, null);
                  this.setModelScale(model, 1.0F);
               }
            }
         }
      }
   }

   private void drawCube(VertexConsumer buffer, Pose entry, float size, float r, float g, float b, float alpha, int light) {
      float h = size / 2.0F;
      this.drawVertex(buffer, entry, -h, h, -h, r, g, b, alpha, light, 0.0F, 1.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, h, h, r, g, b, alpha, light, 0.0F, 1.0F, 0.0F);
      this.drawVertex(buffer, entry, h, h, h, r, g, b, alpha, light, 0.0F, 1.0F, 0.0F);
      this.drawVertex(buffer, entry, h, h, -h, r, g, b, alpha, light, 0.0F, 1.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, -h, -h, r, g, b, alpha, light, 0.0F, -1.0F, 0.0F);
      this.drawVertex(buffer, entry, h, -h, -h, r, g, b, alpha, light, 0.0F, -1.0F, 0.0F);
      this.drawVertex(buffer, entry, h, -h, h, r, g, b, alpha, light, 0.0F, -1.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, -h, h, r, g, b, alpha, light, 0.0F, -1.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, -h, h, r, g, b, alpha, light, 0.0F, 0.0F, 1.0F);
      this.drawVertex(buffer, entry, h, -h, h, r, g, b, alpha, light, 0.0F, 0.0F, 1.0F);
      this.drawVertex(buffer, entry, h, h, h, r, g, b, alpha, light, 0.0F, 0.0F, 1.0F);
      this.drawVertex(buffer, entry, -h, h, h, r, g, b, alpha, light, 0.0F, 0.0F, 1.0F);
      this.drawVertex(buffer, entry, -h, -h, -h, r, g, b, alpha, light, 0.0F, 0.0F, -1.0F);
      this.drawVertex(buffer, entry, -h, h, -h, r, g, b, alpha, light, 0.0F, 0.0F, -1.0F);
      this.drawVertex(buffer, entry, h, h, -h, r, g, b, alpha, light, 0.0F, 0.0F, -1.0F);
      this.drawVertex(buffer, entry, h, -h, -h, r, g, b, alpha, light, 0.0F, 0.0F, -1.0F);
      this.drawVertex(buffer, entry, -h, -h, -h, r, g, b, alpha, light, -1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, -h, h, r, g, b, alpha, light, -1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, h, h, r, g, b, alpha, light, -1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, -h, h, -h, r, g, b, alpha, light, -1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, h, -h, -h, r, g, b, alpha, light, 1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, h, h, -h, r, g, b, alpha, light, 1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, h, h, h, r, g, b, alpha, light, 1.0F, 0.0F, 0.0F);
      this.drawVertex(buffer, entry, h, -h, h, r, g, b, alpha, light, 1.0F, 0.0F, 0.0F);
   }

   private void drawVertex(
      VertexConsumer buffer, Pose entry, float x, float y, float z, float r, float g, float b, float alpha, int light, float nx, float ny, float nz
   ) {
      buffer.addVertex(entry.pose(), x, y, z)
         .setColor(r, g, b, alpha)
         .setUv(0.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(entry, nx, ny, nz);
   }

   public static class AdaptiveColor {
      public final float r;
      public final float g;
      public final float b;
      public final float shadowR;
      public final float shadowG;
      public final float shadowB;

      public AdaptiveColor(float r, float g, float b, float sr, float sg, float sb) {
         this.r = r;
         this.g = g;
         this.b = b;
         this.shadowR = sr;
         this.shadowG = sg;
         this.shadowB = sb;
      }
   }

   public static class BreathState {
      public final WeatheringFeatureRenderer.HeadRotationState headRotation;
      public final Vec3 startPos;
      public final float bodyYaw;

      public BreathState(WeatheringFeatureRenderer.HeadRotationState headRotation, Vec3 startPos, float bodyYaw) {
         this.headRotation = headRotation;
         this.startPos = startPos;
         this.bodyYaw = bodyYaw;
      }
   }

   public static class ColorTransitionState {
      public float currentR;
      public float currentG;
      public float currentB;

      public ColorTransitionState(float r, float g, float b) {
         this.currentR = r;
         this.currentG = g;
         this.currentB = b;
      }
   }

   public static class DirtMaskGroup {
      public final Identifier stage1;
      public final Identifier stage2;
      public final Identifier stage3;
      public final Identifier stage4;

      public DirtMaskGroup(Identifier s1, Identifier s2, Identifier s3, Identifier s4) {
         this.stage1 = s1;
         this.stage2 = s2;
         this.stage3 = s3;
         this.stage4 = s4;
      }

      public Identifier getStage(int stage) {
         return switch (stage) {
            case 1 -> this.stage1;
            case 2 -> this.stage2;
            case 3 -> this.stage3;
            case 4 -> this.stage4;
            default -> this.stage1;
         };
      }
   }

   public static class HeadRotationState {
      public final float yaw;
      public final float pitch;
      public final float roll;
      public final float pivotX;
      public final float pivotY;
      public final float pivotZ;

      public HeadRotationState(float yaw, float pitch, float roll, float pivotX, float pivotY, float pivotZ) {
         this.yaw = yaw;
         this.pitch = pitch;
         this.roll = roll;
         this.pivotX = pivotX;
         this.pivotY = pivotY;
         this.pivotZ = pivotZ;
      }
   }
}
