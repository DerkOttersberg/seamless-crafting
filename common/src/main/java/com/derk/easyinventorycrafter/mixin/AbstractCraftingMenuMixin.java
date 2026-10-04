package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.PendingNearbyWithdrawal;
import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CraftingMenu.class, InventoryMenu.class})
public abstract class AbstractCraftingMenuMixin implements NearbyCraftingAccess {
    @Unique
    private final List<PendingNearbyWithdrawal> derk$pendingNearbyWithdrawals = new ArrayList<>();

    @Unique
    private final Map<Integer, Integer> derk$slotBaselineCounts = new HashMap<>();

    @Unique
    private boolean derk$reconcilingNearbyWithdrawals;

    @Unique
    private boolean derk$cancellingNearbyWithdrawals;

    @Unique
    private boolean derk$autofillingNearbyWithdrawals;

    @Unique
    private Player owner() {
        return (Object) this instanceof CraftingMenu
            ? ((CraftingMenuAccessor) this).derk$getOwner()
            : ((InventoryMenuAccessor) this).derk$getOwner();
    }

    @Unique
    private List<Slot> getInputGridSlots() {
        RecipeBookMenu<?> menu = (RecipeBookMenu<?>) (Object) this;
        return menu.slots.subList(1, 1 + menu.getGridWidth() * menu.getGridHeight());
    }

    @Override
    public ContainerLevelAccess derk$getAccess() {
        if ((Object) this instanceof CraftingMenu) {
            return ((CraftingMenuAccessor) this).derk$getLevelAccess();
        }
        Player player = owner();
        return ContainerLevelAccess.create(player.level(), player.blockPosition());
    }

    @Override
    public Player derk$getPlayer() {
        return owner();
    }

    @Override
    public void derk$recordNearbyWithdrawal(
        NearbyStorage inventory,
        int sourceSlot,
        int craftingSlotIndex,
        ItemStack stack,
        int count,
        int baselineCount
    ) {
        if (count <= 0 || craftingSlotIndex < 0) {
            return;
        }

        derk$slotBaselineCounts.putIfAbsent(craftingSlotIndex, baselineCount);
        derk$pendingNearbyWithdrawals.add(new PendingNearbyWithdrawal(
            inventory,
            sourceSlot,
            craftingSlotIndex,
            stack.copyWithCount(1),
            count
        ));
    }

    @Override
    public void derk$prepareNearbyWithdrawalsForAutofill() {
        derk$returnNearbyWithdrawals(false);
    }

    @Override
    public void derk$cancelNearbyWithdrawals() {
        derk$returnNearbyWithdrawals(true);
    }

    @Override
    public void derk$reconcileNearbyWithdrawals() {
        if (derk$reconcilingNearbyWithdrawals || derk$pendingNearbyWithdrawals.isEmpty()) {
            return;
        }

        derk$reconcilingNearbyWithdrawals = true;
        try {
            List<Slot> inputSlots = getInputGridSlots();
            Map<Integer, Integer> cancelableCounts = new HashMap<>();
            for (Integer slotIndex : derk$slotBaselineCounts.keySet()) {
                if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
                    continue;
                }

                ItemStack slotStack = inputSlots.get(slotIndex).getItem();
                int baselineCount = derk$slotBaselineCounts.getOrDefault(slotIndex, 0);
                cancelableCounts.put(slotIndex, Math.max(0, slotStack.getCount() - baselineCount));
            }

            for (Iterator<PendingNearbyWithdrawal> iterator = derk$pendingNearbyWithdrawals.iterator(); iterator.hasNext();) {
                PendingNearbyWithdrawal withdrawal = iterator.next();
                int slotIndex = withdrawal.craftingSlotIndex();
                if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
                    iterator.remove();
                    continue;
                }

                ItemStack slotStack = inputSlots.get(slotIndex).getItem();
                if (slotStack.isEmpty() || !ItemStack.isSameItemSameTags(slotStack, withdrawal.templateStack())) {
                    iterator.remove();
                    continue;
                }

                int availableForCancel = cancelableCounts.getOrDefault(slotIndex, 0);
                if (availableForCancel <= 0) {
                    iterator.remove();
                    continue;
                }

                withdrawal.setRemainingCount(Math.min(withdrawal.remainingCount(), availableForCancel));
                cancelableCounts.put(slotIndex, availableForCancel - withdrawal.remainingCount());
                if (withdrawal.remainingCount() <= 0) {
                    iterator.remove();
                }
            }

