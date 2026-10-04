package com.derk.easyinventorycrafter.client;

import io.github.derkottersberg.seamlesscrafting.internal.client.SettingsScreen;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EasyColorPickerScreen extends SettingsScreen {
    private final IntConsumer onSave;
    private String hex;
    private int color;

    public EasyColorPickerScreen(Screen parent, int initialColor, IntConsumer onSave) {
        super(parent, "Container Highlight Color", "Choose a color. Save returns to the unsaved Crafting settings.");
        this.onSave = onSave;
        setColor(initialColor);
    }

    private void setColor(int value) {
        this.color = value & 0xFFFFFF;
        this.hex = String.format(Locale.ROOT, "#%06X", this.color);
    }

    @Override
    protected void buildSettings() {
        textSetting("Color", "Hex color (#RRGGBB)", "Six hex digits, for example #55CCFF. Changes are previewed below.",
            this.hex, value -> {
                this.hex = value;
                try { this.color = parseHex(value); } catch (IllegalArgumentException ignored) { }
            });
        widgetSetting("RGB channels", "Red (0-255)", "Drag the slider or use arrow keys. Changing a channel updates the hex color.",
            new ChannelSlider("Red", (this.color >> 16) & 255, value -> setColor((this.color & 0x00FFFF) | (value << 16))));
        widgetSetting("RGB channels", "Green (0-255)", "Drag to adjust the amount of green in the highlight color.",
            new ChannelSlider("Green", (this.color >> 8) & 255, value -> setColor((this.color & 0xFF00FF) | (value << 8))));
        widgetSetting("RGB channels", "Blue (0-255)", "Drag to adjust the amount of blue in the highlight color.",
            new ChannelSlider("Blue", this.color & 255, value -> setColor((this.color & 0xFFFF00) | value)));
    }

    @Override
    protected void resetDraft() { setColor(0xFFD700); }

    @Override
    protected void saveDraft() { this.onSave.accept(parseHex(this.hex)); }

    @Override
    protected String summary() { return "Preview: " + this.hex + ". Save the main settings afterward to apply."; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - Math.min(520, this.width - 24)) / 2 + 30;
        graphics.fill(x, 46, x + 16, 62, 0xFF000000 | this.color);
        graphics.outline(x, 46, 16, 16, 0xFFFFFFFF);
    }

    private static int parseHex(String raw) {
        String value = raw.trim().replaceFirst("^#", "");
        if (!value.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Color: enter six hex digits, such as #55CCFF.");
        return Integer.parseInt(value, 16);
    }

    private static final class ChannelSlider extends AbstractSliderButton {
        private final String channel;
        private final IntConsumer changed;

        private ChannelSlider(String channel, int value, IntConsumer changed) {
            super(0, 0, 100, 20, Component.empty(), value / 255.0);
            this.channel = channel;
            this.changed = changed;
            updateMessage();
        }

        @Override
        protected void updateMessage() { setMessage(Component.literal(this.channel + ": " + Math.round(this.value * 255))); }

        @Override
        protected void applyValue() { this.changed.accept((int) Math.round(this.value * 255)); }
    }
}
