package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.ConfigData;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.LocateTrailParticle;
import io.github.derkottersberg.seamlesscrafting.internal.client.SettingsScreen;
import java.util.Locale;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EasyInventoryCrafterConfigScreen extends SettingsScreen {
    private ConfigData draft;
    private String color, duration, radius, highlightOpacity, panelOpacity, refresh;

    public EasyInventoryCrafterConfigScreen(Screen parent) {
        super(parent, "Seamless Crafting Settings", "Appearance: this client. Search range: singleplayer / server.");
        setDraft(EasyInventoryCrafterConfig.snapshot());
    }

    private void setDraft(ConfigData data) {
        this.draft = data;
        this.color = String.format(Locale.ROOT, "#%06X", data.highlightColor);
        this.duration = seconds(data.highlightDurationTicks);
        this.radius = Integer.toString(data.nearbyRadius);
        this.highlightOpacity = Integer.toString(data.highlightOpacityPercent);
        this.panelOpacity = Integer.toString(data.nearbyPanelOpacityPercent);
        this.refresh = seconds(data.autoRefreshTicks);
    }

    @Override
    protected void buildSettings() {
        textSetting("Nearby items", "Search range (blocks)", "1-64 blocks in each direction from you or the crafting table. Multiplayer uses the server's range.",
            this.radius, v -> this.radius = v);
        textSetting("Nearby items", "Refresh interval (seconds)", "0.25-30 seconds between nearby-item updates. Lower values update faster.",
            this.refresh, v -> this.refresh = v);
        textSetting("Nearby items", "Panel opacity (%)", "5-100%. Higher values make the nearby-items background less transparent.",
            this.panelOpacity, v -> this.panelOpacity = v);
        toggleSetting("Nearby items", "Open panel automatically", "Open the nearby-items panel when entering your inventory or crafting table.",
            this.draft.nearbyPanelOpenByDefault, v -> this.draft.nearbyPanelOpenByDefault = v);
        toggleSetting("Container highlights", "Highlight containers", "Outline matching containers in the world when you locate an item from the nearby panel.",
            this.draft.showHighlighter, v -> this.draft.showHighlighter = v);
        actionSetting("Container highlights", "Highlight color", "Choose the color of matching-container highlights. Opens a color picker.",
            this.color + " / Pick", button -> this.minecraft.setScreenAndShow(new EasyColorPickerScreen(
                this, parsedColorOrDefault(), chosen -> this.color = String.format(Locale.ROOT, "#%06X", chosen))));
        textSetting("Container highlights", "Visible for (seconds)", "0.5-60 seconds. How long a located container stays highlighted.",
            this.duration, v -> this.duration = v);
        textSetting("Container highlights", "Highlight opacity (%)", "5-100%. Higher values make the colored highlight fill more visible.",
            this.highlightOpacity, v -> this.highlightOpacity = v);
        toggleSetting("Locate effects", "Show distance labels", "Display the distance in blocks above highlighted containers.",
            this.draft.showDistanceLabel, v -> this.draft.showDistanceLabel = v);
        toggleSetting("Locate effects", "Turn camera to container", "Automatically turn your camera toward the nearest matching container when locating an item.",
            this.draft.snapAimToChest, v -> this.draft.snapAimToChest = v);
        toggleSetting("Locate effects", "Show guiding particles", "Draw particles between you and the located container.",
            this.draft.resolveLocateTrail(), v -> { this.draft.showLocateTrail = v; this.draft.showSmokeTrail = null; });
        LocateTrailParticle particle = this.draft.locateTrailParticle;
        actionSetting("Locate effects", "Guiding particle type", "Click to cycle particle styles. Visible only when guiding particles are enabled.",
            particle.getLabel(), button -> {
                this.draft.locateTrailParticle = this.draft.locateTrailParticle.next();
                button.setMessage(Component.literal(this.draft.locateTrailParticle.getLabel()));
            });
    }

    @Override
    protected void resetDraft() { setDraft(ConfigData.defaults()); }

    @Override
    protected void saveDraft() {
        ConfigData updated = this.draft.copy();
        updated.highlightColor = parseColor(this.color);
        updated.highlightDurationTicks = (int) Math.round(decimal(this.duration, 0.5, 60, "Highlight duration (seconds)") * 20);
        updated.nearbyRadius = integer(this.radius, 1, 64, "Search range (blocks)");
        updated.highlightOpacityPercent = integer(this.highlightOpacity, 5, 100, "Highlight opacity (%)");
        updated.nearbyPanelOpacityPercent = integer(this.panelOpacity, 5, 100, "Panel opacity (%)");
        updated.autoRefreshTicks = (int) Math.round(decimal(this.refresh, 0.25, 30, "Refresh interval (seconds)") * 20);
        updated.showSmokeTrail = null;
        EasyInventoryCrafterConfig.update(updated);
        NearbyItemsClientState.requestUpdate();
    }

    private int parsedColorOrDefault() {
        try { return parseColor(this.color); }
        catch (IllegalArgumentException ignored) { return this.draft.highlightColor; }
    }

    private static int parseColor(String raw) {
        String value = raw.trim().replaceFirst("^#", "");
        if (!value.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Highlight color: use six hex digits, such as #55CCFF.");
        return Integer.parseInt(value, 16);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.2f", ticks / 20.0).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
