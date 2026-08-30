package com.derk.easyinventorycrafter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EasyInventoryCrafterConfig {
    public static final String CANONICAL_FILE_NAME = "seamless-crafting.json";
    private static final List<String> LEGACY_FILE_NAMES = List.of(
        "seamless_crafting.json",
        "derk_easy_inventory_crafter.json",
        "bluethooth_chest.json",
        "bluethooth-chest.json"
    );
    private static final Logger LOGGER = LoggerFactory.getLogger("Seamless Crafting/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static ConfigData data = ConfigData.defaults();

    private EasyInventoryCrafterConfig() {
    }

    public static synchronized void load(Path configDirectory) {
        configPath = configDirectory.resolve(CANONICAL_FILE_NAME);
        migrateLegacyFile(configDirectory);

        ConfigData loaded = null;
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                loaded = GSON.fromJson(reader, ConfigData.class);
            } catch (IOException | JsonSyntaxException exception) {
                backupInvalidConfig(configPath);
                LOGGER.warn("Could not read {}; defaults will be written", configPath, exception);
                loaded = null;
            }
        }

        data = sanitize(loaded);
        save();
    }

    public static synchronized void save() {
        if (configPath == null) {
            throw new IllegalStateException("Config must be loaded with a loader config directory before it can be saved");
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not write {}", configPath, exception);
        }
    }

    public static ConfigData snapshot() {
        return data.copy();
    }

    public static synchronized void update(ConfigData updated) {
        data = sanitize(updated);
        save();
    }

    private static void migrateLegacyFile(Path configDirectory) {
        if (Files.exists(configPath)) {
            return;
        }

        for (String legacyFileName : LEGACY_FILE_NAMES) {
            Path legacyPath = configDirectory.resolve(legacyFileName);
            if (!Files.isRegularFile(legacyPath)) {
                continue;
            }

            Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + ".pre-2.0.bak");
            try {
                Files.createDirectories(configDirectory);
                Files.copy(legacyPath, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                Files.copy(legacyPath, configPath, StandardCopyOption.COPY_ATTRIBUTES);
                LOGGER.info("Migrated legacy config {} to {}; backup retained at {}", legacyPath, configPath, backupPath);
            } catch (IOException exception) {
                LOGGER.warn("Could not migrate legacy config {}", legacyPath, exception);
            }
            return;
        }
    }

    private static void backupInvalidConfig(Path invalidPath) {
        if (!Files.isRegularFile(invalidPath)) {
            return;
        }
        String suffix = ".invalid-" + Instant.now().toEpochMilli() + ".bak";
        try {
            Files.copy(invalidPath, invalidPath.resolveSibling(invalidPath.getFileName() + suffix));
        } catch (IOException exception) {
            LOGGER.warn("Could not back up invalid config {}", invalidPath, exception);
        }
    }

    static synchronized void resetForTests() {
        configPath = null;
        data = ConfigData.defaults();
    }

    public static int getHighlightColor() {
        return data.highlightColor;
    }

    public static int getHighlightDurationTicks() {
        return data.highlightDurationTicks;
    }

    public static int getNearbyRadius() {
        return data.nearbyRadius;
    }

    public static float getHighlightOpacity() {
        return data.highlightOpacityPercent / 100.0f;
    }

    public static boolean isDistanceLabelEnabled() {
        return data.showDistanceLabel;
    }

    public static boolean isHighlightEnabled() {
        return data.showHighlighter;
    }

    public static boolean isSnapAimEnabled() {
        return data.snapAimToChest;
    }

    public static boolean isLocateTrailEnabled() {
        return data.showLocateTrail;
    }

    public static LocateTrailParticle getLocateTrailParticle() {
        return data.locateTrailParticle;
    }

    public static boolean isNearbyPanelOpenByDefault() {
        return data.nearbyPanelOpenByDefault;
    }

    public static int getAutoRefreshTicks() {
        return data.autoRefreshTicks;
    }

    public static float getNearbyPanelOpacity() {
        return data.nearbyPanelOpacityPercent / 100.0f;
    }

    private static ConfigData sanitize(ConfigData source) {
        ConfigData sanitized = source == null ? ConfigData.defaults() : source.copy();
        sanitized.showLocateTrail = sanitized.resolveLocateTrail();
        sanitized.showSmokeTrail = null;
        if (sanitized.locateTrailParticle == null) {
            sanitized.locateTrailParticle = LocateTrailParticle.WATER_EVAPORATION;
        }
        sanitized.highlightColor = clampColor(sanitized.highlightColor);
        sanitized.highlightDurationTicks = clamp(sanitized.highlightDurationTicks, 10, 20 * 60);
        sanitized.nearbyRadius = clamp(sanitized.nearbyRadius, 1, 64);
        sanitized.highlightOpacityPercent = clamp(sanitized.highlightOpacityPercent, 5, 100);
        sanitized.nearbyPanelOpacityPercent = clamp(sanitized.nearbyPanelOpacityPercent, 5, 100);
        sanitized.autoRefreshTicks = clamp(sanitized.autoRefreshTicks, 5, 20 * 30);
        return sanitized;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampColor(int color) {
        return color & 0xFFFFFF;
    }

    public static final class ConfigData {
        public int highlightColor;
        public int highlightDurationTicks;
        public int nearbyRadius;
        public int highlightOpacityPercent;
        public int nearbyPanelOpacityPercent;
        public boolean showHighlighter;
        public boolean showDistanceLabel;
        public boolean snapAimToChest;
        public Boolean showLocateTrail;
        public Boolean showSmokeTrail;
        public LocateTrailParticle locateTrailParticle;
        public boolean nearbyPanelOpenByDefault;
        public int autoRefreshTicks;

        public static ConfigData defaults() {
            ConfigData defaults = new ConfigData();
            defaults.highlightColor = 0xFFD700;
            defaults.highlightDurationTicks = 100;
            defaults.nearbyRadius = 16;
            defaults.highlightOpacityPercent = 35;
            defaults.nearbyPanelOpacityPercent = 40;
            defaults.showHighlighter = true;
            defaults.showDistanceLabel = true;
            defaults.snapAimToChest = false;
            defaults.showLocateTrail = true;
            defaults.showSmokeTrail = null;
            defaults.locateTrailParticle = LocateTrailParticle.WATER_EVAPORATION;
            defaults.nearbyPanelOpenByDefault = true;
            defaults.autoRefreshTicks = 20;
            return defaults;
        }

        public boolean resolveLocateTrail() {
            if (showLocateTrail != null) {
                return showLocateTrail;
            }
            if (showSmokeTrail != null) {
                return showSmokeTrail;
            }
            return true;
        }

        public ConfigData copy() {
            ConfigData copy = new ConfigData();
            copy.highlightColor = highlightColor;
            copy.highlightDurationTicks = highlightDurationTicks;
            copy.nearbyRadius = nearbyRadius;
            copy.highlightOpacityPercent = highlightOpacityPercent;
            copy.nearbyPanelOpacityPercent = nearbyPanelOpacityPercent;
            copy.showHighlighter = showHighlighter;
            copy.showDistanceLabel = showDistanceLabel;
            copy.snapAimToChest = snapAimToChest;
            copy.showLocateTrail = resolveLocateTrail();
            copy.showSmokeTrail = showSmokeTrail;
            copy.locateTrailParticle = locateTrailParticle;
            copy.nearbyPanelOpenByDefault = nearbyPanelOpenByDefault;
            copy.autoRefreshTicks = autoRefreshTicks;
            return copy;
        }
    }

    public enum LocateTrailParticle {
        WATER_EVAPORATION("Water Evap."),
        SMOKE("Smoke"),
        END_ROD("End Rod");

        private final String label;

        LocateTrailParticle(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public LocateTrailParticle next() {
            LocateTrailParticle[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
