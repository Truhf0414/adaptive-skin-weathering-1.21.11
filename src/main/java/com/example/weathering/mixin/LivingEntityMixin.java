package com.example.weathering.mixin;

import com.example.weathering.WeatheringDataManager;
import com.example.weathering.network.WeatheringNetworking;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
   @Inject(method = "checkFallDamage", at = @At("HEAD"))
   private void onLivingEntityFall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo ci) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (!entity.level().isClientSide()) {
         if (onGround && entity.fallDistance > 3.0) {
            UUID uuid = entity.getUUID();
            String blockId = state.getBlock().getDescriptionId().toLowerCase();
            boolean isMuddy = blockId.contains("dirt")
               || blockId.contains("grass")
               || blockId.contains("mud")
               || blockId.contains("clay")
               || blockId.contains("podzol")
               || blockId.contains("mycelium")
               || blockId.contains("farmland")
               || blockId.contains("root")
               || blockId.contains("moss");
            boolean isSandy = blockId.contains("sand");
            boolean isSnowy = blockId.contains("snow") || blockId.contains("ice");
            boolean isDusty = blockId.contains("stone") || blockId.contains("gravel") || blockId.contains("slate");
            int dirtType = -1;
            if (isMuddy) {
               dirtType = 0;
            } else if (isSandy) {
               dirtType = 1;
            } else if (isDusty) {
               dirtType = 2;
            } else if (isSnowy) {
               dirtType = 3;
            }

            if (dirtType >= 0) {
               Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
               Identifier bId = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
               String bPath = bId != null ? bId.getPath().toLowerCase() : "";
               int bType = 0;
               if (bPath.contains("swamp") || bPath.contains("mangrove")) {
                  bType = 1;
               } else if (bPath.contains("jungle")) {
                  bType = 2;
               } else if (bPath.contains("taiga") || bPath.contains("snowy") || bPath.contains("cold") || bPath.contains("frozen") || bPath.contains("ice")) {
                  bType = 3;
               } else if (bPath.contains("badlands") || bPath.contains("mesa")) {
                  bType = 4;
               }

               WeatheringDataManager.setDirtType(uuid, dirtType);
               WeatheringDataManager.setLayerBiomeType(uuid, dirtType, bType);
               float currentLayerDirt = WeatheringDataManager.getDirtLayer(uuid, dirtType);
               float dirtAdded = (float)Math.min(0.12, (entity.fallDistance - 3.0) * 0.015);
               float newDirt = Math.min(1.0F, currentLayerDirt + dirtAdded);
               WeatheringDataManager.setDirtLayer(uuid, dirtType, newDirt);
               if (currentLayerDirt <= 0.01F && newDirt > 0.01F) {
                  WeatheringDataManager.promoteLayerToTop(uuid, dirtType);
               }

               WeatheringDataManager.getDirtLayerMask(uuid, dirtType);
               WeatheringNetworking.syncToClients(entity);
            }
         }
      }
   }
}
