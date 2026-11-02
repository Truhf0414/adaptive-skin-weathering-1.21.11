package com.example.weathering.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return FabricLoader.getInstance().isModLoaded("cloth-config2") ? ClothConfigScreenHelper.createScreenFactory() : parent -> null;
   }
}
