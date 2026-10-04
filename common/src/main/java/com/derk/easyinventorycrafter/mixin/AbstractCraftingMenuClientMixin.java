package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CraftingMenu.class, InventoryMenu.class})
public class AbstractCraftingMenuClientMixin {
    @Inject(method = "fillCraftSlotsStackedContents", at = @At("TAIL"))
    private void derk$addNearbyClientStacks(StackedContents contents, CallbackInfo ci) {
        for (ItemStack stack : NearbyItemsClientState.getRecipeFinderStacks()) {
            contents.accountStack(stack);
        }
    }
}
