package com.derk.easyinventorycrafter;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class PendingNearbyWithdrawal {
    private final Container sourceInventory;
    private final int sourceSlot;
    private final int craftingSlotIndex;
    private final ItemStack templateStack;
    private int remainingCount;

    public PendingNearbyWithdrawal(Container sourceInventory, int sourceSlot, int craftingSlotIndex, ItemStack templateStack, int remainingCount) {
        this.sourceInventory = sourceInventory;
        this.sourceSlot = sourceSlot;
        this.craftingSlotIndex = craftingSlotIndex;
        this.templateStack = templateStack;
        this.remainingCount = remainingCount;
    }

    public Container sourceInventory() {
        return sourceInventory;
    }

    public int sourceSlot() {
        return sourceSlot;
    }

    public int craftingSlotIndex() {
        return craftingSlotIndex;
    }

    public ItemStack templateStack() {
        return templateStack;
    }

    public int remainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
    }
}
