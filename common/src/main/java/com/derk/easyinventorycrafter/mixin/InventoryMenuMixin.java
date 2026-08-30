package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void derk$reconcileOnContentChanged(Container container, CallbackInfo ci) {
        ((NearbyCraftingAccess) this).derk$onCraftingSlotsChanged();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void derk$returnNearbyInputsBeforeClose(Player player, CallbackInfo ci) {
        ((NearbyCraftingAccess) this).derk$cancelNearbyWithdrawals();
    }
}
