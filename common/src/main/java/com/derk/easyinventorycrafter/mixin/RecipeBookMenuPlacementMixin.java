package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookMenu.class)
public abstract class RecipeBookMenuPlacementMixin {
    @Inject(method = "handlePlacement", at = @At("HEAD"))
    private void derk$begin(boolean max, Recipe<?> recipe, ServerPlayer player, CallbackInfo ci) {
        if ((Object) this instanceof NearbyCraftingAccess access) access.derk$beginAutofill();
    }

    @Inject(method = "handlePlacement", at = @At("RETURN"))
    private void derk$end(boolean max, Recipe<?> recipe, ServerPlayer player, CallbackInfo ci) {
        if ((Object) this instanceof NearbyCraftingAccess access) access.derk$endAutofill();
    }
}
