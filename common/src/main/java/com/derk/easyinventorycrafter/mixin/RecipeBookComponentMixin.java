package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyRecipeBookComponentAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin implements NearbyRecipeBookComponentAccess {
    @Shadow
    @Nullable
    private RecipeCollection lastRecipeCollection;

    @Shadow
    @Nullable
    private RecipeDisplayId lastRecipe;

    @Invoker("tryPlaceRecipe")
    protected abstract boolean derk$invokeTryPlaceRecipe(RecipeCollection collection, RecipeDisplayId recipeId, boolean shiftDown);

    @Invoker("updateStackedContents")
    protected abstract void derk$invokeUpdateStackedContents();

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void derk$spacebarAddsOneSet(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() != 32) {
            return;
        }
        if (lastRecipeCollection == null || lastRecipe == null) {
            return;
        }
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        cir.setReturnValue(derk$invokeTryPlaceRecipe(lastRecipeCollection, lastRecipe, false));
    }

    @Override
    public void derk$refreshStackedContents() {
        derk$invokeUpdateStackedContents();
    }
}
