package com.example.weathering.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;

public class SkinEffect {
   private final Identifier id;
   private final List<Identifier> maskTextures;
   private final List<Identifier> slimMaskTextures;
   private final int defaultColor;
   private final float scale;
   private final boolean useAdaptiveColor;
   private final SkinEffect.BlendMode blendMode;
   private final boolean hideOuterLayers;

   public SkinEffect(
      Identifier id,
      List<Identifier> maskTextures,
      List<Identifier> slimMaskTextures,
      int defaultColor,
      float scale,
      boolean useAdaptiveColor,
      SkinEffect.BlendMode blendMode,
      boolean hideOuterLayers
   ) {
      this.id = id;
      this.maskTextures = new ArrayList<>(maskTextures);
      this.slimMaskTextures = slimMaskTextures != null ? new ArrayList<>(slimMaskTextures) : new ArrayList<>();
      this.defaultColor = defaultColor;
      this.scale = scale;
      this.useAdaptiveColor = useAdaptiveColor;
      this.blendMode = blendMode;
      this.hideOuterLayers = hideOuterLayers;
   }

   public SkinEffect(
      Identifier id,
      List<Identifier> maskTextures,
      int defaultColor,
      float scale,
      boolean useAdaptiveColor,
      SkinEffect.BlendMode blendMode,
      boolean hideOuterLayers
   ) {
      this(id, maskTextures, new ArrayList<>(), defaultColor, scale, useAdaptiveColor, blendMode, hideOuterLayers);
   }

   public Identifier getId() {
      return this.id;
   }

   public List<Identifier> getMaskTextures() {
      return Collections.unmodifiableList(this.maskTextures);
   }

   public List<Identifier> getSlimMaskTextures() {
      return Collections.unmodifiableList(this.slimMaskTextures);
   }

   public int getDefaultColor() {
      return this.defaultColor;
   }

   public float getScale() {
      return this.scale;
   }

   public boolean isUseAdaptiveColor() {
      return this.useAdaptiveColor;
   }

   public SkinEffect.BlendMode getBlendMode() {
      return this.blendMode;
   }

   public boolean isHideOuterLayers() {
      return this.hideOuterLayers;
   }

   public static SkinEffect.Builder builder(Identifier id) {
      return new SkinEffect.Builder(id);
   }

   public static enum BlendMode {
      TRANSLUCENT,
      GLOW;
   }

   public static class Builder {
      private final Identifier id;
      private final List<Identifier> maskTextures = new ArrayList<>();
      private final List<Identifier> slimMaskTextures = new ArrayList<>();
      private int defaultColor = 16777215;
      private float scale = 1.001F;
      private boolean useAdaptiveColor = false;
      private SkinEffect.BlendMode blendMode = SkinEffect.BlendMode.TRANSLUCENT;
      private boolean hideOuterLayers = false;

      public Builder(Identifier id) {
         this.id = id;
      }

      public SkinEffect.Builder texture(Identifier texture) {
         this.maskTextures.add(texture);
         return this;
      }

      public SkinEffect.Builder textures(List<Identifier> textures) {
         this.maskTextures.addAll(textures);
         return this;
      }

      public SkinEffect.Builder slimTexture(Identifier texture) {
         this.slimMaskTextures.add(texture);
         return this;
      }

      public SkinEffect.Builder slimTextures(List<Identifier> textures) {
         this.slimMaskTextures.addAll(textures);
         return this;
      }

      public SkinEffect.Builder defaultColor(int color) {
         this.defaultColor = color;
         return this;
      }

      public SkinEffect.Builder scale(float scale) {
         this.scale = scale;
         return this;
      }

      public SkinEffect.Builder useAdaptiveColor(boolean useAdaptiveColor) {
         this.useAdaptiveColor = useAdaptiveColor;
         return this;
      }

      public SkinEffect.Builder blendMode(SkinEffect.BlendMode blendMode) {
         this.blendMode = blendMode;
         return this;
      }

      public SkinEffect.Builder hideOuterLayers(boolean hideOuterLayers) {
         this.hideOuterLayers = hideOuterLayers;
         return this;
      }

      public SkinEffect build() {
         if (this.maskTextures.isEmpty()) {
            throw new IllegalArgumentException("SkinEffect must have at least one mask texture");
         } else {
            return new SkinEffect(
               this.id, this.maskTextures, this.slimMaskTextures, this.defaultColor, this.scale, this.useAdaptiveColor, this.blendMode, this.hideOuterLayers
            );
         }
      }
   }
}
