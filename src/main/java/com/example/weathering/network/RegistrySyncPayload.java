package com.example.weathering.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record RegistrySyncPayload(List<RegistrySyncPayload.EffectData> effects) implements CustomPacketPayload {
   public static final Type<RegistrySyncPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("skin_weathering", "registry_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RegistrySyncPayload> STREAM_CODEC = StreamCodec.of((buf, value) -> {
      buf.writeInt(value.effects.size());

      for (RegistrySyncPayload.EffectData eff : value.effects) {
         buf.writeIdentifier(eff.id());
         buf.writeInt(eff.textures().size());

         for (Identifier tex : eff.textures()) {
            buf.writeIdentifier(tex);
         }

         buf.writeInt(eff.defaultColor());
         buf.writeFloat(eff.scale());
         buf.writeBoolean(eff.useAdaptiveColor());
         buf.writeUtf(eff.blendMode());
         buf.writeBoolean(eff.hideOuterLayers());
      }
   }, buf -> {
      int size = buf.readInt();
      List<RegistrySyncPayload.EffectData> effects = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         Identifier id = buf.readIdentifier();
         int texSize = buf.readInt();
         List<Identifier> textures = new ArrayList<>();

         for (int j = 0; j < texSize; j++) {
            textures.add(buf.readIdentifier());
         }

         int defaultColor = buf.readInt();
         float scale = buf.readFloat();
         boolean useAdaptiveColor = buf.readBoolean();
         String blendMode = buf.readUtf();
         boolean hideOuterLayers = buf.readBoolean();
         effects.add(new RegistrySyncPayload.EffectData(id, textures, defaultColor, scale, useAdaptiveColor, blendMode, hideOuterLayers));
      }

      return new RegistrySyncPayload(effects);
   });

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record EffectData(
      Identifier id, List<Identifier> textures, int defaultColor, float scale, boolean useAdaptiveColor, String blendMode, boolean hideOuterLayers
   ) {
   }
}
