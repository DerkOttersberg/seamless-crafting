package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plans the complete recipe grid before changing player or external storage.
 * Every source is revalidated immediately before the commit, and a failed
 * commit restores the player/grid snapshots and returns extracted items.
 */
public final class NearbyRecipePlacementTransaction {
    private static final Logger LOGGER = LoggerFactory.getLogger("Seamless Crafting/Placement");
    private static final int MAX_COMPONENT_PLAN_BRANCHES = 4_096;

    private NearbyRecipePlacementTransaction() {
    }

    @Nullable
    public static <R extends Recipe<?>> RecipeBookMenu.PostPlaceAction tryPlace(
        ServerPlaceRecipe.CraftingMenuAccess<R> menuAccess,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        Inventory inventory,
        RecipeHolder<R> recipe,
        boolean useMaxItems
    ) {
        if (!(inventory.player.containerMenu instanceof AbstractCraftingMenu craftingMenu)
            || !(craftingMenu instanceof NearbyCraftingAccess nearbyAccess)) {
            return null;
        }

        // Finish or conservatively abandon the previous placement before a
        // new plan snapshots the grid.
        nearbyAccess.derk$prepareNearbyWithdrawalsForAutofill();
        NearbyInventoryScanner.WorldPos worldPos = nearbyAccess.derk$getAccess()
            .evaluate((level, pos) -> new NearbyInventoryScanner.WorldPos(level, pos))
            .orElse(null);
        if (worldPos == null) {
            return null;
        }

        List<NearbyStorage> storages = NearbyInventoryScanner.findNearbyStorages(
            worldPos.level(),
            worldPos.pos(),
            NearbyInventoryScanner.getConfiguredRadius(),
            inventory.player
        );
        return tryPlaceWithResolvedStorages(
            menuAccess,
            gridWidth,
            gridHeight,
            inputGridSlots,
            inventory,
            recipe,
            useMaxItems,
            nearbyAccess,
            storages
        );
    }

    /** Internal live-test seam for exercising commit races after storage admission. */
    @Nullable
    public static <R extends Recipe<?>> RecipeBookMenu.PostPlaceAction tryPlaceWithStoragesForTesting(
        ServerPlaceRecipe.CraftingMenuAccess<R> menuAccess,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        Inventory inventory,
        RecipeHolder<R> recipe,
        boolean useMaxItems,
        List<NearbyStorage> storages
    ) {
        if (!(inventory.player.containerMenu instanceof AbstractCraftingMenu craftingMenu)
            || !(craftingMenu instanceof NearbyCraftingAccess nearbyAccess)) {
            return null;
        }
        nearbyAccess.derk$prepareNearbyWithdrawalsForAutofill();
        return tryPlaceWithResolvedStorages(
            menuAccess,
            gridWidth,
            gridHeight,
            inputGridSlots,
            inventory,
            recipe,
            useMaxItems,
            nearbyAccess,
            List.copyOf(storages)
        );
    }

