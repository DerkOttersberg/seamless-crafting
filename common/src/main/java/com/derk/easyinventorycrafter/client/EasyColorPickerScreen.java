package com.derk.easyinventorycrafter.client;

import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EasyColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer onSave;
    private int color;
    private EditBox hexField;
    private RgbSliderButton redSlider;
    private RgbSliderButton greenSlider;
    private RgbSliderButton blueSlider;
    private Component errorText = Component.empty();
    private boolean syncingControls;

    public EasyColorPickerScreen(Screen parent, int initialColor, IntConsumer onSave) {
        super(Component.literal("Highlight Color"));
        this.parent = parent;
        this.onSave = onSave;
        this.color = initialColor & 0xFFFFFF;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelX = centerX - 140;
        int fieldX = panelX + 140;
        int rowY = 68;

        this.hexField = new EditBox(this.font, fieldX, rowY - 2, 120, 20, Component.empty());
        this.hexField.setValue(this.derk$formatHex(this.color));
        this.addRenderableWidget(this.hexField);

        this.redSlider = this.addRenderableWidget(new RgbSliderButton(panelX + 92, rowY + 34, 168, 20, "Red", (this.color >> 16) & 0xFF, value -> this.derk$updateColorFromSliders()));
        this.greenSlider = this.addRenderableWidget(new RgbSliderButton(panelX + 92, rowY + 64, 168, 20, "Green", (this.color >> 8) & 0xFF, value -> this.derk$updateColorFromSliders()));
        this.blueSlider = this.addRenderableWidget(new RgbSliderButton(panelX + 92, rowY + 94, 168, 20, "Blue", this.color & 0xFF, value -> this.derk$updateColorFromSliders()));

        this.addRenderableWidget(Button.builder(Component.literal("Apply Hex"), button -> this.derk$applyHex()).bounds(fieldX + 126, rowY - 2, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose()).bounds(centerX - 104, this.height - 52, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            this.onSave.accept(this.color);
            this.onClose();
        }).bounds(centerX + 4, this.height - 52, 100, 20).build());

        this.derk$syncControlsFromColor();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xD0101014);
        int centerX = this.width / 2;
        int panelX = centerX - 140;
        int panelY = 36;
        int panelWidth = 280;
        int panelHeight = 170;
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA1A1D24);
        derk$drawBorder(g, panelX, panelY, panelWidth, panelHeight, 0xFF444A56);
        g.centeredText(this.font, this.title, centerX, panelY + 14, 0xFFFFFF);
        g.text(this.font, Component.literal("Hex Code"), panelX + 18, panelY + 34, 0xE6E6E6, true);
        g.text(this.font, Component.literal("Preview"), panelX + 18, panelY + 68, 0xE6E6E6, true);
        g.fill(panelX + 18, panelY + 82, panelX + 70, panelY + 134, 0xFF000000 | this.color);
        derk$drawBorder(g, panelX + 18, panelY + 82, 52, 52, 0xFFFFFFFF);
        g.text(this.font, Component.literal(this.derk$formatHex(this.color)), panelX + 82, panelY + 86, 0xFFFFFF, true);
        g.text(this.font, Component.literal("Use the sliders or type a hex value."), panelX + 82, panelY + 102, 0xB7BDC9, true);
        if (!this.errorText.getString().isEmpty()) {
            g.centeredText(this.font, this.errorText, centerX, this.height - 78, 0xFF6B6B);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void derk$applyHex() {
        try {
            this.color = this.derk$parseHexColor(this.hexField.getValue());
            this.derk$syncControlsFromColor();
            this.errorText = Component.empty();
        } catch (IllegalArgumentException e) {
            this.errorText = Component.literal(e.getMessage());
        }
    }

    private void derk$updateColorFromSliders() {
        if (this.syncingControls) return;
        this.color = (this.redSlider.getChannelValue() << 16) | (this.greenSlider.getChannelValue() << 8) | this.blueSlider.getChannelValue();
        this.hexField.setValue(this.derk$formatHex(this.color));
        this.errorText = Component.empty();
    }

    private void derk$syncControlsFromColor() {
        this.syncingControls = true;
        this.hexField.setValue(this.derk$formatHex(this.color));
        this.redSlider.setChannelValue((this.color >> 16) & 0xFF);
        this.greenSlider.setChannelValue((this.color >> 8) & 0xFF);
        this.blueSlider.setChannelValue(this.color & 0xFF);
        this.syncingControls = false;
    }

    private int derk$parseHexColor(String raw) {
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Color must be 6 hex digits.");
        return Integer.parseInt(s, 16);
    }

    private String derk$formatHex(int c) {
        return String.format(Locale.ROOT, "#%06X", c & 0xFFFFFF);
    }

    private static void derk$drawBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    static final class RgbSliderButton extends AbstractSliderButton {
        private final String label;
        private final java.util.function.IntConsumer onChange;

        RgbSliderButton(int x, int y, int w, int h, String label, int initialValue, java.util.function.IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), initialValue / 255.0);
            this.label = label;
            this.onChange = onChange;
            this.updateMessage();
        }

        int getChannelValue() {
            return (int) Math.round(this.value * 255.0);
        }

        void setChannelValue(int value) {
            this.value = Math.max(0.0, Math.min(1.0, value / 255.0));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.label + ": " + this.getChannelValue()));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.getChannelValue());
        }
    }
}
