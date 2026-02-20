package org.oryxel.viabedrockutility.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LodConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LodConfig INSTANCE;

    public enum Preset {
        HIGH_QUALITY,
        BALANCED,
        PERFORMANCE,
        CUSTOM
    }

    public static class LodTier {
        public double distance;
        public int frameInterval;

        public LodTier() {}

        public LodTier(double distance, int frameInterval) {
            this.distance = distance;
            this.frameInterval = frameInterval;
        }
    }

    private Preset preset = Preset.BALANCED;
    private LodTier tier1 = new LodTier(24.0, 2);
    private LodTier tier2 = new LodTier(48.0, 4);
    private LodTier tier3 = new LodTier(0, 1);

    public static LodConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public Preset getPreset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
    }

    public LodTier getTier1() {
        return tier1;
    }

    public LodTier getTier2() {
        return tier2;
    }

    public LodTier getTier3() {
        return tier3;
    }

    /**
     * Determines whether animation should be computed this frame based on distance and frame counter.
     * Checks tiers from farthest to nearest; enabled tiers have distance > 0.
     */
    public boolean shouldAnimate(double distance, int frameCounter) {
        if (preset == Preset.HIGH_QUALITY) {
            return true;
        }

        // Check tiers from farthest to nearest
        if (tier3.distance > 0 && distance > tier3.distance) {
            return frameCounter % tier3.frameInterval == 0;
        }
        if (tier2.distance > 0 && distance > tier2.distance) {
            return frameCounter % tier2.frameInterval == 0;
        }
        if (tier1.distance > 0 && distance > tier1.distance) {
            return frameCounter % tier1.frameInterval == 0;
        }

        return true;
    }

    /**
     * Applies preset values to tier parameters.
     */
    public void applyPreset(Preset preset) {
        this.preset = preset;
        switch (preset) {
            case HIGH_QUALITY -> {
                tier1 = new LodTier(0, 1);
                tier2 = new LodTier(0, 1);
                tier3 = new LodTier(0, 1);
            }
            case BALANCED -> {
                tier1 = new LodTier(24.0, 2);
                tier2 = new LodTier(48.0, 4);
                tier3 = new LodTier(0, 1);
            }
            case PERFORMANCE -> {
                tier1 = new LodTier(16.0, 2);
                tier2 = new LodTier(32.0, 4);
                tier3 = new LodTier(48.0, 8);
            }
            case CUSTOM -> {} // Keep current values
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("viabedrockutility.json");
    }

    public static void load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                INSTANCE = GSON.fromJson(json, LodConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new LodConfig();
                }
                ViaBedrockUtilityFabric.LOGGER.debug("[Config] Loaded LOD config: preset={}", INSTANCE.preset);
            } catch (Exception e) {
                ViaBedrockUtilityFabric.LOGGER.warn("[Config] Failed to load config, using defaults", e);
                INSTANCE = new LodConfig();
            }
        } else {
            INSTANCE = new LodConfig();
            INSTANCE.save();
            ViaBedrockUtilityFabric.LOGGER.debug("[Config] Created default config file");
        }
    }

    public void save() {
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            ViaBedrockUtilityFabric.LOGGER.warn("[Config] Failed to save config", e);
        }
    }
}
