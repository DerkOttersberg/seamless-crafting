package io.github.derkottersberg.seamlesscrafting.neoforge;

import com.derk.easyinventorycrafter.StackIdentity;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.NearbyStorageContract;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class NeoForgeNearbyStorageScenario {
    private NeoForgeNearbyStorageScenario() {
    }

    public static void verifiesStandardStorageAdapter(GameTestHelper helper) {
        ItemStack enchanted = new ItemStack(Items.OAK_PLANKS, 4);
        enchanted.enchant(
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING),
            1
        );
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(2);
        handler.set(0, ItemResource.of(enchanted), 4);
        NeoForgeNearbyStorage storage = new NeoForgeNearbyStorage(handler, BlockPos.ZERO);

        NearbyStorage.SlotSnapshot source = storage.snapshot().getFirst();
        helper.assertValueEqual(source.amount(), 4L, "NeoForge storage snapshot lost items");
        helper.assertTrue(NearbyStorageContract.isUsable(storage), "NeoForge storage failed reversible-extraction admission");
        helper.assertValueEqual(handler.getAmountAsInt(0), 4, "NeoForge admission mutated storage");
        helper.assertTrue(
            storage.extractExact(source.sourceIndex(), StackIdentity.of(new ItemStack(Items.OAK_PLANKS)), 1).isEmpty(),
            "NeoForge storage ignored exact components"
        );
        helper.assertValueEqual(handler.getAmountAsInt(0), 4, "A rejected NeoForge extraction mutated storage");
        ItemStack removed = storage.extractExact(source.sourceIndex(), StackIdentity.of(enchanted), 4);
        helper.assertValueEqual(removed.getCount(), 4, "NeoForge storage did not extract the planned amount");
        helper.assertTrue(ItemStack.isSameItemSameComponents(removed, enchanted), "NeoForge extraction changed components");
        handler.set(0, ItemResource.of(new ItemStack(Items.STONE)), 64);
        helper.assertValueEqual(storage.insertExact(0, removed), 4, "NeoForge rollback did not fall back to another slot");
        helper.assertTrue(removed.isEmpty(), "NeoForge rollback reported insertion without consuming its input");
        helper.assertTrue(
            ItemStack.isSameItemSameComponents(handler.getResource(1).toStack(), enchanted),
            "NeoForge rollback changed components"
        );
        helper.assertValueEqual(handler.getAmountAsInt(1), 4, "NeoForge extraction/rollback violated conservation");

        BlockPos capabilityPos = new BlockPos(1, 1, 1);
        helper.setBlock(capabilityPos, Blocks.TEST_BLOCK.defaultBlockState());
        BlockPos absoluteCapabilityPos = helper.absolutePos(capabilityPos);
        CapabilityOnlyBlockEntity capabilityOnly = new CapabilityOnlyBlockEntity(
            absoluteCapabilityPos,
            helper.getLevel().getBlockState(absoluteCapabilityPos)
        );
        capabilityOnly.items.set(0, ItemResource.of(enchanted), 4);
        helper.getLevel().setBlockEntity(capabilityOnly);
        Player player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        var discovered = NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player);
        helper.assertValueEqual(discovered.size(), 1, "NeoForge lookup did not discover a capability-only block entity");
        helper.assertValueEqual(
            NearbyInventoryScanner.collectItemCounts(List.of(discovered.getFirst().storage())).getFirst().count(),
            4L,
            "NeoForge capability-only scan lost or duplicated items"
        );
        helper.assertValueEqual(
            capabilityOnly.items.getAmountAsInt(0),
            4,
            "NeoForge capability discovery mutated storage"
        );
        helper.succeed();
    }

    public static final class CapabilityOnlyBlockEntity extends BlockEntity implements ResourceHandler<ItemResource> {
        private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(1);

        private CapabilityOnlyBlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.TEST_BLOCK, pos, state);
        }

        @Override
        public int size() {
            return items.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return items.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return items.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return items.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return items.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return items.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return items.extract(index, resource, amount, transaction);
        }
    }
}
