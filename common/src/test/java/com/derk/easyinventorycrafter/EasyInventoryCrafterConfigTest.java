package com.derk.easyinventorycrafter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EasyInventoryCrafterConfigTest {
    @TempDir
    Path configDirectory;

    @AfterEach
    void resetConfig() {
        EasyInventoryCrafterConfig.resetForTests();
    }

    @Test
    void migratesForgeFilenameAndRetainsBackup() throws IOException {
        Path legacy = configDirectory.resolve("derk_easy_inventory_crafter.json");
        Files.writeString(legacy, """
            {
              "nearbyRadius": 24,
              "highlightColor": 1193046,
              "highlightDurationTicks": 80,
              "highlightOpacityPercent": 50,
              "nearbyPanelOpacityPercent": 45,
              "autoRefreshTicks": 30,
              "showSmokeTrail": false
            }
            """);

        EasyInventoryCrafterConfig.load(configDirectory);

        assertEquals(24, EasyInventoryCrafterConfig.getNearbyRadius());
        assertFalse(EasyInventoryCrafterConfig.isLocateTrailEnabled());
        assertTrue(Files.isRegularFile(configDirectory.resolve("seamless-crafting.json")));
        assertTrue(Files.isRegularFile(configDirectory.resolve("derk_easy_inventory_crafter.json.pre-2.0.bak")));
    }

    @Test
    void invalidConfigIsBackedUpAndReplacedWithDefaults() throws IOException {
        Path canonical = configDirectory.resolve("seamless-crafting.json");
        Files.writeString(canonical, "{ definitely not json");

        EasyInventoryCrafterConfig.load(configDirectory);

        assertEquals(16, EasyInventoryCrafterConfig.getNearbyRadius());
        assertTrue(Files.readString(canonical).contains("\"nearbyRadius\": 16"));
        try (var paths = Files.list(configDirectory)) {
            assertTrue(paths.anyMatch(path -> path.getFileName().toString().startsWith("seamless-crafting.json.invalid-")));
        }
    }
}
