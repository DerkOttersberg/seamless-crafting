package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyRecipeBookComponentAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin implements NearbyRecipeBookComponentAccess {
    @Shadow private RecipeBookPage recipeBookPage;
    @Shadow protected RecipeBookMenu<?> menu;
    @Shadow public abstract boolean isVisible();
    @Unique private Recipe<?> derk$lastRecipe;

    @Invoker("updateStackedContents")
    protected abstract void derk$invokeUpdateStackedContents();

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void derk$rememberRecipe(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && recipeBookPage.getLastClickedRecipe() != null) {
            derk$lastRecipe = recipeBookPage.getLastClickedRecipe();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void derk$spacebarAddsOneSet(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (keyCode == 32 && isVisible() && derk$lastRecipe != null
                && minecraft.player != null && minecraft.gameMode != null) {
            minecraft.gameMode.handlePlaceRecipe(menu.containerId, derk$lastRecipe, false);
            cir.setReturnValue(true);
        }
    }

    @Override
    public void derk$refreshStackedContents() { derk$invokeUpdateStackedContents(); }
}
