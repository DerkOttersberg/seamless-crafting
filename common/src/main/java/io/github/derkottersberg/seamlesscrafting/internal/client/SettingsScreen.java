package io.github.derkottersberg.seamlesscrafting.internal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Small, responsive settings pages. Draft values live in the subclass, so page changes,
 * window resizing and a child color picker cannot silently discard uncommitted edits.
 * Kept inside each mod to avoid adding a mandatory client UI dependency.
 */
public abstract class SettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 42;
    private final Screen parent;
    private final String scope;
    private final List<Row> rows = new ArrayList<>();
    private final List<List<Row>> pages = new ArrayList<>();
    private int page;
    private int left;
    private int contentWidth;
    private String error = "";

    protected SettingsScreen(Screen parent, String title, String scope) {
        super(Component.literal(title));
        this.parent = parent;
        this.scope = scope;
    }

    protected abstract void buildSettings();
    protected abstract void resetDraft();
    protected abstract void saveDraft();

    protected String summary() { return "Changes are applied only when you select Save."; }
    protected boolean validDraft() { return true; }

    @Override
    protected final void init() {
        this.contentWidth = Math.min(520, this.width - 24);
        this.left = (this.width - this.contentWidth) / 2;
        this.rows.clear();
        this.pages.clear();
        buildSettings();
        int capacity = Math.max(1, (this.height - 136) / ROW_HEIGHT);
        List<Row> current = null;
        for (Row row : this.rows) {
            if (current == null || current.size() == capacity
                || !current.getFirst().section.equals(row.section)) {
                current = new ArrayList<>();
                this.pages.add(current);
            }
            current.add(row);
        }
        this.page = Math.min(this.page, Math.max(0, this.pages.size() - 1));
        if (!this.pages.isEmpty()) {
            List<Row> visible = this.pages.get(this.page);
            int controlWidth = Math.min(146, this.contentWidth / 2);
            for (int i = 0; i < visible.size(); i++) {
                Row row = visible.get(i);
                row.widget.setRectangle(controlWidth, 20,
                    this.left + this.contentWidth - controlWidth - 8, 74 + i * ROW_HEIGHT);
                row.widget.setTooltip(Tooltip.create(Component.literal(row.label + "\n" + row.help)));
                this.addRenderableWidget(row.widget);
            }
        }
        Button previous = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
            .bounds(this.left, 44, 24, 20).build());
        previous.active = this.page > 0;
        previous.setTooltip(Tooltip.create(Component.literal("Previous settings page")));
        Button next = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
            .bounds(this.left + this.contentWidth - 24, 44, 24, 20).build());
        next.active = this.page + 1 < this.pages.size();
        next.setTooltip(Tooltip.create(Component.literal("Next settings page")));

        int buttonWidth = Math.min(120, (this.contentWidth - 16) / 3);
        int start = (this.width - (buttonWidth * 3 + 16)) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Reset defaults"), b -> {
            resetDraft();
            this.error = "";
            rebuildWidgets();
        }).bounds(start, this.height - 28, buttonWidth, 20)
            .tooltip(Tooltip.create(Component.literal("Reset every page. Select Save to apply, or Cancel to discard."))).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(start + buttonWidth + 8, this.height - 28, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            try {
                saveDraft();
                onClose();
            } catch (IllegalArgumentException exception) {
                this.error = exception.getMessage();
            }
        }).bounds(start + buttonWidth * 2 + 16, this.height - 28, buttonWidth, 20).build());
    }

    private void changePage(int direction) {
        this.page += direction;
        this.error = "";
        rebuildWidgets();
    }

    protected final void textSetting(String section, String label, String help, String value, Consumer<String> changed) {
        EditBox field = new EditBox(this.font, 0, 0, 100, 20, Component.literal(label));
        field.setMaxLength(32);
        field.setValue(value);
        field.setResponder(changed);
        this.rows.add(new Row(section, label, help, field));
    }

    protected final void toggleSetting(String section, String label, String help, boolean value, Consumer<Boolean> changed) {
        final boolean[] draft = {value};
        actionSetting(section, label, help, value ? "Enabled" : "Disabled", button -> {
            draft[0] = !draft[0];
            changed.accept(draft[0]);
            button.setMessage(Component.literal(draft[0] ? "Enabled" : "Disabled"));
        });
    }

    protected final void actionSetting(String section, String label, String help, String value, Consumer<Button> pressed) {
        this.rows.add(new Row(section, label, help,
            Button.builder(Component.literal(value), pressed::accept).bounds(0, 0, 100, 20).build()));
    }

    protected final void widgetSetting(String section, String label, String help, AbstractWidget widget) {
        this.rows.add(new Row(section, label, help, widget));
    }

    @Override
    public final void onClose() { this.minecraft.setScreenAndShow(this.parent); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xF010141C);
        drawFitted(graphics, this.title.getString(), this.width / 2, 12, this.width - 20, 0xFFFFFFFF, true);
        drawFitted(graphics, this.scope, this.width / 2, 28, this.width - 20, 0xFFB3C1D3, true);
        if (!this.pages.isEmpty()) {
            List<Row> visible = this.pages.get(this.page);
            String heading = visible.getFirst().section + "  (" + (this.page + 1) + "/" + this.pages.size() + ")";
            drawFitted(graphics, heading, this.width / 2, 50, this.contentWidth - 60, 0xFF8FD7CB, true);
            for (int i = 0; i < visible.size(); i++) {
                Row row = visible.get(i);
                int y = 74 + i * ROW_HEIGHT;
                graphics.fill(this.left, y - 3, this.left + this.contentWidth, y + 35, 0xFF1C2430);
                drawFitted(graphics, row.label, this.left + 8, y + 5,
                    row.widget.getX() - this.left - 16, 0xFFFFFFFF, false);
                drawFitted(graphics, row.help, this.left + 8, y + 24, this.contentWidth - 16, 0xFFAFBDCF, false);
                if (mouseX >= this.left && mouseX < this.left + this.contentWidth
                    && mouseY >= y && mouseY < y + 34 && !row.widget.isMouseOver(mouseX, mouseY)) {
                    graphics.setTooltipForNextFrame(this.font,
                        Component.literal(row.label + "\n" + row.help), mouseX, mouseY);
                }
            }
        }
        String footer = this.error.isEmpty() ? summary() : this.error;
        int color = !this.error.isEmpty() || !validDraft() ? 0xFFFF9292 : 0xFFB3C1D3;
        var lines = this.font.split(Component.literal(footer), this.contentWidth);
        int y = this.height - 52;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.centeredText(this.font, lines.get(i), this.width / 2, y + i * 10, color);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFitted(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int color, boolean centered) {
        String visible = this.font.width(text) <= maxWidth ? text
            : this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
        if (centered) graphics.centeredText(this.font, visible, x, y, color);
        else graphics.text(this.font, visible, x, y, color, false);
    }

    protected static int integer(String raw, int min, int max, String label) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + ": enter a whole number from " + min + " to " + max + ".");
    }

    protected static double decimal(String raw, double min, double max, String label) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (Double.isFinite(value) && value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + ": enter a number from " + min + " to " + max + ".");
    }

    private record Row(String section, String label, String help, AbstractWidget widget) { }
}
