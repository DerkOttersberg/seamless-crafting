package com.derk.easyinventorycrafter;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable, count-free identity for an item and all of its data components.
 *
 * <p>{@link ItemStack} deliberately has identity equality, so it cannot be
 * used directly as a component-aware map key.</p>
 */
public final class StackIdentity implements Comparable<StackIdentity> {
    private final ItemStack representative;
    private final int hash;
    private final String componentSortKey;

    private StackIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("stack must not be empty");
        }
        representative = stack.copyWithCount(1);
        hash = ItemStack.hashItemAndComponents(representative);
        componentSortKey = canonicalComponentSortKey(representative);
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
    public int compareTo(StackIdentity other) {
        Objects.requireNonNull(other, "other");
        if (equals(other)) {
            return 0;
        }
        int byItem = BuiltInRegistries.ITEM.getKey(representative.getItem()).toString()
            .compareTo(BuiltInRegistries.ITEM.getKey(other.representative.getItem()).toString());
        if (byItem != 0) {
            return byItem;
        }
        int byName = representative.getHoverName().getString()
            .compareToIgnoreCase(other.representative.getHoverName().getString());
        if (byName != 0) {
            return byName;
        }
        int byNameCase = representative.getHoverName().getString()
            .compareTo(other.representative.getHoverName().getString());
        if (byNameCase != 0) {
            return byNameCase;
        }
        int byComponents = componentSortKey.compareTo(other.componentSortKey);
        if (byComponents != 0) {
            return byComponents;
        }
        return Integer.compare(hash, other.hash);
    }

    private static String canonicalComponentSortKey(ItemStack stack) {
        return stack.getComponentsPatch().entrySet().stream()
            .sorted((first, second) -> componentTypeKey(first.getKey()).compareTo(componentTypeKey(second.getKey())))
            .map(entry -> componentTypeKey(entry.getKey()) + "=" + componentValueKey(entry.getValue()))
            .collect(Collectors.joining("\u0000"));
    }

    private static String componentTypeKey(DataComponentType<?> type) {
        var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return key == null ? type.getClass().getName() : key.toString();
    }

    private static String componentValueKey(Optional<?> value) {
        if (value.isEmpty()) {
            return "!";
        }
        Object component = value.get();
        return component.getClass().getName() + ":" + component;
    }

    @Override
    public String toString() {
        return representative.toString();
    }
}
