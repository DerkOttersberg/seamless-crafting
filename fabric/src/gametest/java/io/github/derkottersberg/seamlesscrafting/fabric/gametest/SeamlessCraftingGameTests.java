package io.github.derkottersberg.seamlesscrafting.fabric.gametest;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

@SuppressWarnings("removal")
public final class SeamlessCraftingGameTests {
    private static final BlockPos LEFT_CHEST_POS = new BlockPos(1, 1, 1);
    private static final BlockPos RIGHT_CHEST_POS = new BlockPos(2, 1, 1);
    private static final BlockPos BARREL_POS = new BlockPos(1, 1, 1);
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 3);

    @GameTest(maxTicks = 40, padding = 24)
    public void scansDoubleChestOnce(GameTestHelper helper) {
        placeDoubleChest(helper);
        ChestBlockEntity left = helper.getBlockEntity(LEFT_CHEST_POS, ChestBlockEntity.class);
        ChestBlockEntity right = helper.getBlockEntity(RIGHT_CHEST_POS, ChestBlockEntity.class);
        left.setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        right.setItem(0, new ItemStack(Items.OAK_PLANKS, 7));

        Player player = makePlayerNearStorage(helper);
        List<NearbyInventoryScanner.NearbyInventory> scanned = NearbyInventoryScanner.scan(
            helper.getLevel(),
            player.blockPosition(),
            4,
            player
        );

        helper.assertValueEqual(scanned.size(), 1, "A double chest was counted as two nearby inventories");
        helper.assertValueEqual(scanned.getFirst().container().getContainerSize(), 54, "The double chest was not resolved as 54 slots");

        int plankCount = NearbyInventoryScanner.collectItemCounts(
            scanned.stream().map(NearbyInventoryScanner.NearbyInventory::container).toList()
        ).stream()
            .filter(entry -> entry.stack().is(Items.OAK_PLANKS))
            .mapToInt(NearbyInventoryScanner.NearbyItemEntry::count)
            .sum();
        helper.assertValueEqual(plankCount, 12, "Double-chest items were lost or duplicated while scanning");
        helper.assertValueEqual(
            NearbyInventoryScanner.findInventoryPositionsWithItem(
                helper.getLevel(),
                player.blockPosition(),
                4,
                player,
                Items.OAK_PLANKS
            ).size(),
            2,
            "Both halves of the highlighted double chest were not retained"
        );
        helper.succeed();
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void returnsUncraftedNearbyIngredients(GameTestHelper helper) {
        helper.setBlock(BARREL_POS, Blocks.BARREL.defaultBlockState());
        BarrelBlockEntity barrel = helper.getBlockEntity(BARREL_POS, BarrelBlockEntity.class);
        barrel.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));

        Player player = makePlayerNearStorage(helper);
        player.containerMenu = player.inventoryMenu;
        RecipeHolder<?> craftingTableRecipe = helper.getLevel().recipeAccess().getRecipes().stream()
            .filter(recipe -> recipe.id().identifier().getNamespace().equals("minecraft"))
            .filter(recipe -> recipe.id().identifier().getPath().equals("crafting_table"))
            .findFirst()
            .orElseThrow(() -> helper.assertionException("The live crafting-table recipe was not loaded"));

        RecipeBookMenu.PostPlaceAction action = player.inventoryMenu.handlePlacement(
            false,
            false,
            craftingTableRecipe,
            helper.getLevel(),
            player.getInventory()
        );
        helper.assertValueEqual(action, RecipeBookMenu.PostPlaceAction.NOTHING, "Nearby recipe placement failed");
        helper.assertTrue(barrel.getItem(0).isEmpty(), "Nearby planks were not withdrawn from the barrel");
        helper.assertValueEqual(countGridItems(player), 4, "The recipe grid did not receive exactly four planks");

        helper.assertTrue(
            player.inventoryMenu instanceof NearbyCraftingAccess,
            "The server-authoritative crafting mixin was not applied"
        );
        ((NearbyCraftingAccess) player.inventoryMenu).derk$prepareNearbyWithdrawalsForAutofill();

        helper.assertValueEqual(barrel.getItem(0).getCount(), 4, "Cancelled ingredients were not returned to their source");
        helper.assertValueEqual(countGridItems(player), 0, "Cancelled ingredients remained in the crafting grid");
        helper.assertValueEqual(
            countStorageAndGrid(barrel, player),
            4,
            "Cancelling nearby crafting changed the total item count"
        );
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

    private static Player makePlayerNearStorage(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayerInLevel();
        BlockPos absolute = helper.absolutePos(PLAYER_POS);
        player.setPos(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
        return player;
    }

    private static int countGridItems(Player player) {
        return player.inventoryMenu.getInputGridSlots().stream()
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
}
