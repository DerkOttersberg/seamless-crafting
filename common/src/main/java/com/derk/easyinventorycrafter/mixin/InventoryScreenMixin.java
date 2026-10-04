package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.client.HoveredNearbyStack;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import com.derk.easyinventorycrafter.client.NearbyPanelController;
import com.derk.easyinventorycrafter.client.NearbyPanelLayout;
import com.derk.easyinventorycrafter.client.NearbyRecipeBookRefreshAccess;
import com.derk.easyinventorycrafter.client.PanelBounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> implements NearbyPanelAccess {
    @Unique
    private final NearbyPanelController derk$nearbyPanel = new NearbyPanelController();
    @Unique
    private Button derk$nearbyButton;
    @Unique
    private EditBox derk$searchField;

    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void derk$initNearbyPanel(CallbackInfo ci) {
        NearbyItemsClientState.clear();
        boolean open = EasyInventoryCrafterConfig.isNearbyPanelOpenByDefault();
        derk$nearbyPanel.initialize(open, null);
        NearbyPanelLayout layout = derk$updateLayout();
        derk$nearbyButton = this.addRenderableWidget(Button.builder(Component.literal("Nearby"), button -> derk$nearbyPanel.toggleOpen())
            .bounds(layout.buttonX(), layout.buttonY(), 60, 20).build());
        derk$searchField = new EditBox(this.font, layout.buttonX(), layout.searchY(), 84, 14, Component.empty());
        derk$searchField.setMaxLength(50);
        derk$searchField.setHint(Component.literal("Search..."));
        this.addRenderableWidget(derk$searchField);
        derk$nearbyPanel.initialize(open, derk$searchField);
        NearbyItemsClientState.requestUpdate();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void derk$drawNearbyPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NearbyPanelLayout layout = derk$updateLayout();
        derk$nearbyButton.setX(layout.buttonX());
        derk$nearbyButton.setY(layout.buttonY());
        derk$searchField.setX(layout.buttonX());
        derk$searchField.setY(layout.searchY());
        derk$searchField.visible = layout.expanded();
        derk$nearbyPanel.render(graphics, this.font, mouseX, mouseY);
    }

    @Unique
    private NearbyPanelLayout derk$updateLayout() {
        return derk$nearbyPanel.updateLayout(
            this.width,
            this.height,
            this.leftPos,
            this.topPos,
            this.imageWidth,
            ((NearbyRecipeBookRefreshAccess) (Object) this).derk$isRecipeBookVisible(),
            60
        );
    }

    @Override
    public List<PanelBounds> derk$getVisiblePanelBounds() {
        derk$updateLayout();
        return derk$nearbyPanel.visibleBounds();
    }

    @Override
    public List<PanelBounds> derk$getOverlayExclusionBounds() {
        derk$updateLayout();
        return derk$nearbyPanel.overlayExclusionBounds();
    }

    @Override
    public Optional<HoveredNearbyStack> derk$getNearbyStackAt(double mouseX, double mouseY) {
        derk$updateLayout();
        return derk$nearbyPanel.hoveredStack(mouseX, mouseY);
    }

    @Override
    public boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount) {
        derk$updateLayout();
        return derk$nearbyPanel.handleScroll(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean derk$handleCharTyped(char codePoint, int modifiers) {
        derk$updateLayout();
        return derk$nearbyPanel.handleCharTyped(codePoint, modifiers);
    }

    @Override
    public boolean derk$handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        derk$updateLayout();
        return derk$nearbyPanel.handleKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean derk$handleMouseClick(double mouseX, double mouseY, int button) {
        derk$updateLayout();
        if (derk$nearbyPanel.handleMouseClick(mouseX, mouseY, button)) {
            this.onClose();
            return true;
        }
        return false;
    }
}
