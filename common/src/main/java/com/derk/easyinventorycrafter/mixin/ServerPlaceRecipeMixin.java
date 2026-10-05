package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyRecipePlacementTransaction;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plans and validates the full grid before touching nearby storage. */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Shadow protected Inventory inventory;
    @Shadow protected RecipeBookMenu<?, ?> menu;

    @Inject(method = "recipeClicked", at = @At("HEAD"), cancellable = true)
    private void derk$placeNearbyRecipeAtomically(ServerPlayer player, RecipeHolder<?> recipe,
                                                boolean useMaxItems, CallbackInfo ci) {
        if (!(menu instanceof NearbyCraftingAccess) || player.containerMenu != menu) {
            return;
        }
        // Never let a client place an unknown recipe or use a stale/distant menu.
        if (recipe == null || !player.getRecipeBook().contains(recipe) || !menu.stillValid(player)) {
            ci.cancel();
            return;
        }
        if (NearbyRecipePlacementTransaction.tryPlace(menu, menu.getGridWidth(), menu.getGridHeight(),
                menu.slots.subList(1, 1 + menu.getGridWidth() * menu.getGridHeight()),
                player.getInventory(), recipe, useMaxItems) != null) {
            ci.cancel();
        }
    }

    @Inject(method = "clearGrid", at = @At("HEAD"))
    private void derk$returnNearbyInputsToOrigin(CallbackInfo ci) {
        if (inventory != null && inventory.player.containerMenu instanceof NearbyCraftingAccess access) {
            access.derk$prepareNearbyWithdrawalsForAutofill();
        }
    }
}
