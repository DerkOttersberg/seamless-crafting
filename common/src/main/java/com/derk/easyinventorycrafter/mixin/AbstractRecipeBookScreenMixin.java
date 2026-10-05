package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyRecipeBookRefreshAccess;
import com.derk.easyinventorycrafter.client.NearbyRecipeBookComponentAccess;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 1.21.1 has two recipe-book screens, without a shared recipe-book superclass. */
@Mixin({CraftingScreen.class, InventoryScreen.class})
public abstract class AbstractRecipeBookScreenMixin extends AbstractContainerScreen<AbstractContainerMenu>
        implements NearbyRecipeBookRefreshAccess {
    private RecipeBookComponent derk$recipeBook() {
        return ((net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener) (Object) this).getRecipeBookComponent();
    }

    protected AbstractRecipeBookScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if ((Object) this instanceof NearbyPanelAccess access && access.derk$handleCharTyped(codePoint, modifiers)) return true;
        return derk$recipeBook().charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((Object) this instanceof NearbyPanelAccess access && access.derk$handleKeyPressed(keyCode, scanCode, modifiers)) return true;
        return derk$recipeBook().keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double amount) {
        if ((Object) this instanceof NearbyPanelAccess access && access.derk$handleScroll(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, amount);
    }

    @Override
    public void derk$refreshNearbyRecipeBook() {
        // Tabs are initialized only while the book is visible.
        if (derk$recipeBook().isVisible()) {
            ((NearbyRecipeBookComponentAccess) derk$recipeBook()).derk$refreshStackedContents();
        }
    }

    @Override
    public boolean derk$isRecipeBookVisible() {
        return derk$recipeBook().isVisible();
    }
}
