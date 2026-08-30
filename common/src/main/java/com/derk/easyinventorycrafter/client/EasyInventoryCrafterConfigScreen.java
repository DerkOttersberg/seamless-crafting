package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.ConfigData;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.LocateTrailParticle;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EasyInventoryCrafterConfigScreen extends Screen {
    private final Screen parent;
    private EditBox highlightColorField;
    private EditBox highlightDurationField;
    private EditBox nearbyRadiusField;
    private EditBox highlightOpacityField;
    private EditBox nearbyPanelOpacityField;
    private EditBox autoRefreshField;
    private Button highlightColorPickerButton;
    private boolean showHighlighter;
    private boolean showDistanceLabel;
    private boolean snapAimToChest;
    private boolean showLocateTrail;
    private LocateTrailParticle locateTrailParticle;
    private boolean nearbyPanelOpenByDefault;
    private Button highlightEnabledButton;
    private Button showDistanceLabelButton;
    private Button snapAimButton;
    private Button locateTrailButton;
    private Button trailParticleButton;
    private Button panelDefaultButton;
    private Component errorText = Component.empty();

    public EasyInventoryCrafterConfigScreen(Screen parent) {
        super(Component.literal("Seamless Crafting Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigData config = EasyInventoryCrafterConfig.snapshot();
        this.showHighlighter = config.showHighlighter;
        this.showDistanceLabel = config.showDistanceLabel;
        this.snapAimToChest = config.snapAimToChest;
        this.showLocateTrail = config.resolveLocateTrail();
        this.locateTrailParticle = config.locateTrailParticle == null ? LocateTrailParticle.WATER_EVAPORATION : config.locateTrailParticle;
        this.nearbyPanelOpenByDefault = config.nearbyPanelOpenByDefault;

        int centerX = this.width / 2;
        int labelX = centerX - 168;
        int labelW = 126;
        int fieldX = labelX + labelW + 10;
        int fieldW = 108;
        int startY = 62;
        int row = 34;

        this.derk$addLabel(labelX, startY, labelW, "Highlight Color");
        this.derk$addLabel(labelX, startY + row, labelW, "Highlight Duration");
        this.derk$addLabel(labelX, startY + row * 2, labelW, "Nearby Distance");
        this.derk$addLabel(labelX, startY + row * 3, labelW, "Highlight Opacity");
        this.derk$addLabel(labelX, startY + row * 4, labelW, "Nearby Panel Opacity");
        this.derk$addLabel(labelX, startY + row * 5, labelW, "Auto Refresh");
        this.derk$addLabel(labelX, startY + row * 6, labelW, "Chest Highlighter");
        this.derk$addLabel(labelX, startY + row * 7, labelW, "Distance Label");
        this.derk$addLabel(labelX, startY + row * 8, labelW, "Snap Aim To Chest");
        this.derk$addLabel(labelX, startY + row * 9, labelW, "Locate Trail");
        this.derk$addLabel(labelX, startY + row * 10, labelW, "Trail Particle");
        this.derk$addLabel(labelX, startY + row * 11, labelW, "Panel Default");

        this.highlightColorField = this.derk$addField(fieldX, startY, fieldW, String.format(Locale.ROOT, "#%06X", config.highlightColor));
        this.highlightColorPickerButton = this.addRenderableWidget(Button.builder(Component.literal("Pick"), btn ->
            this.minecraft.setScreenAndShow(new EasyColorPickerScreen(
                this,
                this.derk$parseHexColorSafe(this.highlightColorField.getValue()),
                col -> this.highlightColorField.setValue(String.format(Locale.ROOT, "#%06X", col))
            ))).bounds(fieldX + fieldW + 8, startY, 56, 20).build());

        this.highlightDurationField = this.derk$addField(fieldX, startY + row, fieldW, this.derk$formatSeconds(config.highlightDurationTicks));
        this.nearbyRadiusField = this.derk$addField(fieldX, startY + row * 2, fieldW, Integer.toString(config.nearbyRadius));
        this.highlightOpacityField = this.derk$addField(fieldX, startY + row * 3, fieldW, Integer.toString(config.highlightOpacityPercent));
        this.nearbyPanelOpacityField = this.derk$addField(fieldX, startY + row * 4, fieldW, Integer.toString(config.nearbyPanelOpacityPercent));
        this.autoRefreshField = this.derk$addField(fieldX, startY + row * 5, fieldW, this.derk$formatSeconds(config.autoRefreshTicks));

        this.highlightEnabledButton = this.addRenderableWidget(Button.builder(this.derk$onOff(this.showHighlighter), btn -> {
            this.showHighlighter = !this.showHighlighter;
            btn.setMessage(this.derk$onOff(this.showHighlighter));
        }).bounds(fieldX, startY + row * 6, 96, 20).build());

        this.showDistanceLabelButton = this.addRenderableWidget(Button.builder(this.derk$onOff(this.showDistanceLabel), btn -> {
            this.showDistanceLabel = !this.showDistanceLabel;
            btn.setMessage(this.derk$onOff(this.showDistanceLabel));
        }).bounds(fieldX, startY + row * 7, 96, 20).build());

        this.snapAimButton = this.addRenderableWidget(Button.builder(this.derk$onOff(this.snapAimToChest), btn -> {
            this.snapAimToChest = !this.snapAimToChest;
            btn.setMessage(this.derk$onOff(this.snapAimToChest));
        }).bounds(fieldX, startY + row * 8, 96, 20).build());

        this.locateTrailButton = this.addRenderableWidget(Button.builder(this.derk$onOff(this.showLocateTrail), btn -> {
            this.showLocateTrail = !this.showLocateTrail;
            btn.setMessage(this.derk$onOff(this.showLocateTrail));
        }).bounds(fieldX, startY + row * 9, 96, 20).build());

        this.trailParticleButton = this.addRenderableWidget(Button.builder(Component.literal(this.locateTrailParticle.getLabel()), btn -> {
            this.locateTrailParticle = this.locateTrailParticle.next();
            btn.setMessage(Component.literal(this.locateTrailParticle.getLabel()));
        }).bounds(fieldX, startY + row * 10, 132, 20).build());

        this.panelDefaultButton = this.addRenderableWidget(Button.builder(this.derk$openClosed(this.nearbyPanelOpenByDefault), btn -> {
            this.nearbyPanelOpenByDefault = !this.nearbyPanelOpenByDefault;
            btn.setMessage(this.derk$openClosed(this.nearbyPanelOpenByDefault));
        }).bounds(fieldX, startY + row * 11, 96, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), btn -> this.derk$resetToDefaults()).bounds(centerX - 155, this.height - 52, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> this.onClose()).bounds(centerX - 50, this.height - 52, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> this.derk$save()).bounds(centerX + 55, this.height - 52, 100, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xCC101014);
        int cx = this.width / 2;
        int panelX = cx - 190;
        int panelY = 34;
        g.fill(panelX, panelY, panelX + 380, panelY + 464, 0xAA191C22);
        derk$drawBorder(g, panelX, panelY, 380, 464, 0xFF404652);
        g.centeredText(this.font, this.title, cx, 18, 0xFFFFFF);
        if (!this.errorText.getString().isEmpty()) {
            g.centeredText(this.font, this.errorText, cx, this.height - 78, 0xFF6B6B);
        }

        int startY = 62;
        int row = 34;
        int labelX = cx - 168;
        g.text(this.font, Component.literal("Pick a color visually or type a hex code."), labelX, startY + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("How long chest highlights stay visible."), labelX, startY + row + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Search radius around the crafting table."), labelX, startY + row * 2 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Opacity of the filled highlight overlay."), labelX, startY + row * 3 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Opacity of the nearby items panel."), labelX, startY + row * 4 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("How often the nearby list refreshes."), labelX, startY + row * 5 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Turn world chest highlighting on or off."), labelX, startY + row * 6 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Show floating distance text above highlights."), labelX, startY + row * 7 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Rotate camera toward nearest matching chest."), labelX, startY + row * 8 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Show a trail leading to the located chest."), labelX, startY + row * 9 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Particle type for the locate-trail effect."), labelX, startY + row * 10 + 24, 0xB9C0CB, true);
        g.text(this.font, Component.literal("Whether the nearby panel starts opened."), labelX, startY + row * 11 + 24, 0xB9C0CB, true);

        try {
            int previewColor = this.derk$parseHexColor(this.highlightColorField.getValue());
            int fieldX = labelX + 126 + 10;
            g.fill(fieldX + 176, startY + 2, fieldX + 196, startY + 18, 0xFF000000 | previewColor);
            derk$drawBorder(g, fieldX + 176, startY + 2, 20, 16, 0xFFFFFFFF);
        } catch (IllegalArgumentException ignored) {
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void derk$addLabel(int x, int y, int w, String label) {
        this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {}).bounds(x, y, w, 20).build());
    }

    private EditBox derk$addField(int x, int y, int w, String value) {
        EditBox field = new EditBox(this.font, x, y, w, 20, Component.empty());
        field.setValue(value);
        this.addRenderableWidget(field);
        return field;
    }

    private void derk$resetToDefaults() {
        ConfigData d = ConfigData.defaults();
        this.highlightColorField.setValue(String.format(Locale.ROOT, "#%06X", d.highlightColor));
        this.highlightDurationField.setValue(this.derk$formatSeconds(d.highlightDurationTicks));
        this.nearbyRadiusField.setValue(Integer.toString(d.nearbyRadius));
        this.highlightOpacityField.setValue(Integer.toString(d.highlightOpacityPercent));
        this.nearbyPanelOpacityField.setValue(Integer.toString(d.nearbyPanelOpacityPercent));
        this.autoRefreshField.setValue(this.derk$formatSeconds(d.autoRefreshTicks));
        this.showHighlighter = d.showHighlighter;
        this.showDistanceLabel = d.showDistanceLabel;
        this.snapAimToChest = d.snapAimToChest;
        this.showLocateTrail = d.resolveLocateTrail();
        this.locateTrailParticle = d.locateTrailParticle == null ? LocateTrailParticle.WATER_EVAPORATION : d.locateTrailParticle;
        this.nearbyPanelOpenByDefault = d.nearbyPanelOpenByDefault;
        this.highlightEnabledButton.setMessage(this.derk$onOff(this.showHighlighter));
        this.showDistanceLabelButton.setMessage(this.derk$onOff(this.showDistanceLabel));
        this.snapAimButton.setMessage(this.derk$onOff(this.snapAimToChest));
        this.locateTrailButton.setMessage(this.derk$onOff(this.showLocateTrail));
        this.trailParticleButton.setMessage(Component.literal(this.locateTrailParticle.getLabel()));
        this.panelDefaultButton.setMessage(this.derk$openClosed(this.nearbyPanelOpenByDefault));
        this.errorText = Component.empty();
    }

    private void derk$save() {
        try {
            ConfigData u = EasyInventoryCrafterConfig.snapshot();
            u.highlightColor = this.derk$parseHexColor(this.highlightColorField.getValue());
            u.highlightDurationTicks = this.derk$parseSecondsToTicks(this.highlightDurationField.getValue(), 0.5, 60.0);
            u.nearbyRadius = this.derk$parseInt(this.nearbyRadiusField.getValue(), 1, 64, "Nearby radius");
            u.highlightOpacityPercent = this.derk$parseInt(this.highlightOpacityField.getValue(), 5, 100, "Highlight opacity");
            u.nearbyPanelOpacityPercent = this.derk$parseInt(this.nearbyPanelOpacityField.getValue(), 5, 100, "Nearby panel opacity");
            u.autoRefreshTicks = this.derk$parseSecondsToTicks(this.autoRefreshField.getValue(), 0.25, 30.0);
            u.showHighlighter = this.showHighlighter;
            u.showDistanceLabel = this.showDistanceLabel;
            u.snapAimToChest = this.snapAimToChest;
            u.showLocateTrail = this.showLocateTrail;
            u.showSmokeTrail = null;
            u.locateTrailParticle = this.locateTrailParticle;
            u.nearbyPanelOpenByDefault = this.nearbyPanelOpenByDefault;
            EasyInventoryCrafterConfig.update(u);
            NearbyItemsClientState.requestUpdate();
            this.onClose();
        } catch (IllegalArgumentException e) {
            this.errorText = Component.literal(e.getMessage());
        }
    }

    private int derk$parseHexColor(String raw) {
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Highlight Color: must be a 6-digit hex code.");
        return Integer.parseInt(s, 16);
    }

    private int derk$parseHexColorSafe(String raw) {
        try {
            return derk$parseHexColor(raw);
        } catch (IllegalArgumentException e) {
            return EasyInventoryCrafterConfig.getHighlightColor();
        }
    }

    private int derk$parseSecondsToTicks(String raw, double minSec, double maxSec) {
        try {
            double seconds = Double.parseDouble(raw.trim());
            if (seconds < minSec || seconds > maxSec) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Value must be between %.2f and %.2f.", minSec, maxSec));
            }
            return Math.max(1, (int) Math.round(seconds * 20.0));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + raw.trim());
        }
    }

    private int derk$parseInt(String raw, int min, int max, String fieldName) {
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min || v > max) throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max + ".");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + ": not a valid integer.");
        }
    }

    private String derk$formatSeconds(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds == (long) seconds) return Long.toString((long) seconds);
        return String.format(Locale.ROOT, "%.2f", seconds);
    }

    private Component derk$onOff(boolean value) {
        return Component.literal(value ? "On" : "Off");
    }

    private Component derk$openClosed(boolean value) {
        return Component.literal(value ? "Open" : "Closed");
    }

    private static void derk$drawBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
