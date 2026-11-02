package com.example.weathering.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

public class SkinEffectRegistry {
   private static final Map<Identifier, SkinEffect> EFFECTS = new LinkedHashMap<>();

   public static synchronized void register(SkinEffect effect) {
      if (effect == null) {
         throw new IllegalArgumentException("Cannot register a null SkinEffect");
      } else {
         EFFECTS.put(effect.getId(), effect);
      }
   }

   public static synchronized SkinEffect get(Identifier id) {
      return EFFECTS.get(id);
   }

   public static synchronized Collection<SkinEffect> getAll() {
      return EFFECTS.values();
   }
}
