package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Shared panel state, filtering, rendering, hit-testing, and scrolling. */
public final class NearbyPanelController {
    private NearbyPanelLayout layout = new NearbyPanelLayout(4, 48, 0, false, 60);
    private EditBox searchField;
    private int scrollOffset;
    private boolean requestedOpen;
    private int lastClickIndex = -1;
    private long lastClickTick = -1000L;

    public void initialize(boolean openByDefault, EditBox searchField) {
        requestedOpen = openByDefault;
        this.searchField = searchField;
        scrollOffset = 0;
        lastClickIndex = -1;
        lastClickTick = -1000L;
    }

    public NearbyPanelLayout updateLayout(
        int screenWidth,
        int screenHeight,
        int leftPos,
        int topPos,
        int imageWidth,
        boolean recipeBookOpen,
        int controlsWidth
    ) {
        layout = NearbyPanelLayout.calculate(
            screenWidth,
            screenHeight,
            leftPos,
            topPos,
            imageWidth,
            requestedOpen,
            recipeBookOpen,
            controlsWidth
        );
        clampScroll(filteredEntries().size());
        return layout;
    }

    public boolean toggleOpen() {
        requestedOpen = !requestedOpen;
        if (requestedOpen) {
            NearbyItemsClientState.requestUpdate();
        }
        return requestedOpen;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (!layout.expanded()) {
            return;
        }
        int panelX = layout.panelX();
        int panelY = layout.panelY();
        graphics.fill(
            panelX,
            panelY,
            panelX + NearbyPanelLayout.PANEL_WIDTH,
            panelY + layout.panelHeight(),
            withOpacity(0x88000000)
        );
        Component header = NearbyItemsClientState.isTruncated()
            ? Component.literal("Nearby (partial)")
            : Component.literal("Nearby");
        graphics.text(font, header, panelX + 4, panelY + 4, NearbyItemsClientState.isTruncated() ? 0xFFD166 : 0xFFFFFF, true);

        List<NearbyItemEntry> entries = filteredEntries();
        if (entries.isEmpty()) {
            Component state;
            if (NearbyItemsClientState.isLoading() || !NearbyItemsClientState.hasReceivedPayload()) {
                state = Component.literal("Loading...");
            } else if (NearbyItemsClientState.getEntries().isEmpty()) {
                state = Component.literal("No nearby items");
            } else {
                state = Component.literal("No matches");
            }
            graphics.text(font, state, panelX + 5, panelY + 20, 0xB8B8B8, false);
            return;
        }

        int startX = panelX + 3;
        int startY = panelY + 14;
        int maxItems = NearbyPanelLayout.COLUMNS * layout.rows();
        for (int row = 0; row < layout.rows(); row++) {
            for (int col = 0; col < NearbyPanelLayout.COLUMNS; col++) {
                int slotX = startX + col * NearbyPanelLayout.SLOT_SIZE;
                int slotY = startY + row * NearbyPanelLayout.SLOT_SIZE;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, withOpacity(0x55000000));
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, withOpacity(0x2A000000));
                graphics.fill(slotX, slotY, slotX + 18, slotY + 1, withOpacity(0x66FFFFFF));
                graphics.fill(slotX, slotY, slotX + 1, slotY + 18, withOpacity(0x66FFFFFF));
                graphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, withOpacity(0x33000000));
                graphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, withOpacity(0x33000000));
            }
        }

        int startIndex = scrollOffset * NearbyPanelLayout.COLUMNS;
        int endIndex = Math.min(entries.size(), startIndex + maxItems);
        for (int index = startIndex; index < endIndex; index++) {
            int local = index - startIndex;
            int itemX = startX + local % NearbyPanelLayout.COLUMNS * NearbyPanelLayout.SLOT_SIZE + 2;
            int itemY = startY + local / NearbyPanelLayout.COLUMNS * NearbyPanelLayout.SLOT_SIZE + 1;
            NearbyItemEntry entry = entries.get(index);
            graphics.item(entry.stack(), itemX, itemY);
            graphics.itemDecorations(font, entry.stack(), itemX, itemY, formatCount(entry.count()));
        }

        int hovered = hoveredIndex(mouseX, mouseY, entries.size());
        if (hovered >= 0) {
            graphics.setTooltipForNextFrame(font, entries.get(hovered).stack(), mouseX, mouseY);
        }
        renderClickPulse(graphics, entries.size());
    }

    public boolean handleMouseClick(MouseButtonEvent click) {
        if (!layout.expanded() || click.button() != 0 || !insidePanel(click.x(), click.y())) {
            return false;
        }
        List<NearbyItemEntry> entries = filteredEntries();
        int index = hoveredIndex(click.x(), click.y(), entries.size());
        if (index < 0) {
            return false;
        }
        NearbyItemsClientState.requestHighlightAndAim(entries.get(index).stack());
        lastClickIndex = index;
        lastClickTick = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    public boolean handleScroll(double mouseX, double mouseY, double verticalAmount) {
        if (!layout.expanded() || !insidePanel(mouseX, mouseY)) {
            return false;
        }
        scrollOffset += verticalAmount > 0 ? -1 : verticalAmount < 0 ? 1 : 0;
        clampScroll(filteredEntries().size());
        return true;
    }

    public boolean handleCharTyped(CharacterEvent input) {
        if (layout.expanded() && searchField != null && searchField.charTyped(input)) {
            scrollOffset = 0;
            return true;
        }
        return false;
    }

    public boolean handleKeyPressed(KeyEvent input) {
        if (layout.expanded() && searchField != null && searchField.keyPressed(input)) {
            scrollOffset = 0;
            return true;
        }
        return false;
    }

    public List<PanelBounds> visibleBounds() {
        return layout.visibleBounds();
    }

    public Optional<HoveredNearbyStack> hoveredStack(double mouseX, double mouseY) {
        if (!layout.expanded()) {
            return Optional.empty();
        }
        List<NearbyItemEntry> entries = filteredEntries();
        int index = hoveredIndex(mouseX, mouseY, entries.size());
        if (index < 0) {
            return Optional.empty();
        }
        int local = index - scrollOffset * NearbyPanelLayout.COLUMNS;
        int x = layout.panelX() + 3 + local % NearbyPanelLayout.COLUMNS * NearbyPanelLayout.SLOT_SIZE;
        int y = layout.panelY() + 14 + local / NearbyPanelLayout.COLUMNS * NearbyPanelLayout.SLOT_SIZE;
        return Optional.of(new HoveredNearbyStack(entries.get(index).stack(), new PanelBounds(x, y, 18, 18)));
    }

    private List<NearbyItemEntry> filteredEntries() {
        String query = searchField == null ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<NearbyItemEntry> filtered = new ArrayList<>();
        for (NearbyItemEntry entry : NearbyItemsClientState.getEntries()) {
            if (query.isEmpty() || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(entry);
            }
        }
        filtered.sort(Comparator.comparingInt((NearbyItemEntry entry) -> categoryRank(entry.stack()))
            .thenComparing(entry -> entry.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
        return filtered;
    }

    private int hoveredIndex(double mouseX, double mouseY, int totalEntries) {
        int startX = layout.panelX() + 3;
        int startY = layout.panelY() + 14;
        int relativeX = (int) mouseX - startX;
        int relativeY = (int) mouseY - startY;
        if (relativeX < 0 || relativeY < 0) {
            return -1;
        }
        int col = relativeX / NearbyPanelLayout.SLOT_SIZE;
        int row = relativeY / NearbyPanelLayout.SLOT_SIZE;
        if (col < 0 || col >= NearbyPanelLayout.COLUMNS || row < 0 || row >= layout.rows()) {
            return -1;
        }
        int slotX = startX + col * NearbyPanelLayout.SLOT_SIZE;
        int slotY = startY + row * NearbyPanelLayout.SLOT_SIZE;
        if (mouseX > slotX + 18 || mouseY > slotY + 18) {
            return -1;
        }
        int index = (scrollOffset + row) * NearbyPanelLayout.COLUMNS + col;
        return index >= 0 && index < totalEntries ? index : -1;
    }

    private void clampScroll(int entryCount) {
        int totalRows = (int) Math.ceil(entryCount / (double) NearbyPanelLayout.COLUMNS);
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, totalRows - layout.rows()));
    }

    private boolean insidePanel(double mouseX, double mouseY) {
        return mouseX >= layout.panelX()
            && mouseX <= layout.panelX() + NearbyPanelLayout.PANEL_WIDTH
            && mouseY >= layout.panelY()
            && mouseY <= layout.panelY() + layout.panelHeight();
    }

    private void renderClickPulse(GuiGraphicsExtractor graphics, int totalEntries) {
        if (!EasyInventoryCrafterConfig.isHighlightEnabled() || lastClickIndex < 0 || lastClickIndex >= totalEntries) {
            return;
        }
        long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        long age = now - lastClickTick;
        int local = lastClickIndex - scrollOffset * NearbyPanelLayout.COLUMNS;
        if (age < 0 || age > 6 || local < 0 || local >= NearbyPanelLayout.COLUMNS * layout.rows()) {
            return;
        }
        int slotX = layout.panelX() + 3 + local % NearbyPanelLayout.COLUMNS * NearbyPanelLayout.SLOT_SIZE;
        int slotY = layout.panelY() + 14 + local / NearbyPanelLayout.COLUMNS * NearbyPanelLayout.SLOT_SIZE;
        int alpha = Mth.clamp((int) (160 * (1.0F - age / 6.0F)), 0, 160);
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, (alpha << 24) | EasyInventoryCrafterConfig.getHighlightColor());
    }

    private static int withOpacity(int color) {
        int alpha = color >>> 24 & 0xFF;
        int scaled = Mth.clamp(Math.round(alpha * EasyInventoryCrafterConfig.getNearbyPanelOpacity()), 0, 255);
        return scaled << 24 | color & 0x00FFFFFF;
    }

    private static int categoryRank(ItemStack stack) {
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.LOGS_THAT_BURN) || stack.is(ItemTags.PLANKS)) {
            return 0;
        }
        if (stack.is(ItemTags.COAL_ORES)
            || stack.is(ItemTags.IRON_ORES)
            || stack.is(ItemTags.COPPER_ORES)
            || stack.is(ItemTags.GOLD_ORES)
            || stack.is(ItemTags.REDSTONE_ORES)
            || stack.is(ItemTags.LAPIS_ORES)
            || stack.is(ItemTags.DIAMOND_ORES)
            || stack.is(ItemTags.EMERALD_ORES)) {
            return 1;
        }
        return stack.has(DataComponents.FOOD) ? 2 : 3;
    }

    private static String formatCount(long count) {
        if (count < 1_000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return count / 1_000 + "k";
        }
        if (count < 1_000_000_000) {
            return count / 1_000_000 + "M";
        }
        return count / 1_000_000_000 + "B";
    }
}
