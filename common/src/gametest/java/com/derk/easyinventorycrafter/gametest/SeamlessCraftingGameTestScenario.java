package com.derk.easyinventorycrafter.gametest;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.NearbyInventoryAccounting;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyRecipePlacementTransaction;
import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.NearbyStorageContract;
import com.derk.easyinventorycrafter.StackIdentity;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.LockCode;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Loader-neutral live scenarios used by Fabric, Forge, and NeoForge. */
public final class SeamlessCraftingGameTestScenario {
    private static java.util.function.Function<GameTestHelper, ServerPlayer> playerFactory = GameTestHelper::makeMockServerPlayerInLevel;

    public static void usePlayerFactory(java.util.function.Function<GameTestHelper, ServerPlayer> factory) {
        playerFactory = java.util.Objects.requireNonNull(factory);
    }

    private static final BlockPos LEFT_CHEST_POS = new BlockPos(1, 1, 1);
    private static final BlockPos RIGHT_CHEST_POS = new BlockPos(2, 1, 1);
    private static final BlockPos BARREL_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 3);

    private SeamlessCraftingGameTestScenario() {
    }

    public static void scansDoubleChestOnce(GameTestHelper helper) {
        useIsolatedScanRadius();
        placeDoubleChest(helper);
        ChestBlockEntity left = (ChestBlockEntity) helper.getBlockEntity(LEFT_CHEST_POS);
        ChestBlockEntity right = (ChestBlockEntity) helper.getBlockEntity(RIGHT_CHEST_POS);
        left.setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        right.setItem(0, new ItemStack(Items.OAK_PLANKS, 7));

        Player player = makePlayerNearStorage(helper);
        List<NearbyInventoryScanner.NearbyInventory> scanned = NearbyInventoryScanner.scan(
            helper.getLevel(), player.blockPosition(), 4, player
        );

        helper.assertTrue(java.util.Objects.equals(scanned.size(), 1), "A double chest was counted as two nearby inventories");
        helper.assertTrue(java.util.Objects.equals(scanned.get(0).container().getContainerSize(), 54), "The double chest was not resolved as 54 slots");

        long plankCount = NearbyInventoryScanner.collectItemCounts(
            scanned.stream().map(NearbyInventoryScanner.NearbyInventory::storage).toList()
        ).stream()
            .filter(entry -> entry.stack().is(Items.OAK_PLANKS))
            .mapToLong(NearbyInventoryScanner.NearbyItemEntry::count)
            .sum();
        helper.assertTrue(java.util.Objects.equals(plankCount, 12L), "Double-chest items were lost or duplicated while scanning");
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.findInventoryPositionsWithItem(
                helper.getLevel(), player.blockPosition(), 4, player, new ItemStack(Items.OAK_PLANKS)
            ).size(), 2), "Both halves of the highlighted double chest were not retained");
        helper.succeed();
    }

    public static void returnsEnchantedIngredientsExactly(GameTestHelper helper) {
        useIsolatedScanRadius();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        ItemStack enchantedPlanks = new ItemStack(Items.OAK_PLANKS, 4);
        enchantedPlanks.enchant(
            Enchantments.UNBREAKING,
            1
        );
        barrel.setItem(0, enchantedPlanks.copy());

        Player player = makePlayerNearStorage(helper);
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(player.inventoryMenu instanceof NearbyCraftingAccess, "The crafting mixin was not applied");
        helper.assertTrue(java.util.Objects.equals(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum(), 0), "The mock player's saved inventory leaked into the scenario");
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 0), "The mock player's crafting grid was not reset");
        Recipe<?> craftingTableRecipe = helper.getLevel().getRecipeManager().getRecipes().stream()
            .filter(recipe -> recipe.getId().getNamespace().equals("minecraft"))
            .filter(recipe -> recipe.getId().getPath().equals("crafting_table"))
            .findFirst()
            .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("The live crafting-table recipe was not loaded"));

        long scannedPlanks = NearbyInventoryScanner.collectItemCounts(
            NearbyInventoryScanner.findNearbyStorages(helper.getLevel(), player.blockPosition(), 4, player)
        ).stream().filter(entry -> entry.stack().is(Items.OAK_PLANKS)).mapToLong(entry -> entry.count()).sum();
        helper.assertTrue(java.util.Objects.equals(scannedPlanks, 4L), "The nearby barrel was not visible to recipe accounting");
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.findInventoryPositionsWithItem(
                helper.getLevel(), player.blockPosition(), 4, player, new ItemStack(Items.OAK_PLANKS)
            ).size(), 0), "Highlight lookup ignored the requested stack components");
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.findInventoryPositionsWithItem(
                helper.getLevel(), player.blockPosition(), 4, player, enchantedPlanks
            ).size(), 1), "Highlight lookup did not find the exact enchanted stack");
        placeRecipe(player, false, craftingTableRecipe);
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 4), "The recipe grid did not receive exactly four planks");
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 0), "Nearby planks were not withdrawn from the barrel");
        helper.assertTrue(
            player.inventoryMenu.slots.subList(1, 5).stream()
                .map(slot -> slot.getItem())
                .filter(stack -> !stack.isEmpty())
                .allMatch(stack -> ItemStack.isSameItemSameTags(stack, enchantedPlanks)),
            "Enchantment components were changed while filling the recipe grid"
        );

        ((NearbyCraftingAccess) player.inventoryMenu).derk$prepareNearbyWithdrawalsForAutofill();

        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 4), "Cancelled ingredients were not returned to their source");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(barrel.getItem(0), enchantedPlanks),
            "Enchantment components were changed during rollback"
        );
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 0), "Cancelled ingredients remained in the crafting grid");
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(barrel, player), 4), "Cancelling changed the total item count");
        helper.succeed();
    }

    public static void craftsMaximumExactComponentsAndReturnsThem(GameTestHelper helper) {
        useIsolatedScanRadius();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        ItemStack enchantedPlanks = enchantedPlanks(helper, 12);
        barrel.setItem(0, enchantedPlanks.copy());

        Player player = makePlayerNearStorage(helper);
        player.containerMenu = player.inventoryMenu;
        placeRecipe(player, true, craftingTableRecipe(helper));

        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 12), "Maximum placement did not fill three complete recipes");
        helper.assertTrue(
            player.inventoryMenu.slots.subList(1, 5).stream()
                .map(slot -> slot.getItem())
                .allMatch(stack -> stack.getCount() == 3 && ItemStack.isSameItemSameTags(stack, enchantedPlanks)),
            "Maximum placement changed counts or exact components"
        );
        helper.assertTrue(barrel.getItem(0).isEmpty(), "Maximum placement did not withdraw all planned items");
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(barrel, player), 12), "Maximum placement violated conservation");

        ((NearbyCraftingAccess) player.inventoryMenu).derk$prepareNearbyWithdrawalsForAutofill();
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 12), "Maximum placement cancellation did not restore storage");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(barrel.getItem(0), enchantedPlanks),
            "Maximum placement cancellation changed components"
        );
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 0), "Maximum placement cancellation left grid items");
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(barrel, player), 12), "Maximum placement rollback violated conservation");
        helper.succeed();
    }

    public static void rollsBackAfterPartialCommitExtraction(GameTestHelper helper) {
        Player player = makePlayerNearStorage(helper);
        player.containerMenu = player.inventoryMenu;
        ItemStack exactPlanks = enchantedPlanks(helper, 1);
        CommitRaceStorage first = new CommitRaceStorage(BlockPos.ZERO, exactPlanks, 2, false);
        CommitRaceStorage second = new CommitRaceStorage(new BlockPos(1, 0, 0), exactPlanks, 2, true);
        @SuppressWarnings("unchecked")
        CraftingRecipe recipe = (CraftingRecipe) (Recipe<?>) craftingTableRecipe(helper);
        RecipeBookMenu<?> menuAccess = player.inventoryMenu;

        NearbyRecipePlacementTransaction.PostPlaceAction action = NearbyRecipePlacementTransaction.tryPlaceWithStoragesForTesting(
            menuAccess,
            2,
            2,
            player.inventoryMenu.slots.subList(1, 5),
            player.getInventory(),
            recipe,
            false,
            List.of(first, second)
        );

        helper.assertTrue(first.committedExtractions > 0, "The first source was not mutated before the forced failure");
        helper.assertTrue(second.committedExtractions > 0, "The partial extraction failure was not exercised");
        helper.assertTrue(java.util.Objects.equals(first.amount, 2), "Rollback did not restore the first committed extraction");
        helper.assertTrue(java.util.Objects.equals(second.amount, 2), "Rollback did not restore the partial extraction");
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 0), "Rollback did not restore the empty grid snapshot");
        helper.assertTrue(java.util.Objects.equals(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum(), 0), "Rollback introduced items into the player snapshot");
        helper.assertTrue(java.util.Objects.equals(first.amount + second.amount + countGridItems(player), 4), "Commit failure violated conservation");

        CommitRaceStorage limitedRestore = new CommitRaceStorage(
            new BlockPos(2, 0, 0),
            exactPlanks,
            4,
            false,
            2
        );
        NearbyRecipePlacementTransaction.PostPlaceAction limitedAction = NearbyRecipePlacementTransaction.tryPlaceWithStoragesForTesting(
            menuAccess,
            2,
            2,
            player.inventoryMenu.slots.subList(1, 5),
            player.getInventory(),
            recipe,
            false,
            List.of(limitedRestore)
        );
        helper.assertTrue(java.util.Objects.equals(limitedAction, NearbyRecipePlacementTransaction.PostPlaceAction.NOTHING), "A non-restorable full extraction did not terminate safely");
        helper.assertTrue(java.util.Objects.equals(limitedRestore.committedExtractions, 0), "A non-restorable planned amount was extracted");
        helper.assertTrue(java.util.Objects.equals(limitedRestore.amount, 4), "Reversible-amount validation mutated storage");
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 0), "A rejected non-restorable plan changed the grid");
        helper.succeed();
    }

    public static void respectsLockedContainers(GameTestHelper helper) {
        useIsolatedScanRadius();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        barrel.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        setLock(barrel, new LockCode("QA diamond key"));
        Player player = makePlayerNearStorage(helper);

        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player).size(), 0), "A locked container was exposed without its key");
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 4), "Locked-container rejection changed stored items");

        ItemStack key = new ItemStack(Items.DIAMOND);
        key.setHoverName(Component.literal("QA diamond key"));
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        List<NearbyInventoryScanner.NearbyInventory> unlocked = NearbyInventoryScanner.scan(
            helper.getLevel(), player.blockPosition(), 4, player
        );
        helper.assertTrue(java.util.Objects.equals(unlocked.size(), 1), "The matching key did not unlock nearby scanning");
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.collectItemCounts(unlocked.stream()
                .map(NearbyInventoryScanner.NearbyInventory::storage)
                .toList()).get(0).count(), 4L), "Unlocked scanning changed or lost stored items");
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 4), "Lock validation violated conservation");
        helper.succeed();
    }

    public static void doesNotLoadChunksWhileScanning(GameTestHelper helper) {
        useIsolatedScanRadius();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        barrel.setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        Player player = makePlayerNearStorage(helper);

        int offset = 4_096;
        BlockPos unloadedCenter = player.blockPosition().offset(offset, 0, offset);
        while (helper.getLevel().isLoaded(unloadedCenter) && offset < 65_536) {
            offset *= 2;
            unloadedCenter = player.blockPosition().offset(offset, 0, offset);
        }
        helper.assertTrue(!helper.getLevel().isLoaded(unloadedCenter), "Could not select an unloaded scan center");
        List<NearbyInventoryScanner.NearbyInventory> scanned = NearbyInventoryScanner.scan(
            helper.getLevel(), unloadedCenter, 4, player
        );
        helper.assertTrue(scanned.isEmpty(), "An unloaded area unexpectedly exposed nearby storage");
        helper.assertTrue(!helper.getLevel().isLoaded(unloadedCenter), "Nearby scanning loaded an unloaded chunk");
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 5), "Unloaded-chunk scanning changed live storage");
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(barrel, player), 5), "Unloaded-chunk scanning violated conservation");
        helper.succeed();
    }

    public static void rejectsIncompletePlacementWithoutMutation(GameTestHelper helper) {
        useIsolatedScanRadius();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        ItemStack enchantedPlanks = enchantedPlanks(helper, 3);
        barrel.setItem(0, enchantedPlanks.copy());

        Player player = makePlayerNearStorage(helper);
        player.containerMenu = player.inventoryMenu;
        Recipe<?> recipe = craftingTableRecipe(helper);
        placeRecipe(player, false, recipe);

        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 3), "An incomplete plan partially mutated nearby storage");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(barrel.getItem(0), enchantedPlanks),
            "An incomplete plan changed source components"
        );
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 0), "An incomplete plan partially mutated the crafting grid");
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(barrel, player), 3), "An incomplete plan violated item conservation");
        helper.succeed();
    }

    public static void choosesMatchingComponentVariantBeforeCommit(GameTestHelper helper) {
        useIsolatedScanRadius();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        ItemStack plainPlanks = new ItemStack(Items.OAK_PLANKS, 4);
        ItemStack enchantedPlanks = enchantedPlanks(helper, 4);
        barrel.setItem(0, plainPlanks.copy());
        barrel.setItem(1, enchantedPlanks.copy());

        Player player = makePlayerNearStorage(helper);
        player.containerMenu = player.inventoryMenu;
        Recipe<?> loaded = craftingTableRecipe(helper);
        CraftingRecipe exactRecipe = constrainedRecipe(
            loaded,
            input -> gridItems(input).stream().filter(stack -> !stack.isEmpty()).allMatch(ItemStack::isEnchanted)
        );

        placeRecipe(player, false, exactRecipe);

        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 4), "The preferred plain variant was mutated before validation");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(barrel.getItem(0), plainPlanks),
            "The rejected component variant changed"
        );
        helper.assertTrue(barrel.getItem(1).isEmpty(), "The validated enchanted variant was not withdrawn");
        helper.assertTrue(
            player.inventoryMenu.slots.subList(1, 5).stream()
                .map(slot -> slot.getItem())
                .filter(stack -> !stack.isEmpty())
                .allMatch(stack -> ItemStack.isSameItemSameTags(stack, enchantedPlanks)),
            "The planner did not retry the exact component variant selected by the recipe matcher"
        );

        ((NearbyCraftingAccess) player.inventoryMenu).derk$prepareNearbyWithdrawalsForAutofill();
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(0).getCount(), 4), "Rollback changed the rejected plain variant");
        helper.assertTrue(java.util.Objects.equals(barrel.getItem(1).getCount(), 4), "Rollback did not restore the selected enchanted variant");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(barrel.getItem(1), enchantedPlanks),
            "Rollback changed the selected variant's components"
        );
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(barrel, player), 8), "Variant backtracking violated conservation");

        helper.setBlock(BARREL_POS, Blocks.AIR.defaultBlockState());
        player.getInventory().items.set(0, enchantedPlanks.copy());
        placeRecipe(player, false, exactRecipe);
        helper.assertTrue(java.util.Objects.equals(countGridItems(player), 4), "Player-only autofill did not fill the complete grid");
        helper.assertTrue(
            player.inventoryMenu.slots.subList(1, 5).stream()
                .map(slot -> slot.getItem())
                .filter(stack -> !stack.isEmpty())
                .allMatch(stack -> ItemStack.isSameItemSameTags(stack, enchantedPlanks)),
            "Player-only autofill changed enchantment components"
        );
        helper.assertTrue(java.util.Objects.equals(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum() + countGridItems(player), 4), "Player-only exact placement violated conservation");

        player.inventoryMenu.slots.subList(1, 5).forEach(slot -> slot.set(ItemStack.EMPTY));
        player.getInventory().clearContent();
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity priorityBarrel = (BarrelBlockEntity) helper.getBlockEntity(BARREL_POS);
        ItemStack storageVariant = enchantedPlanks.copy();
        ItemStack playerPreferredVariant = enchantedPlanks.copy();
        playerPreferredVariant.setHoverName(Component.literal("zz-player-variant"));
        priorityBarrel.setItem(0, storageVariant.copy());
        player.getInventory().items.set(0, playerPreferredVariant.copy());

        placeRecipe(player, false, exactRecipe);
        helper.assertTrue(java.util.Objects.equals(priorityBarrel.getItem(0).getCount(), 4), "A storage-only modified identity was consumed before the player variant");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(priorityBarrel.getItem(0), storageVariant),
            "Source-priority planning changed the untouched storage variant"
        );
        helper.assertTrue(java.util.Objects.equals(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum(), 0), "Source-priority planning did not consume the player variant");
        helper.assertTrue(
            player.inventoryMenu.slots.subList(1, 5).stream()
                .map(slot -> slot.getItem())
                .filter(stack -> !stack.isEmpty())
                .allMatch(stack -> ItemStack.isSameItemSameTags(stack, playerPreferredVariant)),
            "Source-priority planning selected a naturally earlier storage identity"
        );
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(priorityBarrel, player), 8), "Player-before-storage identity selection violated conservation");

        player.inventoryMenu.slots.subList(1, 5).forEach(slot -> slot.set(ItemStack.EMPTY));
        player.getInventory().clearContent();
        ItemStack oakCandidate = enchantedPlanks(helper, 4);
        ItemStack birchCandidate = new ItemStack(Items.BIRCH_PLANKS, 4);
        birchCandidate.enchant(
            Enchantments.UNBREAKING,
            1
        );
        priorityBarrel.setItem(0, oakCandidate.copy());
        priorityBarrel.setItem(1, birchCandidate.copy());
        CraftingRecipe birchOnlyRecipe = constrainedRecipe(
            loaded,
            input -> gridItems(input).stream()
                .filter(stack -> !stack.isEmpty())
                .allMatch(stack -> stack.is(Items.BIRCH_PLANKS) && stack.isEnchanted())
        );
        placeRecipe(player, false, birchOnlyRecipe);
        helper.assertTrue(java.util.Objects.equals(priorityBarrel.getItem(0).getCount(), 4), "The rejected oak item candidate was consumed");
        helper.assertTrue(priorityBarrel.getItem(1).isEmpty(), "The recipe-valid birch item candidate was not consumed");
        helper.assertTrue(
            player.inventoryMenu.slots.subList(1, 5).stream()
                .map(slot -> slot.getItem())
                .filter(stack -> !stack.isEmpty())
                .allMatch(stack -> ItemStack.isSameItemSameTags(stack, birchCandidate)),
            "Whole-grid planning did not backtrack across ingredient item choices"
        );
        ((NearbyCraftingAccess) player.inventoryMenu).derk$prepareNearbyWithdrawalsForAutofill();
        helper.assertTrue(java.util.Objects.equals(priorityBarrel.getItem(1).getCount(), 4), "Alternate item rollback did not restore storage");
        helper.assertTrue(java.util.Objects.equals(countStorageAndGrid(priorityBarrel, player), 8), "Alternate item planning violated conservation");
        helper.succeed();
    }

    public static void preservesPacketBoundsAndStackIdentity(GameTestHelper helper) {
        ItemStack enchanted = enchantedPlanks(helper, 1);
        ItemStack sameWithDifferentCount = enchanted.copyWithCount(32);
        ItemStack plain = new ItemStack(Items.OAK_PLANKS);
        StackIdentity first = StackIdentity.of(enchanted);
        StackIdentity second = StackIdentity.of(sameWithDifferentCount);
        helper.assertTrue(first.equals(second), "Stack identity incorrectly included the count");
        helper.assertTrue(java.util.Objects.equals(first.hashCode(), second.hashCode()), "Equal stack identities have different hashes");
        helper.assertTrue(!first.equals(StackIdentity.of(plain)), "Stack identity ignored enchantment components");

        ItemStack named = plain.copy();
        named.setHoverName(Component.literal("deterministic"));
        List<StackIdentity> encounterOrder = List.of(StackIdentity.of(named), StackIdentity.of(plain), first);
        List<StackIdentity> accountedOrder = List.copyOf(NearbyInventoryAccounting.totalCounts(List.of(List.of(
            new NearbyInventoryAccounting.Counted<>(encounterOrder.get(0), 1),
            new NearbyInventoryAccounting.Counted<>(encounterOrder.get(1), 1),
            new NearbyInventoryAccounting.Counted<>(encounterOrder.get(2), 1),
            new NearbyInventoryAccounting.Counted<>(StackIdentity.of(named.copyWithCount(8)), 2)
        ))).keySet());
        helper.assertTrue(java.util.Objects.equals(accountedOrder, encounterOrder), "Exact accounting lost stable source encounter order");
        ItemStack firstInsertionOrder = new ItemStack(Items.WOODEN_SWORD);
        firstInsertionOrder.setHoverName(Component.literal("ordered"));
        firstInsertionOrder.setDamageValue(1);
        ItemStack secondInsertionOrder = new ItemStack(Items.WOODEN_SWORD);
        secondInsertionOrder.setDamageValue(1);
        secondInsertionOrder.setHoverName(Component.literal("ordered"));
        StackIdentity firstOrdered = StackIdentity.of(firstInsertionOrder);
        StackIdentity secondOrdered = StackIdentity.of(secondInsertionOrder);
        helper.assertTrue(firstOrdered.equals(secondOrdered), "Component insertion order changed exact identity");
        helper.assertTrue(java.util.Objects.equals(firstOrdered.hashCode(), secondOrdered.hashCode()), "Equal component patches hashed differently");

        long largeCount = (1L << 40) + 123;
        NearbyItemsPacket packet = new NearbyItemsPacket(
            List.of(new NearbyInventoryScanner.NearbyItemEntry(enchanted, largeCount)),
            List.of(enchanted),
            true
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        NearbyItemsPacket.STREAM_CODEC.encode(buffer, packet);
        NearbyItemsPacket decoded = NearbyItemsPacket.STREAM_CODEC.decode(buffer);
        buffer.release();
        helper.assertTrue(java.util.Objects.equals(decoded.entries().get(0).count(), largeCount), "The VarLong count did not round-trip");
        helper.assertTrue(decoded.truncated(), "The packet truncation flag did not round-trip");
        helper.assertTrue(
            ItemStack.isSameItemSameTags(decoded.entries().get(0).stack(), enchanted),
            "Packet encoding changed item components"
        );

        List<NearbyStorage.SlotSnapshot> excessive = new ArrayList<>();
        for (int index = 0; index <= NearbyInventoryScanner.MAX_ENTRIES; index++) {
            ItemStack unique = new ItemStack(Items.STONE);
            unique.setHoverName(Component.literal("bounded-" + index));
            long amount = index == 0 ? NearbyInventoryScanner.MAX_REPORTED_COUNT + 1 : 1;
            excessive.add(new NearbyStorage.SlotSnapshot(index, unique, amount));
        }
        NearbyStorage synthetic = new NearbyStorage() {
            @Override
            public BlockPos key() {
                return BlockPos.ZERO;
            }

            @Override
            public List<BlockPos> positions() {
                return List.of(BlockPos.ZERO);
            }

            @Override
            public List<SlotSnapshot> snapshot() {
                return excessive;
            }

            @Override
            public ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount) {
                return ItemStack.EMPTY;
            }

            @Override
            public int insertExact(int preferredIndex, ItemStack stack) {
                return 0;
            }
        };
        NearbyInventoryScanner.NearbyItemsSnapshot bounded = NearbyInventoryScanner.collectNearbyItems(List.of(synthetic));
        helper.assertTrue(java.util.Objects.equals(bounded.entries().size(), NearbyInventoryScanner.MAX_ENTRIES), "The scanner exceeded its entry bound");
        helper.assertTrue(bounded.truncated(), "A bounded scanner result did not disclose truncation");
        StackIdentity firstExcessiveIdentity = StackIdentity.of(excessive.get(0).stack());
        long boundedLargeCount = bounded.entries().stream()
            .filter(entry -> firstExcessiveIdentity.matches(entry.stack()))
            .mapToLong(NearbyInventoryScanner.NearbyItemEntry::count)
            .findFirst()
            .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("The bounded count entry was omitted"));
        helper.assertTrue(java.util.Objects.equals(boundedLargeCount, NearbyInventoryScanner.MAX_REPORTED_COUNT), "The scanner did not conservatively bound a large count");
        helper.succeed();
    }

    public static void rejectsBrokenStorageContractsAndDeduplicates(GameTestHelper helper) {
        Object sharedBacking = new Object();
        ContractStorage first = new ContractStorage(
            sharedBacking,
            BlockPos.ZERO,
            List.of(BlockPos.ZERO),
            ContractBehavior.HEALTHY
        );
        ContractStorage duplicateView = new ContractStorage(
            sharedBacking,
            new BlockPos(1, 0, 0),
            List.of(new BlockPos(1, 0, 0)),
            ContractBehavior.HEALTHY
        );
        ContractStorage ambiguous = new ContractStorage(
            new Object(),
            new BlockPos(2, 0, 0),
            List.of(new BlockPos(2, 0, 0)),
            ContractBehavior.AMBIGUOUS_INDEX
        );
        ContractStorage readOnly = new ContractStorage(
            new Object(),
            new BlockPos(3, 0, 0),
            List.of(new BlockPos(3, 0, 0)),
            ContractBehavior.READ_ONLY
        );
        ContractStorage wrongIdentity = new ContractStorage(
            new Object(),
            new BlockPos(4, 0, 0),
            List.of(new BlockPos(4, 0, 0)),
            ContractBehavior.WRONG_IDENTITY
        );
        ContractStorage partialExtraction = new ContractStorage(
            new Object(),
            new BlockPos(5, 0, 0),
            List.of(new BlockPos(5, 0, 0)),
            ContractBehavior.PARTIAL_EXTRACTION
        );

        helper.assertTrue(NearbyStorageContract.isUsable(first), "A reversible exact storage was rejected");
        helper.assertTrue(!NearbyStorageContract.isUsable(ambiguous), "Ambiguous source indices were accepted");
        helper.assertTrue(!NearbyStorageContract.isUsable(readOnly), "A read-only storage was accepted");
        helper.assertTrue(!NearbyStorageContract.isUsable(wrongIdentity), "A wrong-identity extraction was accepted");
        helper.assertTrue(!NearbyStorageContract.isUsable(partialExtraction), "A partial extraction was accepted");

        List<NearbyInventoryScanner.NearbyInventory> deduplicated = NearbyInventoryScanner.deduplicateStorages(List.of(
            new NearbyInventoryScanner.NearbyInventory(first, first.key(), first.positions()),
            new NearbyInventoryScanner.NearbyInventory(duplicateView, duplicateView.key(), duplicateView.positions()),
            new NearbyInventoryScanner.NearbyInventory(ambiguous, ambiguous.key(), ambiguous.positions()),
            new NearbyInventoryScanner.NearbyInventory(readOnly, readOnly.key(), readOnly.positions()),
            new NearbyInventoryScanner.NearbyInventory(wrongIdentity, wrongIdentity.key(), wrongIdentity.positions()),
            new NearbyInventoryScanner.NearbyInventory(
                partialExtraction,
                partialExtraction.key(),
                partialExtraction.positions()
            )
        ));
        helper.assertTrue(java.util.Objects.equals(deduplicated.size(), 1), "Invalid or duplicate storage views survived admission");
        helper.assertTrue(java.util.Objects.equals(deduplicated.get(0).positions().size(), 2), "Deduplication discarded one physical storage position");
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.collectItemCounts(List.of(deduplicated.get(0).storage())).get(0).count(), 4L), "A duplicate view doubled the admitted storage count");
        helper.assertTrue(java.util.Objects.equals(first.mutationCount, 0), "Storage admission or deduplication mutated live contents");
        helper.assertTrue(java.util.Objects.equals(duplicateView.mutationCount, 0), "A duplicate storage probe mutated live contents");
        helper.succeed();
    }

    private static void placeDoubleChest(GameTestHelper helper) {
        var rightState = Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, Direction.NORTH)
            .setValue(ChestBlock.TYPE, ChestType.RIGHT);
        var leftState = Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, Direction.NORTH)
            .setValue(ChestBlock.TYPE, ChestType.LEFT);
        helper.setBlock(RIGHT_CHEST_POS, rightState);
        helper.setBlock(LEFT_CHEST_POS, leftState);
    }

    private static ItemStack enchantedPlanks(GameTestHelper helper, int count) {
        ItemStack stack = new ItemStack(Items.OAK_PLANKS, count);
        stack.enchant(
            Enchantments.UNBREAKING,
            1
        );
        return stack;
    }

    private static Recipe<?> craftingTableRecipe(GameTestHelper helper) {
        return helper.getLevel().getRecipeManager().getRecipes().stream()
            .filter(recipe -> recipe.getId().getNamespace().equals("minecraft"))
            .filter(recipe -> recipe.getId().getPath().equals("crafting_table"))
            .findFirst()
            .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("The live crafting-table recipe was not loaded"));
    }

    private static CraftingRecipe constrainedRecipe(Recipe<?> loaded, Predicate<CraftingContainer> constraint) {
        CraftingRecipe delegate = (CraftingRecipe) loaded;
        return new CraftingRecipe() {
            @Override public boolean matches(CraftingContainer input, Level level) { return delegate.matches(input, level) && constraint.test(input); }
            @Override public ItemStack assemble(CraftingContainer input, RegistryAccess access) { return delegate.assemble(input, access); }
            @Override public boolean canCraftInDimensions(int width, int height) { return delegate.canCraftInDimensions(width, height); }
            @Override public ItemStack getResultItem(RegistryAccess access) { return delegate.getResultItem(access); }
            @Override public ResourceLocation getId() { return delegate.getId(); }
            @Override public NonNullList<Ingredient> getIngredients() { return delegate.getIngredients(); }
            @Override public RecipeSerializer<?> getSerializer() { return delegate.getSerializer(); }
            @Override public String getGroup() { return delegate.getGroup(); }
            @Override public CraftingBookCategory category() { return delegate.category(); }
        };
    }

    private static void placeRecipe(Player player, boolean useMaxItems, Recipe<?> recipe) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        serverPlayer.awardRecipes(List.of(recipe));
        player.inventoryMenu.handlePlacement(useMaxItems, recipe, serverPlayer);
    }

    public static List<ItemStack> gridItems(CraftingContainer input) {
        return java.util.stream.IntStream.range(0, input.getContainerSize()).mapToObj(input::getItem).toList();
    }

    private static void useIsolatedScanRadius() {
        EasyInventoryCrafterConfig.ConfigData config = EasyInventoryCrafterConfig.snapshot();
        if (config.nearbyRadius != 4) {
            config.nearbyRadius = 4;
            EasyInventoryCrafterConfig.update(config);
        }
    }

    private static Player makePlayerNearStorage(GameTestHelper helper) {
        Player player = playerFactory.apply(helper);
        // GameTest servers reuse their development world between invocations,
        // including the fixed mock player's saved inventory. Start every
        // scenario from a deterministic empty player state.
        player.getInventory().clearContent();
        player.inventoryMenu.slots.subList(1, 5).forEach(slot -> slot.set(ItemStack.EMPTY));
        BlockPos absolute = helper.absolutePos(PLAYER_POS);
        player.setPos(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
        return player;
    }

    private static int countGridItems(Player player) {
        return player.inventoryMenu.slots.subList(1, 5).stream()
            .map(slot -> slot.getItem().getCount())
            .mapToInt(Integer::intValue)
            .sum();
    }

    private static int countStorageAndGrid(Container storage, Player player) {
        int total = countGridItems(player);
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            total += storage.getItem(slot).getCount();
        }
        return total;
    }

    private static void setLock(BaseContainerBlockEntity container, LockCode lock) {
        try {
            var field = BaseContainerBlockEntity.class.getDeclaredField("lockKey");
            field.setAccessible(true);
            field.set(container, lock);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not configure the live locked-container scenario", exception);
        }
    }

    private static final class CommitRaceStorage implements NearbyStorage {
        private final BlockPos key;
        private final ItemStack template;
        private final boolean returnPartialOnCommit;
        private final int maxRestorable;
        private int amount;
        private int committedExtractions;

        private CommitRaceStorage(BlockPos key, ItemStack template, int amount, boolean returnPartialOnCommit) {
            this(key, template, amount, returnPartialOnCommit, Integer.MAX_VALUE);
        }

        private CommitRaceStorage(
            BlockPos key,
            ItemStack template,
            int amount,
            boolean returnPartialOnCommit,
            int maxRestorable
        ) {
            this.key = key.immutable();
            this.template = template.copyWithCount(1);
            this.amount = amount;
            this.returnPartialOnCommit = returnPartialOnCommit;
            this.maxRestorable = maxRestorable;
        }

        @Override
        public BlockPos key() {
            return key;
        }

        @Override
        public List<BlockPos> positions() {
            return List.of(key);
        }

        @Override
        public List<SlotSnapshot> snapshot() {
            return amount <= 0 ? List.of() : List.of(new SlotSnapshot(0, template, amount));
        }

        @Override
        public ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int requested) {
            if (sourceIndex != 0 || requested <= 0 || amount < requested || !expected.matches(template)) {
                return ItemStack.EMPTY;
            }
            return template.copyWithCount(requested);
        }

        @Override
        public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int requested) {
            return sourceIndex == 0
                && requested > 0
                && requested <= maxRestorable
                && amount >= requested
                && expected.matches(template);
        }

        @Override
        public ItemStack extractExact(int sourceIndex, StackIdentity expected, int requested) {
            if (simulateExtractExact(sourceIndex, expected, requested).isEmpty()) {
                return ItemStack.EMPTY;
            }
            int extracted = returnPartialOnCommit ? Math.max(1, requested - 1) : requested;
            amount -= extracted;
            committedExtractions++;
            return template.copyWithCount(extracted);
        }

        @Override
        public int insertExact(int preferredIndex, ItemStack stack) {
            if (stack == null || stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, template)) {
                return 0;
            }
            int inserted = stack.getCount();
            amount += inserted;
            stack.shrink(inserted);
            return inserted;
        }
    }

    private enum ContractBehavior {
        HEALTHY,
        AMBIGUOUS_INDEX,
        READ_ONLY,
        WRONG_IDENTITY,
        PARTIAL_EXTRACTION
    }

    private static final class ContractStorage implements NearbyStorage {
        private final Object backing;
        private final BlockPos key;
        private final List<BlockPos> positions;
        private final ContractBehavior behavior;
        private final ItemStack contents = new ItemStack(Items.STONE, 4);
        private int mutationCount;

        private ContractStorage(Object backing, BlockPos key, List<BlockPos> positions, ContractBehavior behavior) {
            this.backing = backing;
            this.key = key;
            this.positions = List.copyOf(positions);
            this.behavior = behavior;
        }

        @Override
        public Object identityKey() {
            return backing;
        }

        @Override
        public BlockPos key() {
            return key;
        }

        @Override
        public List<BlockPos> positions() {
            return positions;
        }

        @Override
        public List<SlotSnapshot> snapshot() {
            if (behavior == ContractBehavior.AMBIGUOUS_INDEX) {
                return List.of(
                    new SlotSnapshot(0, contents, 2),
                    new SlotSnapshot(0, new ItemStack(Items.DIRT), 1)
                );
            }
            return List.of(new SlotSnapshot(0, contents, contents.getCount()));
        }

        @Override
        public ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int amount) {
            if (sourceIndex != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            if (behavior == ContractBehavior.WRONG_IDENTITY) {
                return new ItemStack(Items.DIRT, amount);
            }
            int extracted = behavior == ContractBehavior.PARTIAL_EXTRACTION ? Math.max(0, amount - 1) : amount;
            return extracted == 0 ? ItemStack.EMPTY : contents.copyWithCount(extracted);
        }

        @Override
        public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
            return behavior != ContractBehavior.READ_ONLY;
        }

        @Override
        public ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount) {
            mutationCount++;
            return ItemStack.EMPTY;
        }

        @Override
        public int insertExact(int preferredIndex, ItemStack stack) {
            mutationCount++;
            return 0;
        }
    }
}
