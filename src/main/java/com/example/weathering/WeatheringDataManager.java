package com.example.weathering;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.resources.Identifier;

public class WeatheringDataManager {
   public static final ConcurrentHashMap<UUID, float[]> DIRT_LAYERS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, int[]> DIRT_LAYER_MASKS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, int[]> LAYER_BIOME_TYPES = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Integer> ACTIVE_DIRT_TYPES = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, int[]> LAYER_RENDER_ORDER = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Float> BLOOD_LEVELS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Float> WET_LEVELS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Float> CHAR_LEVELS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Float> FROST_LEVELS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Float> SHAKE_INTENSITIES = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Integer> BLOOD_MASKS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Integer> WET_MASKS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Integer> CHAR_MASKS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Integer> FROST_MASKS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, ConcurrentHashMap<Identifier, Float>> DYNAMIC_EFFECT_LEVELS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, ConcurrentHashMap<Identifier, Integer>> DYNAMIC_EFFECT_MASKS = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Boolean> BREATHING_DISABLED = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, Boolean> SHAKE_DISABLED = new ConcurrentHashMap<>();

   public static int getLayerBiomeType(UUID uuid, int type) {
      int[] types = LAYER_BIOME_TYPES.get(uuid);
      if (types == null) {
         return 0;
      } else {
         return type >= 0 && type < 4 ? types[type] : 0;
      }
   }

   public static void setLayerBiomeType(UUID uuid, int type, int bType) {
      int[] types = LAYER_BIOME_TYPES.computeIfAbsent(uuid, k -> new int[4]);
      if (type >= 0 && type < 4) {
         types[type] = bType;
      }
   }

   public static int getLayerOrder(UUID uuid, int index) {
      int[] order = LAYER_RENDER_ORDER.get(uuid);
      if (order == null) {
         order = new int[]{0, 1, 2, 3};
         LAYER_RENDER_ORDER.put(uuid, order);
      }

      return order[index];
   }

   public static void setLayerOrder(UUID uuid, int[] order) {
      if (order != null && order.length == 4) {
         LAYER_RENDER_ORDER.put(uuid, order);
      }
   }

   public static void promoteLayerToTop(UUID uuid, int layerType) {
      int[] order = LAYER_RENDER_ORDER.get(uuid);
      if (order == null) {
         order = new int[]{0, 1, 2, 3};
      }

      int foundIdx = -1;

      for (int i = 0; i < 4; i++) {
         if (order[i] == layerType) {
            foundIdx = i;
            break;
         }
      }

      if (foundIdx != -1 && foundIdx < 3) {
         int[] newOrder = new int[4];
         int newIdx = 0;

         for (int ix = 0; ix < 4; ix++) {
            if (ix != foundIdx) {
               newOrder[newIdx++] = order[ix];
            }
         }

         newOrder[3] = layerType;
         LAYER_RENDER_ORDER.put(uuid, newOrder);
      } else if (foundIdx == -1) {
         LAYER_RENDER_ORDER.put(uuid, new int[]{0, 1, 2, 3});
      }
   }

   public static float getDirtLayer(UUID uuid, int type) {
      float[] layers = DIRT_LAYERS.get(uuid);
      if (layers == null) {
         return 0.0F;
      } else {
         return type >= 0 && type < 4 ? layers[type] : 0.0F;
      }
   }

   public static void setDirtLayer(UUID uuid, int type, float val) {
      float[] layers = DIRT_LAYERS.computeIfAbsent(uuid, k -> new float[4]);
      if (type >= 0 && type < 4) {
         layers[type] = Math.max(0.0F, Math.min(1.0F, val));
      }
   }

   public static int getDirtLayerMask(UUID uuid, int type) {
      int[] masks = DIRT_LAYER_MASKS.get(uuid);
      if (masks == null) {
         masks = DIRT_LAYER_MASKS.computeIfAbsent(uuid, k -> {
            int[] m = new int[4];

            for (int i = 0; i < 4; i++) {
               m[i] = ThreadLocalRandom.current().nextInt(9999) + 1;
            }

            return m;
         });
      }

      if (type >= 0 && type < 4) {
         if (masks[type] <= 0) {
            masks[type] = ThreadLocalRandom.current().nextInt(9999) + 1;
         }

         return masks[type];
      } else {
         return 0;
      }
   }

