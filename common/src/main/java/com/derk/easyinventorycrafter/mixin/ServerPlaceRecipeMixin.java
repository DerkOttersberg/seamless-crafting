package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyRecipePlacementTransaction;
import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes nearby recipe-book placement through a whole-grid transaction. */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Shadow
    @Final
    private Inventory inventory;

    @Inject(method = "placeRecipe", at = @At("HEAD"), cancellable = true)
    private static <I extends net.minecraft.world.item.crafting.RecipeInput, R extends Recipe<I>> void derk$placeNearbyRecipeAtomically(
        ServerPlaceRecipe.CraftingMenuAccess<R> menu,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        List<Slot> slotsToClear,
        Inventory inventory,
        RecipeHolder<R> recipe,
        boolean useMaxItems,
        boolean isCreative,
        CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir
    ) {
        RecipeBookMenu.PostPlaceAction result = NearbyRecipePlacementTransaction.tryPlace(
            menu,
            gridWidth,
            gridHeight,
            inputGridSlots,
            inventory,
            recipe,
            useMaxItems
        );
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "clearGrid", at = @At("HEAD"))
    private void derk$returnNearbyInputsToOrigin(CallbackInfo ci) {
        if (inventory.player.containerMenu instanceof NearbyCraftingAccess access) {
            access.derk$prepareNearbyWithdrawalsForAutofill();
        }
    }
}
