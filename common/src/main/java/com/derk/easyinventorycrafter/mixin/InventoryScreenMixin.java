package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> implements NearbyPanelAccess {
    @Unique
    private Button derk$nearbyButton;

    @Unique
    private EditBox derk$searchField;

    @Unique
    private int derk$scrollOffset;

    @Unique
    private boolean derk$nearbyOpen = true;

    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void derk$initNearbyPanel(CallbackInfo ci) {
        NearbyItemsClientState.clear();
        derk$nearbyOpen = EasyInventoryCrafterConfig.isNearbyPanelOpenByDefault();
        int buttonX = this.leftPos + this.imageWidth + 6;
        int buttonY = this.topPos + 6;

        derk$nearbyButton = this.addRenderableWidget(Button.builder(Component.literal("Nearby"), btn -> {
            derk$nearbyOpen = !derk$nearbyOpen;
            if (derk$nearbyOpen) {
                NearbyItemsClientState.requestUpdate();
            }
        }).bounds(buttonX, buttonY, 60, 20).build());

        derk$searchField = new EditBox(this.font, buttonX, buttonY + 24, 84, 14, Component.empty());
        derk$searchField.setMaxLength(50);
        derk$searchField.setHint(Component.literal("Search..."));
        this.addRenderableWidget(derk$searchField);

        derk$scrollOffset = 0;
        NearbyItemsClientState.requestUpdate();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void derk$drawNearbyPanel(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (derk$nearbyButton != null) {
            derk$nearbyButton.setPosition(this.leftPos + this.imageWidth + 6, this.topPos + 6);
        }
        if (derk$searchField != null) {
            derk$searchField.setPosition(this.leftPos + this.imageWidth + 6, this.topPos + 30);
            derk$searchField.setVisible(derk$nearbyOpen);
        }

        if (!derk$nearbyOpen) {
            return;
        }

        List<NearbyItemEntry> entries = derk$getFilteredEntries();
        if (entries.isEmpty()) {
            return;
        }

        int panelX = this.leftPos + this.imageWidth + 6;
        int panelY = this.topPos + 48;
        int columns = 4;
        int rows = 6;
        int slotSize = 21;
        int panelWidth = columns * slotSize + 6;
        int panelHeight = rows * slotSize + 16;
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, derk$withPanelOpacity(0x88000000));
        guiGraphics.text(this.font, Component.literal("Nearby"), panelX + 4, panelY + 4, 0xFFFFFF, true);

        int startX = panelX + 3;
        int startY = panelY + 14;
        int maxItems = columns * rows;
        int totalRows = (int) Math.ceil(entries.size() / (double) columns);
        int maxScroll = Math.max(0, totalRows - rows);
        derk$scrollOffset = Math.max(0, Math.min(derk$scrollOffset, maxScroll));

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotX = startX + col * slotSize;
                int slotY = startY + row * slotSize;
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, derk$withPanelOpacity(0x55000000));
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, derk$withPanelOpacity(0x2A000000));
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 1, derk$withPanelOpacity(0x66FFFFFF));
                guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 18, derk$withPanelOpacity(0x66FFFFFF));
                guiGraphics.fill(slotX, slotY + 17, slotX + 18, slotY + 18, derk$withPanelOpacity(0x33000000));
                guiGraphics.fill(slotX + 17, slotY, slotX + 18, slotY + 18, derk$withPanelOpacity(0x33000000));
            }
        }

        int startIndex = derk$scrollOffset * columns;
        int endIndex = Math.min(entries.size(), startIndex + maxItems);
        for (int index = startIndex; index < endIndex; index++) {
            int gridIndex = index - startIndex;
            int col = gridIndex % columns;
            int row = gridIndex / columns;
            int itemX = startX + col * slotSize + 2;
            int itemY = startY + row * slotSize + 1;
            NearbyItemEntry entry = entries.get(index);
            guiGraphics.item(entry.stack(), itemX, itemY);
            guiGraphics.itemDecorations(this.font, entry.stack(), itemX, itemY, derk$formatCount(entry.count()));
        }

        int hoveredIndex = derk$getHoveredIndex(mouseX, mouseY, entries.size(), panelX, panelY);
        if (hoveredIndex >= 0) {
            guiGraphics.setTooltipForNextFrame(this.font, entries.get(hoveredIndex).stack(), mouseX, mouseY);
        }
    }

    @Unique
    private static int derk$withPanelOpacity(int color) {
        int alpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(alpha * EasyInventoryCrafterConfig.getNearbyPanelOpacity())));
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean derk$handleMouseClick(MouseButtonEvent click, boolean doubleClick) {
        if (!derk$nearbyOpen || click.button() != 0) {
            return false;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int panelX = this.leftPos + this.imageWidth + 6;
        int panelY = this.topPos + 48;
        int columns = 4;
        int rows = 6;
        int slotSize = 21;
        int panelWidth = columns * slotSize + 6;
        int panelHeight = rows * slotSize + 16;

        if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < panelY || mouseY > panelY + panelHeight) {
            return false;
        }

        List<NearbyItemEntry> entries = derk$getFilteredEntries();
        if (entries.isEmpty()) {
            return false;
        }

        int index = derk$getHoveredIndex(mouseX, mouseY, entries.size(), panelX, panelY);
        if (index < 0 || index >= entries.size()) {
            return false;
        }

        NearbyItemEntry entry = entries.get(index);
        NearbyItemsClientState.requestHighlightAndAim(entry.stack());
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        this.onClose();
        return true;
    }

    @Override
    public boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount) {
        if (!derk$nearbyOpen) {
            return false;
        }

        int panelX = this.leftPos + this.imageWidth + 6;
        int panelY = this.topPos + 48;
        int columns = 4;
        int rows = 6;
        int slotSize = 21;
        int panelWidth = columns * slotSize + 6;
        int panelHeight = rows * slotSize + 16;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            int delta = verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
            derk$scrollOffset += delta;
            return true;
        }

        return false;
    }

    @Override
    public boolean derk$handleCharTyped(CharacterEvent input) {
        if (!derk$nearbyOpen) {
            return false;
        }
        if (derk$searchField != null && derk$searchField.charTyped(input)) {
            derk$scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean derk$handleKeyPressed(KeyEvent input) {
        if (!derk$nearbyOpen) {
            return false;
        }
        if (derk$searchField != null && derk$searchField.keyPressed(input)) {
            derk$scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Unique
    private int derk$getHoveredIndex(double mouseX, double mouseY, int totalEntries, int panelX, int panelY) {
        int columns = 4;
        int rows = 6;
        int slotSize = 21;
        int startX = panelX + 3;
        int startY = panelY + 14;
        int relX = (int) mouseX - startX;
        int relY = (int) mouseY - startY;
        if (relX < 0 || relY < 0) {
            return -1;
        }
        int col = relX / slotSize;
        int row = relY / slotSize;
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            return -1;
        }

        int slotX = startX + col * slotSize;
        int slotY = startY + row * slotSize;
        if (mouseX > slotX + 18 || mouseY > slotY + 18) {
            return -1;
        }

        int index = (derk$scrollOffset + row) * columns + col;
        if (index < 0 || index >= totalEntries) {
            return -1;
        }
        return index;
    }

    @Unique
    private List<NearbyItemEntry> derk$getFilteredEntries() {
        List<NearbyItemEntry> entries = NearbyItemsClientState.getEntries();
        String query = derk$searchField == null ? "" : derk$searchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<NearbyItemEntry> filtered = new ArrayList<>();
        for (NearbyItemEntry entry : entries) {
            String name = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query)) {
                filtered.add(entry);
            }
        }
        filtered.sort(Comparator.comparingInt((NearbyItemEntry e) -> derk$getCategoryRank(e.stack()))
            .thenComparing(e -> e.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
        return filtered;
    }

    @Unique
    private int derk$getCategoryRank(ItemStack stack) {
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
        if (stack.has(DataComponents.FOOD)) {
            return 2;
        }
        return 3;
    }

    @Unique
    private static String derk$formatCount(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        if (count < 1_000_000) {
            return (count / 1000) + "k";
        }
        if (count < 1_000_000_000) {
            return (count / 1_000_000) + "M";
        }
        return (count / 1_000_000_000) + "B";
    }
}