   public static void setDirtLayerMask(UUID uuid, int type, int index) {
      int[] masks = DIRT_LAYER_MASKS.computeIfAbsent(uuid, k -> {
         int[] m = new int[4];

         for (int i = 0; i < 4; i++) {
            m[i] = ThreadLocalRandom.current().nextInt(9999) + 1;
         }

         return m;
      });
      if (type >= 0 && type < 4) {
         if (index <= 0) {
            masks[type] = 0;
         } else {
            masks[type] = index;
         }
      }
   }

   public static float getDirt(UUID uuid) {
      float[] layers = DIRT_LAYERS.get(uuid);
      if (layers == null) {
         return 0.0F;
      } else {
         float sum = 0.0F;

         for (float f : layers) {
            sum += f;
         }

         return Math.min(1.0F, sum);
      }
   }

   public static void setDirt(UUID uuid, float val) {
      int activeType = getDirtType(uuid);
      setDirtLayer(uuid, activeType, val);
   }

   public static int getDirtType(UUID uuid) {
      return ACTIVE_DIRT_TYPES.getOrDefault(uuid, 0);
   }

   public static void setDirtType(UUID uuid, int type) {
      if (type >= 0 && type < 4) {
         ACTIVE_DIRT_TYPES.put(uuid, type);
      }
   }

   public static int getDirtMaskIndex(UUID uuid) {
      int activeType = getDirtType(uuid);
      return getDirtLayerMask(uuid, activeType);
   }

   public static void setDirtMaskIndex(UUID uuid, int index) {
      int activeType = getDirtType(uuid);
      setDirtLayerMask(uuid, activeType, index);
   }

   public static float getBlood(UUID uuid) {
      return BLOOD_LEVELS.getOrDefault(uuid, 0.0F);
   }

   public static void setBlood(UUID uuid, float val) {
      BLOOD_LEVELS.put(uuid, Math.max(0.0F, Math.min(1.0F, val)));
   }

   public static float getWetness(UUID uuid) {
      return WET_LEVELS.getOrDefault(uuid, 0.0F);
   }

   public static void setWetness(UUID uuid, float val) {
      WET_LEVELS.put(uuid, Math.max(0.0F, Math.min(1.0F, val)));
   }

   public static float getCharred(UUID uuid) {
      return CHAR_LEVELS.getOrDefault(uuid, 0.0F);
   }

   public static void setCharred(UUID uuid, float val) {
      CHAR_LEVELS.put(uuid, Math.max(0.0F, Math.min(1.0F, val)));
   }

   public static float getFrost(UUID uuid) {
      return FROST_LEVELS.getOrDefault(uuid, 0.0F);
   }

   public static void setFrost(UUID uuid, float val) {
      FROST_LEVELS.put(uuid, Math.max(0.0F, Math.min(1.0F, val)));
   }

   public static float getShakeIntensity(UUID uuid) {
      return SHAKE_INTENSITIES.getOrDefault(uuid, -1.0F);
   }

   public static void setShakeIntensity(UUID uuid, float val) {
      SHAKE_INTENSITIES.put(uuid, Math.max(-1.0F, Math.min(1.0F, val)));
   }

   public static int getBloodMaskIndex(UUID uuid) {
      return BLOOD_MASKS.computeIfAbsent(uuid, k -> ThreadLocalRandom.current().nextInt(9999) + 1);
   }

   public static int getWetMaskIndex(UUID uuid) {
      return WET_MASKS.computeIfAbsent(uuid, k -> ThreadLocalRandom.current().nextInt(9999) + 1);
   }

   public static int getCharMaskIndex(UUID uuid) {
      return CHAR_MASKS.computeIfAbsent(uuid, k -> ThreadLocalRandom.current().nextInt(9999) + 1);
   }

