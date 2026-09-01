package com.derk.easyinventorycrafter.client;

import net.minecraft.world.item.ItemStack;

public record HoveredNearbyStack(ItemStack stack, PanelBounds bounds) {
    public HoveredNearbyStack {
        stack = stack.copyWithCount(1);
    }
}
