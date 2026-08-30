package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.recipebook.ServerPlaceRecipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin<R extends net.minecraft.world.item.crafting.Recipe<?>> {
    @Shadow
    @Final
    private Inventory inventory;

    @Shadow
    @Final
    private ServerPlaceRecipe.CraftingMenuAccess<R> menu;

    @Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
    private void derk$fillFromNearby(Slot slot, Holder<Item> item, int remaining, CallbackInfoReturnable<Integer> cir) {
        int targetCount = remaining;
        ItemStack slotStack = slot.getItem();
        int availableInPlayer = derk$countInPlayerInventory(item, slotStack);
        if (availableInPlayer >= targetCount) {
            return;
        }

        AbstractCraftingMenu screenMenu = derk$resolveMenu();
        if (screenMenu == null) {
            return;
        }

        NearbyInventoryScanner.WorldPos worldPos = derk$getWorldPos(screenMenu);
        if (worldPos == null) {
            return;
        }

        List<Container> inventories = NearbyInventoryScanner.findNearbyInventories(
            worldPos.level(),
            worldPos.pos(),
            NearbyInventoryScanner.getConfiguredRadius(),
            inventory.player
        );

        int availableInNearby = derk$countInInventories(inventories, item, slotStack);
        if (availableInPlayer + availableInNearby < targetCount) {
            return;
        }

        int stillNeeded = targetCount;
        stillNeeded = derk$takeFromPlayerInventory(item, slotStack, slot, stillNeeded);
        slotStack = slot.getItem();
        if (stillNeeded <= 0) {
            cir.setReturnValue(0);
            return;
        }

        for (Container inv : inventories) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (!stack.is(item) || !Inventory.isUsableForCrafting(stack)) {
                    continue;
                }
                if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, stack)) {
                    continue;
                }

                int baselineCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
                int removeCount = Math.min(stillNeeded, stack.getCount());
                ItemStack removed = inv.removeItem(i, removeCount);
                if (removed.isEmpty()) {
                    continue;
                }

                if (slotStack.isEmpty()) {
                    slot.set(removed);
                    slotStack = removed;
                } else {
                    slotStack.grow(removed.getCount());
                    slot.setChanged();
                }

                if (screenMenu instanceof NearbyCraftingAccess access) {
                    access.derk$recordNearbyWithdrawal(inv, i, derk$getCraftingSlotIndex(screenMenu, slot), removed, removed.getCount(), baselineCount);
                }

                inv.setChanged();
                stillNeeded -= removed.getCount();
                if (stillNeeded <= 0) {
                    cir.setReturnValue(0);
                    return;
                }
            }
        }

        cir.setReturnValue(stillNeeded == targetCount ? -1 : stillNeeded);
    }

    @Inject(method = "clearGrid", at = @At("HEAD"))
    private void derk$returnNearbyInputsToOrigin(CallbackInfo ci) {
        AbstractCraftingMenu screenMenu = derk$resolveMenu();
        if (screenMenu instanceof NearbyCraftingAccess access) {
            access.derk$prepareNearbyWithdrawalsForAutofill();
        }
    }

    @Nullable
    private NearbyInventoryScanner.WorldPos derk$getWorldPos(AbstractCraftingMenu menu) {
        if (menu instanceof NearbyCraftingAccess access) {
            return access.derk$getAccess().evaluate((level, pos) -> new NearbyInventoryScanner.WorldPos(level, pos)).orElse(null);
        }
        return null;
    }

    private int derk$countInInventories(List<Container> inventories, Holder<Item> item, ItemStack slotStack) {
        int total = 0;
        for (Container inv : inventories) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (!stack.is(item) || !Inventory.isUsableForCrafting(stack)) {
                    continue;
                }
                if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, stack)) {
                    continue;
                }
                total += stack.getCount();
                if (total >= Integer.MAX_VALUE - 1) {
                    return total;
                }
            }
        }
        return total;
    }

    private int derk$countInPlayerInventory(Holder<Item> item, ItemStack slotStack) {
        int total = 0;
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(item) || !Inventory.isUsableForCrafting(stack)) {
                continue;
            }
            if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, stack)) {
                continue;
            }
            total += stack.getCount();
        }
        return total;
    }

    private int derk$takeFromPlayerInventory(Holder<Item> item, ItemStack slotStack, Slot slot, int remaining) {
        int stillNeeded = remaining;
        for (int i = 0; i < inventory.getNonEquipmentItems().size() && stillNeeded > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(item) || !Inventory.isUsableForCrafting(stack)) {
                continue;
            }
            if (!slotStack.isEmpty() && !ItemStack.isSameItemSameComponents(slotStack, stack)) {
                continue;
            }

            int removeCount = Math.min(stillNeeded, stack.getCount());
            ItemStack removed = inventory.removeItem(i, removeCount);
            if (removed.isEmpty()) {
                continue;
            }

            if (slotStack.isEmpty()) {
                slot.set(removed);
                slotStack = removed;
            } else {
                slotStack.grow(removed.getCount());
                slot.setChanged();
            }

            stillNeeded -= removed.getCount();
        }
        return stillNeeded;
    }

    @Nullable
    private AbstractCraftingMenu derk$resolveMenu() {
        return inventory.player.containerMenu instanceof AbstractCraftingMenu craftingMenu ? craftingMenu : null;
    }

    private int derk$getCraftingSlotIndex(AbstractCraftingMenu menu, Slot slot) {
        List<Slot> inputSlots = menu.getInputGridSlots();
        for (int i = 0; i < inputSlots.size(); i++) {
            if (inputSlots.get(i) == slot) {
                return i;
            }
        }
        return -1;
    }

}
