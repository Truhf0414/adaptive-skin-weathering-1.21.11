package com.example.weathering.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record WeatheringSyncPayload(
   int entityId,
   float[] dirtLayers,
   float blood,
   float wetness,
   float charred,
   float frost,
   int dirtType,
   int[] dirtMasks,
   int[] layerBiomes,
   int[] layerOrder,
   int bloodMaskIndex,
   int wetMaskIndex,
   int charMaskIndex,
   int frostMaskIndex,
   float shakeIntensity,
   boolean breathingDisabled,
   boolean shakeDisabled,
   List<WeatheringSyncPayload.DynamicEffectData> dynamicEffects
) implements CustomPacketPayload {
   public static final Type<WeatheringSyncPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("skin_weathering", "sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, WeatheringSyncPayload> STREAM_CODEC = StreamCodec.of(
      (buf, value) -> {
         buf.writeInt(value.entityId);

         for (int i = 0; i < 4; i++) {
            buf.writeFloat(value.dirtLayers[i]);
         }

         buf.writeFloat(value.blood);
         buf.writeFloat(value.wetness);
         buf.writeFloat(value.charred);
         buf.writeFloat(value.frost);
         buf.writeInt(value.dirtType);

         for (int i = 0; i < 4; i++) {
            buf.writeInt(value.dirtMasks[i]);
         }

         for (int i = 0; i < 4; i++) {
            buf.writeInt(value.layerBiomes[i]);
         }

         for (int i = 0; i < 4; i++) {
            buf.writeInt(value.layerOrder[i]);
         }

         buf.writeInt(value.bloodMaskIndex);
         buf.writeInt(value.wetMaskIndex);
         buf.writeInt(value.charMaskIndex);
         buf.writeInt(value.frostMaskIndex);
         buf.writeFloat(value.shakeIntensity);
         buf.writeBoolean(value.breathingDisabled);
         buf.writeBoolean(value.shakeDisabled);
         buf.writeInt(value.dynamicEffects.size());

         for (WeatheringSyncPayload.DynamicEffectData eff : value.dynamicEffects) {
            buf.writeIdentifier(eff.id());
            buf.writeFloat(eff.level());
            buf.writeInt(eff.mask());
         }
      },
      buf -> {
         int entityId = buf.readInt();
         float[] dirtLayers = new float[4];

         for (int i = 0; i < 4; i++) {
            dirtLayers[i] = buf.readFloat();
         }

         float blood = buf.readFloat();
         float wetness = buf.readFloat();
         float charred = buf.readFloat();
         float frost = buf.readFloat();
         int dirtType = buf.readInt();
         int[] dirtMasks = new int[4];

         for (int i = 0; i < 4; i++) {
            dirtMasks[i] = buf.readInt();
         }

         int[] layerBiomes = new int[4];

         for (int i = 0; i < 4; i++) {
            layerBiomes[i] = buf.readInt();
         }

         int[] layerOrder = new int[4];

         for (int i = 0; i < 4; i++) {
            layerOrder[i] = buf.readInt();
         }

         int bloodMaskIndex = buf.readInt();
         int wetMaskIndex = buf.readInt();
         int charMaskIndex = buf.readInt();
         int frostMaskIndex = buf.readInt();
         float shakeIntensity = buf.readFloat();
         boolean breathingDisabled = buf.readBoolean();
         boolean shakeDisabled = buf.readBoolean();
         int dynCount = buf.readInt();
         List<WeatheringSyncPayload.DynamicEffectData> dynamicEffects = new ArrayList<>();

         for (int i = 0; i < dynCount; i++) {
            dynamicEffects.add(new WeatheringSyncPayload.DynamicEffectData(buf.readIdentifier(), buf.readFloat(), buf.readInt()));
         }

         return new WeatheringSyncPayload(
            entityId,
            dirtLayers,
            blood,
            wetness,
            charred,
            frost,
            dirtType,
            dirtMasks,
            layerBiomes,
            layerOrder,
            bloodMaskIndex,
            wetMaskIndex,
            charMaskIndex,
            frostMaskIndex,
            shakeIntensity,
            breathingDisabled,
            shakeDisabled,
            dynamicEffects
         );
      }
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record DynamicEffectData(Identifier id, float level, int mask) {
   }
}