    @Nullable
    private static <R extends Recipe<?>> RecipeBookMenu.PostPlaceAction tryPlaceWithResolvedStorages(
        ServerPlaceRecipe.CraftingMenuAccess<R> menuAccess,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        Inventory inventory,
        RecipeHolder<R> recipe,
        boolean useMaxItems,
        NearbyCraftingAccess nearbyAccess,
        List<NearbyStorage> storages
    ) {
        List<ItemStack> playerSnapshot = copyStacks(inventory.getNonEquipmentItems());
        List<ItemStack> gridSnapshot = inputGridSlots.stream().map(slot -> slot.getItem().copy()).toList();
        List<Source> sources = snapshotSources(playerSnapshot, gridSnapshot, storages);
        StackedItemContents combined = accountSources(sources, inputGridSlots.size());
        if (!combined.canCraft(recipe.value(), null)) {
            return null;
        }

        boolean recipeMatches = menuAccess.recipeMatches(recipe);
        int biggest = combined.getBiggestCraftableStack(recipe.value(), null);
        if (biggest <= 0 || recipeMatches && cannotIncrementExistingGrid(inputGridSlots, biggest)) {
            return null;
        }

        int requestedAmount = calculateAmount(useMaxItems, recipeMatches, biggest, inputGridSlots);
        int minimumAmount = useMaxItems ? 1 : requestedAmount;
        for (int amount = requestedAmount; amount >= minimumAmount; amount--) {
            List<Holder<Item>> chosenItems = new ArrayList<>();
            if (!combined.canCraft(recipe.value(), amount, chosenItems::add)) {
                continue;
            }
            amount = clampToDefaultStackSize(amount, chosenItems);
            if (amount <= 0) {
                return null;
            }
            chosenItems.clear();
            if (!combined.canCraft(recipe.value(), amount, chosenItems::add)) {
                continue;
            }

            Plan plan = buildPlan(
                recipe,
                gridWidth,
                gridHeight,
                inputGridSlots,
                playerSnapshot,
                sources,
                chosenItems,
                amount,
                inventory
            );
            if (plan == null) {
                continue;
            }
            if (!plan.requiresExactTransaction()) {
                return null;
            }
            return commit(menuAccess, nearbyAccess, inventory, inputGridSlots, playerSnapshot, gridSnapshot, plan);
        }
        return null;
    }