            derk$cleanupSlotBaselines();
        } finally {
            derk$reconcilingNearbyWithdrawals = false;
        }
    }

    @Override
    public void derk$onCraftingSlotsChanged() {
        if (!derk$cancellingNearbyWithdrawals && !derk$autofillingNearbyWithdrawals) {
            derk$reconcileNearbyWithdrawals();
        }
    }

    @Override
    public void derk$beginAutofill() {
        derk$autofillingNearbyWithdrawals = true;
    }

    @Override
    public void derk$endAutofill() {
        derk$autofillingNearbyWithdrawals = false;
        derk$reconcileNearbyWithdrawals();
        if (owner() instanceof ServerPlayer serverPlayer) {
            NearbyItemsSync.sendNearbyItems(serverPlayer);
        }
    }

    @Unique
    private void derk$returnNearbyWithdrawals(boolean refreshAfter) {
        boolean changed = false;
        derk$cancellingNearbyWithdrawals = true;
        try {
            while (true) {
                derk$reconcileNearbyWithdrawals();
                if (derk$pendingNearbyWithdrawals.isEmpty()) {
                    break;
                }

                List<Slot> inputSlots = getInputGridSlots();
                boolean passChanged = false;
                for (Iterator<PendingNearbyWithdrawal> iterator = derk$pendingNearbyWithdrawals.iterator(); iterator.hasNext();) {
                    PendingNearbyWithdrawal withdrawal = iterator.next();
                    int slotIndex = withdrawal.craftingSlotIndex();
                    if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
                        iterator.remove();
                        continue;
                    }

                    Slot slot = inputSlots.get(slotIndex);
                    ItemStack slotStack = slot.getItem();
                    if (slotStack.isEmpty() || !ItemStack.isSameItemSameTags(slotStack, withdrawal.templateStack())) {
                        iterator.remove();
                        continue;
                    }

                    int removableCount = Math.min(withdrawal.remainingCount(), slotStack.getCount());
                    if (removableCount <= 0) {
                        iterator.remove();
                        continue;
                    }

                    ItemStack toReturn = withdrawal.templateStack().copyWithCount(removableCount);
                    int insertedCount = withdrawal.sourceInventory().insertExact(
                        withdrawal.sourceSlot(),
                        toReturn
                    );
                    if (insertedCount <= 0) {
                        continue;
                    }

                    if (insertedCount == slotStack.getCount()) {
                        slot.set(ItemStack.EMPTY);
                    } else {
                        slotStack.shrink(insertedCount);
                        slot.setChanged();
                    }

                    withdrawal.setRemainingCount(withdrawal.remainingCount() - insertedCount);
                    withdrawal.sourceInventory().markChanged();
                    passChanged = true;
                    changed = true;
                    if (withdrawal.remainingCount() <= 0) {
                        iterator.remove();
                    }
                }

                derk$cleanupSlotBaselines();
                if (!passChanged) {
                    break;
                }
            }
        } finally {
            derk$cancellingNearbyWithdrawals = false;
        }

        if (changed && refreshAfter) {
            derk$refreshAfterNearbyTransfer();
        }
    }

    @Unique
    private void derk$cleanupSlotBaselines() {
        derk$slotBaselineCounts.entrySet().removeIf(entry -> derk$pendingNearbyWithdrawals.stream()
            .noneMatch(withdrawal -> withdrawal.craftingSlotIndex() == entry.getKey()));
    }

    @Unique
    private void derk$refreshAfterNearbyTransfer() {
        List<Slot> inputSlots = getInputGridSlots();
        if (!inputSlots.isEmpty()) {
            ((RecipeBookMenu<?>) (Object) this).slotsChanged(inputSlots.get(0).container);
        }
        ((RecipeBookMenu<?>) (Object) this).broadcastChanges();
        if (owner() instanceof ServerPlayer serverPlayer) {
            NearbyItemsSync.sendNearbyItems(serverPlayer);
        }
    }
}
