package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyRecipeBookRefreshAccess;
import com.derk.easyinventorycrafter.client.NearbyRecipeBookComponentAccess;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin implements NearbyRecipeBookRefreshAccess {
    @Shadow
    private RecipeBookComponent<?> recipeBookComponent;

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void derk$handleCharTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof NearbyPanelAccess access) {
            if (access.derk$handleCharTyped(input)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void derk$handleKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof NearbyPanelAccess access) {
            if (access.derk$handleKeyPressed(input)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Override
    public void derk$refreshNearbyRecipeBook() {
        // A hidden recipe book has not created its tab widgets yet. Calling
        // updateStackedContents in that state reaches updateCollections with a
        // null selectedTab on Minecraft 26.2. Vanilla initializes the tabs when
        // the book is opened, so defer the nearby refresh until it is visible.
        if (recipeBookComponent.isVisible()) {
            ((NearbyRecipeBookComponentAccess) recipeBookComponent).derk$refreshStackedContents();
        }
    }

    @Override
    public boolean derk$isRecipeBookVisible() {
        return recipeBookComponent.isVisible();
    }
}
