package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.client.HoveredNearbyStack;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import com.derk.easyinventorycrafter.client.NearbyPanelController;
import com.derk.easyinventorycrafter.client.NearbyPanelLayout;
import com.derk.easyinventorycrafter.client.NearbyRecipeBookRefreshAccess;
import com.derk.easyinventorycrafter.client.PanelBounds;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import com.derk.easyinventorycrafter.net.ReturnNearbyItemsPacket;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractRecipeBookScreen<CraftingMenu> implements NearbyPanelAccess {
    @Unique
    private final NearbyPanelController derk$nearbyPanel = new NearbyPanelController();
    @Unique
    private Button derk$nearbyButton;
    @Unique
    private Button derk$cancelButton;
    @Unique
    private EditBox derk$searchField;

    protected CraftingScreenMixin(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void derk$initNearbyPanel(CallbackInfo ci) {
        NearbyItemsClientState.clear();
        boolean open = EasyInventoryCrafterConfig.isNearbyPanelOpenByDefault();
        derk$nearbyPanel.initialize(open, null);
        NearbyPanelLayout layout = derk$updateLayout();
        derk$nearbyButton = this.addRenderableWidget(Button.builder(Component.literal("Nearby"), button -> derk$nearbyPanel.toggleOpen())
            .bounds(layout.buttonX(), layout.buttonY(), 60, 20).build());
        derk$cancelButton = this.addRenderableWidget(Button.builder(
                Component.literal("X"),
                button -> EasyInventoryCrafterNetwork.sendToServer(new ReturnNearbyItemsPacket())
            )
            .bounds(layout.buttonX() + 64, layout.buttonY(), 20, 20)
            .tooltip(Tooltip.create(Component.literal("Returns nearby items to chest")))
            .build());
        derk$searchField = new EditBox(this.font, layout.buttonX(), layout.searchY(), 84, 14, Component.empty());
        derk$searchField.setMaxLength(50);
        derk$searchField.setHint(Component.literal("Search..."));
        this.addRenderableWidget(derk$searchField);
        derk$nearbyPanel.initialize(open, derk$searchField);
        NearbyItemsClientState.requestUpdate();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void derk$drawNearbyPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NearbyPanelLayout layout = derk$updateLayout();
        derk$nearbyButton.setPosition(layout.buttonX(), layout.buttonY());
        derk$cancelButton.setPosition(layout.buttonX() + 64, layout.buttonY());
        derk$searchField.setPosition(layout.buttonX(), layout.searchY());
        derk$searchField.setVisible(layout.expanded());
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
            84
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
    public boolean derk$handleCharTyped(CharacterEvent input) {
        derk$updateLayout();
        return derk$nearbyPanel.handleCharTyped(input);
    }

    @Override
    public boolean derk$handleKeyPressed(KeyEvent input) {
        derk$updateLayout();
        return derk$nearbyPanel.handleKeyPressed(input);
    }

    @Override
    public boolean derk$handleMouseClick(MouseButtonEvent click, boolean doubleClick) {
        derk$updateLayout();
        if (derk$nearbyPanel.handleMouseClick(click)) {
            this.onClose();
            return true;
        }
        return false;
    }
}
