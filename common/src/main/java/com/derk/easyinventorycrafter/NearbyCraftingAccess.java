package com.derk.easyinventorycrafter;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public interface NearbyCraftingAccess {
    ContainerLevelAccess derk$getAccess();

    Player derk$getPlayer();

    void derk$recordNearbyWithdrawal(Container inventory, int sourceSlot, int craftingSlotIndex, ItemStack stack, int count, int baselineCount);

    void derk$prepareNearbyWithdrawalsForAutofill();

    void derk$cancelNearbyWithdrawals();

    void derk$reconcileNearbyWithdrawals();

    void derk$onCraftingSlotsChanged();
}
