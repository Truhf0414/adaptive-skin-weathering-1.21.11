package com.example.weathering;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import net.fabricmc.loader.api.FabricLoader;

public class WeatheringConfig {
   private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "skin_weathering.json");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public boolean showDirt = true;
   public boolean showBlood = true;
   public boolean showWetness = true;
   public boolean showCharred = true;
   public boolean showFrost = true;
   public boolean enableShaking = true;
   public boolean enableLayering = true;
   public boolean useAdaptiveDirt = true;
   public boolean useAdaptiveBlood = true;
   public int customBloodColor = 10880775;
   public boolean showFirstPersonParticles = true;
   public boolean sequentialDirt = true;
   public float dirtAccumulationRate = 2.0F;
   public float dirtBaseRate = 83.33F;
   public float dirtMudClayRate = 166.66F;
   public float dirtSandRate = 119.05F;
   public float dirtStoneDustRate = 64.1F;
   public float dirtSnowIceRate = 83.33F;
   public float dirtSprintingMultiplier = 1.5F;
   public float carpetCleaningRate = 10000.0F;
   public float dirtDripChance = 0.1F;
   public float bloodDamageMultiplier = 1.5F;
   public float zombieBloodAccumulationMultiplier = 0.33F;
   public boolean skeletonImmunity = true;
   public float lowHpBleedingThreshold = 0.3F;
   public float bloodDripChanceNormal = 0.08F;
   public float bloodDripChanceLowHp = 0.04F;
   public float wetnessDryRate = 1666.67F;
   public float wetnessDripChance = 0.12F;
   public float sootAccumulationRate = 0.003F;
   public float sootWaterWashRate = 0.02F;
   public float sootRainWashRate = 0.002F;
   public float sootNaturalDispersalRate = 160.0F;
   public int breathCycleTicks = 120;
   public int breathDurationTicks = 25;
   public float breathOpacity = 0.65F;
   public boolean breathOnlyInCold = true;
   public float waterDirtWashRate = 5000.0F;
   public float waterBloodWashRate = 5000.0F;
   public float rainDirtWashRate = 1667.0F;
   public float rainBloodWashRate = 1000.0F;
   public float frostFreezeSnowRate = 12000.0F;
   public float frostStormRate = 200.0F;
   public float frostMeltingWaterRate = 50000.0F;
   public float frostMeltingWarmRate = 5000.0F;
   public float frostMeltingNormalRate = 1000.0F;
   public float frostDripChance = 0.08F;
   public boolean consumeSoap = false;
   public boolean swampZombiesSpawnDirty = true;
   public float swampZombiesSpawnDirtLevel = 1.0F;
   public float createDirtWashRate = 0.04F;
   public float createBloodWashRate = 0.035F;
   public float createCharredWashRate = 0.05F;
   public float createWetnessFillRate = 0.06F;
   private static WeatheringConfig instance;

   public static WeatheringConfig get() {
      if (instance == null) {
         instance = load();
      }

      return instance;
   }

   private static WeatheringConfig load() {
      if (FILE.exists()) {
         try (FileReader reader = new FileReader(FILE)) {
            WeatheringConfig config = (WeatheringConfig)GSON.fromJson(reader, WeatheringConfig.class);
            if (config != null) {
               boolean migrated = false;
               if (config.dirtBaseRate > 0.0F && config.dirtBaseRate < 1.0F) {
                  config.dirtBaseRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.dirtMudClayRate > 0.0F && config.dirtMudClayRate < 1.0F) {
                  config.dirtMudClayRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.dirtSandRate > 0.0F && config.dirtSandRate < 1.0F) {
                  config.dirtSandRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.dirtStoneDustRate > 0.0F && config.dirtStoneDustRate < 1.0F) {
                  config.dirtStoneDustRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.dirtSnowIceRate > 0.0F && config.dirtSnowIceRate < 1.0F) {
                  config.dirtSnowIceRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.carpetCleaningRate > 0.0F && config.carpetCleaningRate < 1.0F) {
                  config.carpetCleaningRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.wetnessDryRate > 0.0F && config.wetnessDryRate < 1.0F) {
                  config.wetnessDryRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.sootNaturalDispersalRate > 0.0F && config.sootNaturalDispersalRate < 1.0F) {
                  config.sootNaturalDispersalRate *= 1000000.0F;
                  migrated = true;
               }

               if (config.waterDirtWashRate <= 0.0F) {
                  config.waterDirtWashRate = 5000.0F;
                  migrated = true;
               }

               if (config.waterBloodWashRate <= 0.0F) {
                  config.waterBloodWashRate = 5000.0F;
                  migrated = true;
               }

               if (config.rainDirtWashRate <= 0.0F) {
                  config.rainDirtWashRate = 1667.0F;
                  migrated = true;
               }

               if (config.rainBloodWashRate <= 0.0F) {
                  config.rainBloodWashRate = 1000.0F;
                  migrated = true;
               }

               if (migrated) {
                  config.save();
               }

               return config;
            }
         } catch (Exception var6) {
            var6.printStackTrace();
         }
      }

      WeatheringConfig newConfig = new WeatheringConfig();
      newConfig.save();
      return newConfig;
   }

   public void save() {
      try (FileWriter writer = new FileWriter(FILE)) {
         GSON.toJson(this, writer);
      } catch (Exception var6) {
         var6.printStackTrace();
      }
   }
}
