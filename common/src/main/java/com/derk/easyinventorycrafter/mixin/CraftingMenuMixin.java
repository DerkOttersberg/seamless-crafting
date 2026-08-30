package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(
        method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL")
    )
    private void derk$sendInitialNearbyItems(
        int syncId,
        Inventory inventory,
        ContainerLevelAccess access,
        CallbackInfo ci
    ) {
        if (inventory.player instanceof ServerPlayer serverPlayer) {
            NearbyItemsSync.sendNearbyItems(serverPlayer);
        }
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void derk$reconcileOnContentChanged(Container container, CallbackInfo ci) {
        ((NearbyCraftingAccess) this).derk$onCraftingSlotsChanged();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void derk$returnNearbyInputsBeforeClose(Player player, CallbackInfo ci) {
        ((NearbyCraftingAccess) this).derk$cancelNearbyWithdrawals();
    }
}
