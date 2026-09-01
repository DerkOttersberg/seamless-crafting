package com.derk.easyinventorycrafter.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes exact component-bearing stacks visible to recipe-book accounting. */
@Mixin(Inventory.class)
public class InventoryComponentCraftingMixin {
    @Inject(method = "fillStackedContents", at = @At("TAIL"))
    private void derk$accountComponentStacks(StackedItemContents contents, CallbackInfo ci) {
        Inventory inventory = (Inventory) (Object) this;
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            // Vanilla already accounted simple stacks; add only the protected
            // component-bearing stacks that vanilla deliberately skipped.
            if (!stack.isEmpty() && !Inventory.isUsableForCrafting(stack)) {
                contents.accountStack(stack);
            }
        }
    }
}