   public static int getFrostMaskIndex(UUID uuid) {
      return FROST_MASKS.computeIfAbsent(uuid, k -> ThreadLocalRandom.current().nextInt(9999) + 1);
   }

   public static void setBloodMaskIndex(UUID uuid, int index) {
      if (index <= 0) {
         BLOOD_MASKS.remove(uuid);
      } else {
         BLOOD_MASKS.put(uuid, index);
      }
   }

   public static void setWetMaskIndex(UUID uuid, int index) {
      if (index <= 0) {
         WET_MASKS.remove(uuid);
      } else {
         WET_MASKS.put(uuid, index);
      }
   }

   public static void setCharMaskIndex(UUID uuid, int index) {
      if (index <= 0) {
         CHAR_MASKS.remove(uuid);
      } else {
         CHAR_MASKS.put(uuid, index);
      }
   }

   public static void setFrostMaskIndex(UUID uuid, int index) {
      if (index <= 0) {
         FROST_MASKS.remove(uuid);
      } else {
         FROST_MASKS.put(uuid, index);
      }
   }

   public static float getDynamicEffectLevel(UUID uuid, Identifier effectId) {
      ConcurrentHashMap<Identifier, Float> levels = DYNAMIC_EFFECT_LEVELS.get(uuid);
      return levels == null ? 0.0F : levels.getOrDefault(effectId, 0.0F);
   }

   public static void setDynamicEffectLevel(UUID uuid, Identifier effectId, float val) {
      ConcurrentHashMap<Identifier, Float> levels = DYNAMIC_EFFECT_LEVELS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
      levels.put(effectId, Math.max(0.0F, Math.min(1.0F, val)));
   }

   public static int getDynamicEffectMask(UUID uuid, Identifier effectId) {
      ConcurrentHashMap<Identifier, Integer> masks = DYNAMIC_EFFECT_MASKS.get(uuid);
      if (masks == null) {
         masks = DYNAMIC_EFFECT_MASKS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
      }

      return masks.computeIfAbsent(effectId, k -> ThreadLocalRandom.current().nextInt(9999) + 1);
   }

   public static void setDynamicEffectMask(UUID uuid, Identifier effectId, int index) {
      ConcurrentHashMap<Identifier, Integer> masks = DYNAMIC_EFFECT_MASKS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
      if (index <= 0) {
         masks.remove(effectId);
      } else {
         masks.put(effectId, index);
      }
   }

   public static boolean isBreathingDisabled(UUID uuid) {
      return BREATHING_DISABLED.getOrDefault(uuid, false);
   }

   public static void setBreathingDisabled(UUID uuid, boolean val) {
      BREATHING_DISABLED.put(uuid, val);
   }

   public static boolean isShakeDisabled(UUID uuid) {
      return SHAKE_DISABLED.getOrDefault(uuid, false);
   }

   public static void setShakeDisabled(UUID uuid, boolean val) {
      SHAKE_DISABLED.put(uuid, val);
   }

   public static void clear(UUID uuid) {
      DIRT_LAYERS.remove(uuid);
      DIRT_LAYER_MASKS.remove(uuid);
      LAYER_BIOME_TYPES.remove(uuid);
      ACTIVE_DIRT_TYPES.remove(uuid);
      LAYER_RENDER_ORDER.remove(uuid);
      BLOOD_LEVELS.remove(uuid);
      WET_LEVELS.remove(uuid);
      CHAR_LEVELS.remove(uuid);
      FROST_LEVELS.remove(uuid);
      SHAKE_INTENSITIES.remove(uuid);
      BLOOD_MASKS.remove(uuid);
      WET_MASKS.remove(uuid);
      CHAR_MASKS.remove(uuid);
      FROST_MASKS.remove(uuid);
      DYNAMIC_EFFECT_LEVELS.remove(uuid);
      DYNAMIC_EFFECT_MASKS.remove(uuid);
      BREATHING_DISABLED.remove(uuid);
      SHAKE_DISABLED.remove(uuid);
   }
}
