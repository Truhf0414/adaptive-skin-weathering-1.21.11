package com.example.weathering.client;

import com.example.weathering.WeatheringConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ClothConfigScreenHelper {
   public static ConfigScreenFactory<?> createScreenFactory() {
      return parent -> {
         ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.skin_weathering.title"));
         ConfigEntryBuilder entryBuilder = builder.entryBuilder();
         WeatheringConfig config = WeatheringConfig.get();
         ConfigCategory catGeneral = builder.getOrCreateCategory(Component.translatable("text.skin_weathering.category.general"));
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.showFirstPersonParticles"), config.showFirstPersonParticles)
               .setDefaultValue(true)
               .setTooltip(new Component[]{Component.translatable("text.skin_weathering.option.showFirstPersonParticles.tooltip")})
               .setSaveConsumer(newValue -> config.showFirstPersonParticles = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.showDirt"), config.showDirt)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.showDirt = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.showBlood"), config.showBlood)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.showBlood = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.showWetness"), config.showWetness)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.showWetness = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.showCharred"), config.showCharred)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.showCharred = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.showFrost"), config.showFrost)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.showFrost = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.enableShaking"), config.enableShaking)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.enableShaking = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.useAdaptiveDirt"), config.useAdaptiveDirt)
               .setDefaultValue(true)
               .setTooltip(new Component[]{Component.translatable("text.skin_weathering.option.useAdaptiveDirt.tooltip")})
               .setSaveConsumer(newValue -> config.useAdaptiveDirt = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.useAdaptiveBlood"), config.useAdaptiveBlood)
               .setDefaultValue(true)
               .setTooltip(new Component[]{Component.translatable("text.skin_weathering.option.useAdaptiveBlood.tooltip")})
               .setSaveConsumer(newValue -> config.useAdaptiveBlood = newValue)
               .build()
         );
         catGeneral.addEntry(
            entryBuilder.startColorField(Component.translatable("text.skin_weathering.option.customBloodColor"), config.customBloodColor)
               .setDefaultValue(10880775)
               .setSaveConsumer(newValue -> config.customBloodColor = newValue)
               .build()
         );
         ConfigCategory catDirt = builder.getOrCreateCategory(Component.translatable("text.skin_weathering.category.dirt"));
         catDirt.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.sequentialDirt"), config.sequentialDirt)
               .setDefaultValue(true)
               .setTooltip(new Component[]{Component.translatable("text.skin_weathering.option.sequentialDirt.tooltip")})
               .setSaveConsumer(newValue -> config.sequentialDirt = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.enableLayering"), config.enableLayering)
               .setDefaultValue(true)
               .setTooltip(new Component[]{Component.translatable("text.skin_weathering.option.enableLayering.tooltip")})
               .setSaveConsumer(newValue -> config.enableLayering = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtAccumulationRate"), config.dirtAccumulationRate)
               .setDefaultValue(2.0F)
               .setMin(0.0F)
               .setMax(1000.0F)
               .setSaveConsumer(newValue -> config.dirtAccumulationRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtBaseRate"), config.dirtBaseRate)
               .setDefaultValue(83.33F)
               .setSaveConsumer(newValue -> config.dirtBaseRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtMudClayRate"), config.dirtMudClayRate)
               .setDefaultValue(166.66F)
               .setSaveConsumer(newValue -> config.dirtMudClayRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtSandRate"), config.dirtSandRate)
               .setDefaultValue(119.05F)
               .setSaveConsumer(newValue -> config.dirtSandRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtStoneDustRate"), config.dirtStoneDustRate)
               .setDefaultValue(64.1F)
               .setSaveConsumer(newValue -> config.dirtStoneDustRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtSnowIceRate"), config.dirtSnowIceRate)
               .setDefaultValue(83.33F)
               .setSaveConsumer(newValue -> config.dirtSnowIceRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtSprintingMultiplier"), config.dirtSprintingMultiplier)
               .setDefaultValue(1.5F)
               .setMin(1.0F)
               .setMax(100.0F)
               .setSaveConsumer(newValue -> config.dirtSprintingMultiplier = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.carpetCleaningRate"), config.carpetCleaningRate)
               .setDefaultValue(10000.0F)
               .setSaveConsumer(newValue -> config.carpetCleaningRate = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.dirtDripChance"), config.dirtDripChance)
               .setDefaultValue(0.1F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.dirtDripChance = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.swampZombiesSpawnDirty"), config.swampZombiesSpawnDirty)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.swampZombiesSpawnDirty = newValue)
               .build()
         );
         catDirt.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.swampZombiesSpawnDirtLevel"), config.swampZombiesSpawnDirtLevel)
               .setDefaultValue(1.0F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.swampZombiesSpawnDirtLevel = newValue)
               .build()
         );
         ConfigCategory catBlood = builder.getOrCreateCategory(Component.translatable("text.skin_weathering.category.blood"));
         catBlood.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.bloodDamageMultiplier"), config.bloodDamageMultiplier)
               .setDefaultValue(1.5F)
               .setMin(0.0F)
               .setMax(100.0F)
               .setSaveConsumer(newValue -> config.bloodDamageMultiplier = newValue)
               .build()
         );
         catBlood.addEntry(
            entryBuilder.startFloatField(
                  Component.translatable("text.skin_weathering.option.zombieBloodAccumulationMultiplier"), config.zombieBloodAccumulationMultiplier
               )
               .setDefaultValue(0.33F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.zombieBloodAccumulationMultiplier = newValue)
               .build()
         );
         catBlood.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.skeletonImmunity"), config.skeletonImmunity)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.skeletonImmunity = newValue)
               .build()
         );
         catBlood.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.lowHpBleedingThreshold"), config.lowHpBleedingThreshold)
               .setDefaultValue(0.3F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.lowHpBleedingThreshold = newValue)
               .build()
         );
         catBlood.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.bloodDripChanceNormal"), config.bloodDripChanceNormal)
               .setDefaultValue(0.08F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.bloodDripChanceNormal = newValue)
               .build()
         );
         catBlood.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.bloodDripChanceLowHp"), config.bloodDripChanceLowHp)
               .setDefaultValue(0.04F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.bloodDripChanceLowHp = newValue)
               .build()
         );
         ConfigCategory catWetSoot = builder.getOrCreateCategory(Component.translatable("text.skin_weathering.category.wetness_soot"));
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.wetnessDryRate"), config.wetnessDryRate)
               .setDefaultValue(1666.67F)
               .setSaveConsumer(newValue -> config.wetnessDryRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.wetnessDripChance"), config.wetnessDripChance)
               .setDefaultValue(0.12F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.wetnessDripChance = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.sootAccumulationRate"), config.sootAccumulationRate)
               .setDefaultValue(0.003F)
               .setSaveConsumer(newValue -> config.sootAccumulationRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.sootWaterWashRate"), config.sootWaterWashRate)
               .setDefaultValue(0.02F)
               .setSaveConsumer(newValue -> config.sootWaterWashRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.sootRainWashRate"), config.sootRainWashRate)
               .setDefaultValue(0.002F)
               .setSaveConsumer(newValue -> config.sootRainWashRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.sootNaturalDispersalRate"), config.sootNaturalDispersalRate)
               .setDefaultValue(160.0F)
               .setSaveConsumer(newValue -> config.sootNaturalDispersalRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startIntField(Component.translatable("text.skin_weathering.option.breathCycleTicks"), config.breathCycleTicks)
               .setDefaultValue(120)
               .setSaveConsumer(newValue -> config.breathCycleTicks = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startIntField(Component.translatable("text.skin_weathering.option.breathDurationTicks"), config.breathDurationTicks)
               .setDefaultValue(25)
               .setSaveConsumer(newValue -> config.breathDurationTicks = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.breathOpacity"), config.breathOpacity)
               .setDefaultValue(0.65F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.breathOpacity = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.breathOnlyInCold"), config.breathOnlyInCold)
               .setDefaultValue(true)
               .setSaveConsumer(newValue -> config.breathOnlyInCold = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.waterDirtWashRate"), config.waterDirtWashRate)
               .setDefaultValue(5000.0F)
               .setSaveConsumer(newValue -> config.waterDirtWashRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.waterBloodWashRate"), config.waterBloodWashRate)
               .setDefaultValue(5000.0F)
               .setSaveConsumer(newValue -> config.waterBloodWashRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.rainDirtWashRate"), config.rainDirtWashRate)
               .setDefaultValue(1667.0F)
               .setSaveConsumer(newValue -> config.rainDirtWashRate = newValue)
               .build()
         );
         catWetSoot.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.rainBloodWashRate"), config.rainBloodWashRate)
               .setDefaultValue(1000.0F)
               .setSaveConsumer(newValue -> config.rainBloodWashRate = newValue)
               .build()
         );
         ConfigCategory catFrost = builder.getOrCreateCategory(Component.translatable("text.skin_weathering.category.frost"));
         catFrost.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.frostFreezeSnowRate"), config.frostFreezeSnowRate)
               .setDefaultValue(12000.0F)
               .setSaveConsumer(newValue -> config.frostFreezeSnowRate = newValue)
               .build()
         );
         catFrost.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.frostStormRate"), config.frostStormRate)
               .setDefaultValue(200.0F)
               .setSaveConsumer(newValue -> config.frostStormRate = newValue)
               .build()
         );
         catFrost.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.frostMeltingWaterRate"), config.frostMeltingWaterRate)
               .setDefaultValue(50000.0F)
               .setSaveConsumer(newValue -> config.frostMeltingWaterRate = newValue)
               .build()
         );
         catFrost.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.frostMeltingWarmRate"), config.frostMeltingWarmRate)
               .setDefaultValue(5000.0F)
               .setSaveConsumer(newValue -> config.frostMeltingWarmRate = newValue)
               .build()
         );
         catFrost.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.frostMeltingNormalRate"), config.frostMeltingNormalRate)
               .setDefaultValue(1000.0F)
               .setSaveConsumer(newValue -> config.frostMeltingNormalRate = newValue)
               .build()
         );
         catFrost.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.frostDripChance"), config.frostDripChance)
               .setDefaultValue(0.08F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.frostDripChance = newValue)
               .build()
         );
         ConfigCategory catMods = builder.getOrCreateCategory(Component.translatable("text.skin_weathering.category.integrations"));
         catMods.addEntry(
            entryBuilder.startBooleanToggle(Component.translatable("text.skin_weathering.option.consumeSoap"), config.consumeSoap)
               .setDefaultValue(false)
               .setSaveConsumer(newValue -> config.consumeSoap = newValue)
               .build()
         );
         catMods.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.createDirtWashRate"), config.createDirtWashRate)
               .setDefaultValue(0.04F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.createDirtWashRate = newValue)
               .build()
         );
         catMods.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.createBloodWashRate"), config.createBloodWashRate)
               .setDefaultValue(0.035F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.createBloodWashRate = newValue)
               .build()
         );
         catMods.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.createCharredWashRate"), config.createCharredWashRate)
               .setDefaultValue(0.05F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.createCharredWashRate = newValue)
               .build()
         );
         catMods.addEntry(
            entryBuilder.startFloatField(Component.translatable("text.skin_weathering.option.createWetnessFillRate"), config.createWetnessFillRate)
               .setDefaultValue(0.06F)
               .setMin(0.0F)
               .setMax(1.0F)
               .setSaveConsumer(newValue -> config.createWetnessFillRate = newValue)
               .build()
         );
         builder.setSavingRunnable(config::save);
         return builder.build();
      };
   }
}
