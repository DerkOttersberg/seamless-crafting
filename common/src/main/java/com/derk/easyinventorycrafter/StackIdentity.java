package com.derk.easyinventorycrafter;

import net.minecraft.world.item.ItemStack;

/**
 * Immutable, count-free identity for an item and all of its data components.
 *
 * <p>{@link ItemStack} deliberately has identity equality, so it cannot be
 * used directly as a component-aware map key.</p>
 */
public final class StackIdentity {
    private final ItemStack representative;
    private final int hash;

    private StackIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("stack must not be empty");
        }
        representative = stack.copyWithCount(1);
        hash = ItemStack.hashItemAndComponents(representative);
    }

    public static StackIdentity of(ItemStack stack) {
        return new StackIdentity(stack);
    }

    public ItemStack stack() {
        return representative.copy();
    }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ItemStack.isSameItemSameComponents(representative, stack);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
            || other instanceof StackIdentity identity
            && ItemStack.isSameItemSameComponents(representative, identity.representative);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return representative.toString();
    }
}