    private static boolean cannotIncrementExistingGrid(List<Slot> slots, int biggest) {
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && Math.min(biggest, stack.getMaxStackSize()) < stack.getCount() + 1) {
                return true;
            }
        }
        return false;
    }

    private static int calculateAmount(boolean useMaxItems, boolean recipeMatches, int biggest, List<Slot> slots) {
        if (useMaxItems) {
            return biggest;
        }
        if (!recipeMatches) {
            return 1;
        }
        int smallest = Integer.MAX_VALUE;
        for (Slot slot : slots) {
            if (!slot.getItem().isEmpty()) {
                smallest = Math.min(smallest, slot.getItem().getCount());
            }
        }
        return smallest == Integer.MAX_VALUE ? 1 : smallest + 1;
    }

    private static int clampToDefaultStackSize(int amount, List<Holder<Item>> chosenItems) {
        int result = amount;
        for (Holder<Item> item : chosenItems) {
            result = Math.min(result, item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1));
        }
        return result;
    }

    @Nullable
    private static Plan buildPlan(
        RecipeHolder<?> recipeHolder,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        List<ItemStack> playerSnapshot,
        List<Source> originalSources,
        List<Holder<Item>> chosenItems,
        int amount,
        Inventory inventory
    ) {
        Recipe<?> recipe = recipeHolder.value();
        List<Source> sources = originalSources.stream().map(Source::copyForPlan).toList();
        List<Target> targets = new ArrayList<>();
        PlaceRecipeHelper.placeRecipe(
            gridWidth,
            gridHeight,
            recipe,
            recipe.placementInfo().slotsToIngredientIndex(),
            (ingredientIndex, slotIndex, x, y) -> {
                if (ingredientIndex != null && ingredientIndex >= 0 && ingredientIndex < chosenItems.size()
                    && slotIndex >= 0 && slotIndex < inputGridSlots.size()) {
                    targets.add(new Target(slotIndex, chosenItems.get(ingredientIndex), amount));
                }
            }
        );
        targets.sort(Comparator.comparingInt(Target::slotIndex));
        if (targets.isEmpty()) {
            return null;
        }

        if (!assignTargets(
            0,
            targets,
            inputGridSlots,
            sources,
            playerSnapshot,
            inventory,
            recipeHolder,
            gridWidth,
            gridHeight,
            new SearchBudget(MAX_COMPONENT_PLAN_BRANCHES)
        )) {
            return null;
        }
        return new Plan(List.copyOf(targets), sources, recipeHolder);
    }

    private static boolean assignTargets(
        int targetIndex,
        List<Target> targets,
        List<Slot> inputGridSlots,
        List<Source> sources,
        List<ItemStack> playerSnapshot,
        Inventory inventory,
        RecipeHolder<?> recipe,
        int gridWidth,
        int gridHeight,
        SearchBudget budget
    ) {
        if (targetIndex >= targets.size()) {
            return canStoreUnusedGridItems(playerSnapshot, sources, inventory)
                && matchesPlannedGrid(recipe, gridWidth, gridHeight, targets, inventory);
        }

        Target target = targets.get(targetIndex);
        for (StackIdentity identity : candidateIdentities(target, inputGridSlots, sources)) {
            if (!budget.tryBranch()) {
                return false;
            }
            target.identity = identity;
            int remaining = target.amount;
            List<Source> ordered = new ArrayList<>(sources);
            ordered.sort(Comparator
                .comparingInt((Source source) -> source.priorityFor(target.slotIndex))
                .thenComparingInt(source -> source.order));
            for (Source source : ordered) {
                if (remaining <= 0) {
                    break;
                }
                if (source.remaining <= 0 || !source.identity.equals(identity)) {
                    continue;
                }
                int taken = (int) Math.min(remaining, source.remaining);
                source.remaining -= taken;
                remaining -= taken;
                target.allocations.add(new Allocation(source, taken));
            }
            if (remaining == 0 && assignTargets(
                targetIndex + 1,
                targets,
                inputGridSlots,
                sources,
                playerSnapshot,
                inventory,
                recipe,
                gridWidth,
                gridHeight,
                budget
            )) {
                return true;
            }
            for (Allocation allocation : target.allocations) {
                allocation.source.remaining += allocation.count;
            }
            target.allocations.clear();
            target.identity = null;
        }
        return false;
    }

    private static List<StackIdentity> candidateIdentities(
        Target target,
        List<Slot> inputGridSlots,
        List<Source> sources
    ) {
        ItemStack existing = inputGridSlots.get(target.slotIndex).getItem();
        StackIdentity existingIdentity = existing.isEmpty() ? null : StackIdentity.of(existing);
        Set<StackIdentity> candidates = new LinkedHashSet<>();
        for (Source source : sources) {
            if (source.remaining > 0 && source.identity.stack().is(target.item)) {
                candidates.add(source.identity);
            }
        }
        return candidates.stream()
            .filter(identity -> identity.stack().getMaxStackSize() >= target.amount)
            .filter(identity -> available(identity, sources) >= target.amount)
            .map(identity -> identityCandidate(identity, target.slotIndex, sources))
            .sorted(Comparator
                .comparing((IdentityCandidate candidate) -> !candidate.identity.equals(existingIdentity))
                .thenComparing(candidate -> !candidate.identity.stack().getComponentsPatch().isEmpty())
                .thenComparingInt(candidate -> candidate.sourcePriority)
                .thenComparingInt(candidate -> candidate.sourceOrder)
                .thenComparing(candidate -> candidate.identity))
            .map(candidate -> candidate.identity)
            .toList();
    }

    private static IdentityCandidate identityCandidate(
        StackIdentity identity,
        int targetSlot,
        List<Source> sources
    ) {
        Source bestSource = sources.stream()
            .filter(source -> source.remaining > 0 && source.identity.equals(identity))
            .min(Comparator
                .comparingInt((Source source) -> source.priorityFor(targetSlot))
                .thenComparingInt(source -> source.order))
            .orElseThrow();
        return new IdentityCandidate(identity, bestSource.priorityFor(targetSlot), bestSource.order);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean matchesPlannedGrid(
        RecipeHolder<?> recipe,
        int gridWidth,
        int gridHeight,
        List<Target> targets,
        Inventory inventory
    ) {
        List<ItemStack> simulatedGrid = new ArrayList<>();
        for (int index = 0; index < gridWidth * gridHeight; index++) {
            simulatedGrid.add(ItemStack.EMPTY);
        }
        for (Target target : targets) {
            simulatedGrid.set(target.slotIndex, target.identity.stack().copyWithCount(target.amount));
        }
        CraftingInput input = CraftingInput.of(gridWidth, gridHeight, simulatedGrid);
        return ((Recipe) recipe.value()).matches(input, inventory.player.level());
    }

    private static long available(StackIdentity identity, List<Source> sources) {
        long total = 0;
        for (Source source : sources) {
            if (source.identity.equals(identity)) {
                total = saturatedAdd(total, source.remaining);
            }
        }
        return total;
    }

    private static boolean canStoreUnusedGridItems(
        List<ItemStack> playerSnapshot,
        List<Source> sources,
        Inventory inventory
    ) {
        List<ItemStack> simulated = copyStacks(playerSnapshot);
        for (Source source : sources) {
            if (source.kind == SourceKind.PLAYER) {
                long used = source.originalAmount - source.remaining;
                if (used > 0) {
                    simulated.get(source.index).shrink((int) used);
                }
            }
        }
        for (Source source : sources) {
            if (source.kind == SourceKind.GRID && source.remaining > 0) {
                ItemStack leftover = source.identity.stack().copyWithCount((int) source.remaining);
                if (!insertIntoPlayer(simulated, leftover, inventory)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static RecipeBookMenu.PostPlaceAction commit(
        ServerPlaceRecipe.CraftingMenuAccess<?> menuAccess,
        NearbyCraftingAccess nearbyAccess,
        Inventory inventory,
        List<Slot> gridSlots,
        List<ItemStack> playerSnapshot,
        List<ItemStack> gridSnapshot,
        Plan plan
    ) {
        List<Extracted> extracted = new ArrayList<>();
        if (!revalidate(inventory, gridSlots, plan.sources)) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }

        try {
            menuAccess.clearCraftingContent();

            for (Source source : plan.sources) {
                int used = (int) (source.originalAmount - source.remaining);
                if (used <= 0 || source.kind != SourceKind.PLAYER) {
                    continue;
                }
                ItemStack removed = inventory.removeItem(source.index, used);
                if (removed.getCount() != used || !source.identity.matches(removed)) {
                    throw new IllegalStateException("Player inventory changed during nearby recipe placement");
                }
            }

            for (Source source : plan.sources) {
                int used = (int) (source.originalAmount - source.remaining);
                if (used <= 0 || source.kind != SourceKind.STORAGE) {
                    continue;
                }
                ItemStack removed = source.storage.extractExact(source.index, source.identity, used);
                if (removed.getCount() != used || !source.identity.matches(removed)) {
                    if (!removed.isEmpty()) {
                        extracted.add(new Extracted(source.storage, source.index, removed.copy()));
                    }
                    throw new IllegalStateException("Nearby storage changed during recipe placement");
                }
                extracted.add(new Extracted(source.storage, source.index, removed.copy()));
            }

            List<ItemStack> livePlayer = inventory.getNonEquipmentItems();
            for (Source source : plan.sources) {
                if (source.kind == SourceKind.GRID && source.remaining > 0) {
                    ItemStack leftover = source.identity.stack().copyWithCount((int) source.remaining);
                    if (!insertIntoPlayer(livePlayer, leftover, inventory)) {
                        throw new IllegalStateException("Validated grid return no longer fits in player inventory");
                    }
                }
            }

            for (Target target : plan.targets) {
                gridSlots.get(target.slotIndex).set(target.identity.stack().copyWithCount(target.amount));
            }
            @SuppressWarnings({"rawtypes", "unchecked"})
            boolean matches = ((ServerPlaceRecipe.CraftingMenuAccess) menuAccess)
                .recipeMatches((RecipeHolder) plan.recipeHolder);
            if (!matches) {
                throw new IllegalStateException("Planned exact-component grid does not match the recipe");
            }

            for (Target target : plan.targets) {
                int nearbyCount = target.allocations.stream()
                    .filter(allocation -> allocation.source.kind == SourceKind.STORAGE)
                    .mapToInt(Allocation::count)
                    .sum();
                int baseline = target.amount - nearbyCount;
                for (Allocation allocation : target.allocations) {
                    if (allocation.source.kind == SourceKind.STORAGE) {
                        nearbyAccess.derk$recordNearbyWithdrawal(
                            allocation.source.storage,
                            allocation.source.index,
                            target.slotIndex,
                            target.identity.stack(),
                            allocation.count,
                            baseline
                        );
                    }
                }
            }
            plan.sources.stream()
                .filter(source -> source.kind == SourceKind.STORAGE && source.originalAmount != source.remaining)
                .map(source -> source.storage)
                .distinct()
                .forEach(NearbyStorage::markChanged);
            inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        } catch (RuntimeException failure) {
            rollback(inventory, gridSlots, playerSnapshot, gridSnapshot, extracted);
            LOGGER.warn("Nearby recipe placement changed during commit; restored captured items", failure);
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
    }

    private static boolean revalidate(Inventory inventory, List<Slot> gridSlots, List<Source> sources) {
        for (Source source : sources) {
            if (source.kind == SourceKind.PLAYER) {
                ItemStack current = inventory.getNonEquipmentItems().get(source.index);
                if (!source.identity.matches(current) || current.getCount() != source.originalAmount) {
                    return false;
                }
            } else if (source.kind == SourceKind.GRID) {
                ItemStack current = gridSlots.get(source.index).getItem();
                if (!source.identity.matches(current) || current.getCount() != source.originalAmount) {
                    return false;
                }
            } else {
                NearbyStorage.SlotSnapshot current = source.storage.snapshot().stream()
                    .filter(snapshot -> snapshot.sourceIndex() == source.index)
                    .findFirst()
                    .orElse(null);
                if (current == null || !source.identity.matches(current.stack()) || current.amount() < source.originalAmount) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void rollback(
        Inventory inventory,
        List<Slot> gridSlots,
        List<ItemStack> playerSnapshot,
        List<ItemStack> gridSnapshot,
        List<Extracted> extracted
    ) {
        List<ItemStack> fallback = new ArrayList<>();
        for (Extracted withdrawal : extracted) {
            ItemStack remainder = withdrawal.stack.copy();
            withdrawal.storage.insertExact(withdrawal.sourceIndex, remainder);
            withdrawal.storage.markChanged();
            if (!remainder.isEmpty()) {
                fallback.add(remainder);
            }
        }
        for (int index = 0; index < playerSnapshot.size(); index++) {
            inventory.getNonEquipmentItems().set(index, playerSnapshot.get(index).copy());
        }
        for (int index = 0; index < gridSnapshot.size(); index++) {
            gridSlots.get(index).set(gridSnapshot.get(index).copy());
        }
        for (ItemStack remainder : fallback) {
            inventory.placeItemBackInInventory(remainder, false);
        }
        inventory.setChanged();
    }

    private static List<Source> snapshotSources(
        List<ItemStack> player,
        List<ItemStack> grid,
        List<NearbyStorage> storages
    ) {
        List<Source> sources = new ArrayList<>();
        int order = 0;
        for (int index = 0; index < grid.size(); index++) {
            ItemStack stack = grid.get(index);
            if (!stack.isEmpty()) {
                sources.add(new Source(SourceKind.GRID, index, null, StackIdentity.of(stack), stack.getCount(), order++));
            }
        }
        for (int index = 0; index < player.size(); index++) {
            ItemStack stack = player.get(index);
            if (!stack.isEmpty()) {
                sources.add(new Source(SourceKind.PLAYER, index, null, StackIdentity.of(stack), stack.getCount(), order++));
            }
        }
        for (NearbyStorage storage : storages) {
            for (NearbyStorage.SlotSnapshot snapshot : storage.snapshot()) {
                sources.add(new Source(
                    SourceKind.STORAGE,
                    snapshot.sourceIndex(),
                    storage,
                    StackIdentity.of(snapshot.stack()),
                    snapshot.amount(),
                    order++
                ));
            }
        }
        return List.copyOf(sources);
    }

    private static StackedItemContents accountSources(List<Source> sources, int gridSlotCount) {
        StackedItemContents contents = new StackedItemContents();
        for (Source source : sources) {
            long remaining = Math.min(source.originalAmount, (long) source.identity.stack().getMaxStackSize() * gridSlotCount);
            while (remaining > 0) {
                int count = (int) Math.min(source.identity.stack().getMaxStackSize(), remaining);
                contents.accountStack(source.identity.stack().copyWithCount(count));
                remaining -= count;
            }
        }
        return contents;
    }

    private static boolean insertIntoPlayer(List<ItemStack> player, ItemStack stack, Inventory inventory) {
        for (ItemStack target : player) {
            if (stack.isEmpty()) {
                return true;
            }
            if (!target.isEmpty() && ItemStack.isSameItemSameComponents(target, stack)) {
                int limit = Math.min(target.getMaxStackSize(), inventory.getMaxStackSize(target));
                int inserted = Math.min(stack.getCount(), Math.max(0, limit - target.getCount()));
                target.grow(inserted);
                stack.shrink(inserted);
            }
        }
        for (int index = 0; index < player.size() && !stack.isEmpty(); index++) {
            if (player.get(index).isEmpty()) {
                int inserted = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack)));
                player.set(index, stack.copyWithCount(inserted));
                stack.shrink(inserted);
            }
        }
        return stack.isEmpty();
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private enum SourceKind {
        GRID,
        PLAYER,
        STORAGE
    }

    private static final class Source {
        private final SourceKind kind;
        private final int index;
        @Nullable
        private final NearbyStorage storage;
        private final StackIdentity identity;
        private final long originalAmount;
        private final int order;
        private long remaining;

        private Source(
            SourceKind kind,
            int index,
            @Nullable NearbyStorage storage,
            StackIdentity identity,
            long amount,
            int order
        ) {
            this.kind = kind;
            this.index = index;
            this.storage = storage;
            this.identity = identity;
            this.originalAmount = amount;
            this.remaining = amount;
            this.order = order;
        }

        private Source copyForPlan() {
            return new Source(kind, index, storage, identity, originalAmount, order);
        }

        private int priorityFor(int targetSlot) {
            if (kind == SourceKind.GRID && index == targetSlot) {
                return 0;
            }
            return switch (kind) {
                case GRID -> 1;
                case PLAYER -> 2;
                case STORAGE -> 3;
            };
        }
    }

    private static final class Target {
        private final int slotIndex;
        private final Holder<Item> item;
        private final int amount;
        private final List<Allocation> allocations = new ArrayList<>();
        private StackIdentity identity;

        private Target(int slotIndex, Holder<Item> item, int amount) {
            this.slotIndex = slotIndex;
            this.item = item;
            this.amount = amount;
        }

        private int slotIndex() {
            return slotIndex;
        }
    }

    private record Allocation(Source source, int count) {
    }

    private record IdentityCandidate(StackIdentity identity, int sourcePriority, int sourceOrder) {
    }

    private static final class Plan {
        private final List<Target> targets;
        private final List<Source> sources;
        private final RecipeHolder<?> recipeHolder;

        private Plan(List<Target> targets, List<Source> sources, RecipeHolder<?> recipeHolder) {
            this.targets = targets;
            this.sources = sources;
            this.recipeHolder = recipeHolder;
        }

        private boolean usesNearbyStorage() {
            return targets.stream().flatMap(target -> target.allocations.stream())
                .anyMatch(allocation -> allocation.source.kind == SourceKind.STORAGE);
        }

        private boolean requiresExactTransaction() {
            return usesNearbyStorage() || targets.stream()
                .anyMatch(target -> !target.identity.stack().getComponentsPatch().isEmpty());
        }
    }

    private record Extracted(NearbyStorage storage, int sourceIndex, ItemStack stack) {
        private Extracted {
            Objects.requireNonNull(storage, "storage");
            stack = stack.copy();
        }
    }

    private static final class SearchBudget {
        private int remaining;

        private SearchBudget(int remaining) {
            this.remaining = remaining;
        }

        private boolean tryBranch() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
