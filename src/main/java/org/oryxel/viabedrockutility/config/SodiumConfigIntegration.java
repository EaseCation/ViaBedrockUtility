package org.oryxel.viabedrockutility.config;

//? if >=1.21.11 {
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SodiumConfigIntegration implements ConfigEntryPoint {
    private static final Identifier PRESET_ID = Identifier.of("viabedrockutility", "lod_preset");

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        final LodConfig config = LodConfig.getInstance();

        builder.registerOwnModOptions()
                .addPage(builder.createOptionPage()
                        .setName(Text.translatable("config.viabedrockutility.category.entity_animation_lod"))
                        .addOptionGroup(builder.createOptionGroup()
                                // Preset selector
                                .addOption(builder.createEnumOption(PRESET_ID, LodConfig.Preset.class)
                                        .setName(Text.translatable("config.viabedrockutility.option.preset"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.preset.tooltip"))
                                        .setElementNameProvider(preset -> switch (preset) {
                                            case HIGH_QUALITY -> Text.translatable("config.viabedrockutility.preset.high_quality");
                                            case BALANCED -> Text.translatable("config.viabedrockutility.preset.balanced");
                                            case PERFORMANCE -> Text.translatable("config.viabedrockutility.preset.performance");
                                            case CUSTOM -> Text.translatable("config.viabedrockutility.preset.custom");
                                        })
                                        .setBinding(config::applyPreset, config::getPreset)
                                        .setDefaultValue(LodConfig.Preset.BALANCED)
                                        .setStorageHandler(config::save))

                                // Tier 1 Distance
                                .addOption(builder.createIntegerOption(
                                                Identifier.of("viabedrockutility", "lod_tier1_distance"))
                                        .setName(Text.translatable("config.viabedrockutility.option.tier1_distance"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.tier1_distance.tooltip"))
                                        .setEnabledProvider(state -> state.readEnumOption(PRESET_ID, LodConfig.Preset.class) == LodConfig.Preset.CUSTOM, PRESET_ID)
                                        .setRange(0, 64, 1)
                                        .setValueFormatter(val -> val == 0
                                                ? Text.translatable("config.viabedrockutility.value.disabled")
                                                : Text.translatable("config.viabedrockutility.value.blocks", val))
                                        .setBinding(val -> config.getTier1().distance = val,
                                                () -> (int) config.getTier1().distance)
                                        .setDefaultValue(24)
                                        .setStorageHandler(config::save))

                                // Tier 1 Frame Interval
                                .addOption(builder.createIntegerOption(
                                                Identifier.of("viabedrockutility", "lod_tier1_interval"))
                                        .setName(Text.translatable("config.viabedrockutility.option.tier1_interval"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.tier1_interval.tooltip"))
                                        .setEnabledProvider(state -> state.readEnumOption(PRESET_ID, LodConfig.Preset.class) == LodConfig.Preset.CUSTOM, PRESET_ID)
                                        .setRange(1, 16, 1)
                                        .setValueFormatter(val -> Text.translatable("config.viabedrockutility.value.every_frames", val))
                                        .setBinding(val -> config.getTier1().frameInterval = val,
                                                () -> config.getTier1().frameInterval)
                                        .setDefaultValue(2)
                                        .setStorageHandler(config::save))

                                // Tier 2 Distance
                                .addOption(builder.createIntegerOption(
                                                Identifier.of("viabedrockutility", "lod_tier2_distance"))
                                        .setName(Text.translatable("config.viabedrockutility.option.tier2_distance"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.tier2_distance.tooltip"))
                                        .setEnabledProvider(state -> state.readEnumOption(PRESET_ID, LodConfig.Preset.class) == LodConfig.Preset.CUSTOM, PRESET_ID)
                                        .setRange(0, 96, 1)
                                        .setValueFormatter(val -> val == 0
                                                ? Text.translatable("config.viabedrockutility.value.disabled")
                                                : Text.translatable("config.viabedrockutility.value.blocks", val))
                                        .setBinding(val -> config.getTier2().distance = val,
                                                () -> (int) config.getTier2().distance)
                                        .setDefaultValue(48)
                                        .setStorageHandler(config::save))

                                // Tier 2 Frame Interval
                                .addOption(builder.createIntegerOption(
                                                Identifier.of("viabedrockutility", "lod_tier2_interval"))
                                        .setName(Text.translatable("config.viabedrockutility.option.tier2_interval"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.tier2_interval.tooltip"))
                                        .setEnabledProvider(state -> state.readEnumOption(PRESET_ID, LodConfig.Preset.class) == LodConfig.Preset.CUSTOM, PRESET_ID)
                                        .setRange(1, 16, 1)
                                        .setValueFormatter(val -> Text.translatable("config.viabedrockutility.value.every_frames", val))
                                        .setBinding(val -> config.getTier2().frameInterval = val,
                                                () -> config.getTier2().frameInterval)
                                        .setDefaultValue(4)
                                        .setStorageHandler(config::save))

                                // Tier 3 Distance
                                .addOption(builder.createIntegerOption(
                                                Identifier.of("viabedrockutility", "lod_tier3_distance"))
                                        .setName(Text.translatable("config.viabedrockutility.option.tier3_distance"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.tier3_distance.tooltip"))
                                        .setEnabledProvider(state -> state.readEnumOption(PRESET_ID, LodConfig.Preset.class) == LodConfig.Preset.CUSTOM, PRESET_ID)
                                        .setRange(0, 128, 1)
                                        .setValueFormatter(val -> val == 0
                                                ? Text.translatable("config.viabedrockutility.value.disabled")
                                                : Text.translatable("config.viabedrockutility.value.blocks", val))
                                        .setBinding(val -> config.getTier3().distance = val,
                                                () -> (int) config.getTier3().distance)
                                        .setDefaultValue(0)
                                        .setStorageHandler(config::save))

                                // Tier 3 Frame Interval
                                .addOption(builder.createIntegerOption(
                                                Identifier.of("viabedrockutility", "lod_tier3_interval"))
                                        .setName(Text.translatable("config.viabedrockutility.option.tier3_interval"))
                                        .setTooltip(Text.translatable("config.viabedrockutility.option.tier3_interval.tooltip"))
                                        .setEnabledProvider(state -> state.readEnumOption(PRESET_ID, LodConfig.Preset.class) == LodConfig.Preset.CUSTOM, PRESET_ID)
                                        .setRange(1, 16, 1)
                                        .setValueFormatter(val -> Text.translatable("config.viabedrockutility.value.every_frames", val))
                                        .setBinding(val -> config.getTier3().frameInterval = val,
                                                () -> config.getTier3().frameInterval)
                                        .setDefaultValue(1)
                                        .setStorageHandler(config::save))
                        )
                );
    }
}
//?} else {
/*
public class SodiumConfigIntegration {
}
*///?}
