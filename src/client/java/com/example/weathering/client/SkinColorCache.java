package com.example.weathering.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public class SkinColorCache {
   private static final ConcurrentHashMap<Identifier, SkinColorCache.ColorData> CACHE = new ConcurrentHashMap<>();
   private static final SkinColorCache.ColorData DEFAULT_COLORS = new SkinColorCache.ColorData(0.65F, 0.85F, 0.7F, 0.6F);

   public static SkinColorCache.ColorData getColors(Identifier skinId) {
      return skinId == null ? DEFAULT_COLORS : CACHE.computeIfAbsent(skinId, id -> {
         try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.getResourceManager() != null) {
               Optional<Resource> res = client.getResourceManager().getResource(id);
               if (res.isPresent()) {
                  SkinColorCache.ColorData x2;
                  try (InputStream is = res.get().open()) {
                     NativeImage img = NativeImage.read(is);
                     x2 = analyzeImage(img);
                  }

                  return x2;
               }
            }
         } catch (Exception var8) {
         }

         return DEFAULT_COLORS;
      });
   }

   public static SkinColorCache.ColorData analyzeImage(NativeImage image) {
      if (image == null) {
         return DEFAULT_COLORS;
      } else {
         long totalR = 0L;
         long totalG = 0L;
         long totalB = 0L;
         int sampledPixels = 0;
         int startX = 8;
         int startY = 8;
         int endX = 16;
         int endY = 16;

         try {
            for (int x = startX; x < endX; x++) {
               for (int y = startY; y < endY; y++) {
                  int color = image.getPixel(x, y);
                  int alpha = color >> 24 & 0xFF;
                  if (alpha > 200) {
                     totalR += color & 0xFF;
                     totalG += color >> 8 & 0xFF;
                     totalB += color >> 16 & 0xFF;
                     sampledPixels++;
                  }
               }
            }
         } catch (Exception var16) {
            return DEFAULT_COLORS;
         }

         if (sampledPixels == 0) {
            return DEFAULT_COLORS;
         } else {
            float avgR = (float)totalR / sampledPixels / 255.0F;
            float avgG = (float)totalG / sampledPixels / 255.0F;
            float avgB = (float)totalB / sampledPixels / 255.0F;
            float luminance = 0.299F * avgR + 0.587F * avgG + 0.114F * avgB;
            return new SkinColorCache.ColorData(luminance, avgR, avgG, avgB);
         }
      }
   }

   public static class ColorData {
      public final float skinLuminance;
      public final float r;
      public final float g;
      public final float b;

      public ColorData(float skinLuminance, float r, float g, float b) {
         this.skinLuminance = skinLuminance;
         this.r = r;
         this.g = g;
         this.b = b;
      }
   }
}
