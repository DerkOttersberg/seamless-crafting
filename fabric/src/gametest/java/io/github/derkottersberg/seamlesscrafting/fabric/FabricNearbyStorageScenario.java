package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.StackIdentity;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.NearbyStorageContract;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricNearbyStorageScenario {
    private FabricNearbyStorageScenario() {
    }

    public static void verifiesStandardStorageAdapter(GameTestHelper helper) {
        ItemStack enchanted = new ItemStack(Items.OAK_PLANKS, 4);
        enchanted.enchant(
            Enchantments.UNBREAKING,
            1
        );
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, enchanted.copy());
        FabricNearbyStorage storage = new FabricNearbyStorage(InventoryStorage.of(container, null), BlockPos.ZERO);

        NearbyStorage.SlotSnapshot source = storage.snapshot().get(0);
        helper.assertTrue(java.util.Objects.equals(source.amount(), 4L), "Fabric storage snapshot lost items");
        helper.assertTrue(NearbyStorageContract.isUsable(storage), "Fabric storage failed reversible-extraction admission");
        helper.assertTrue(java.util.Objects.equals(container.getItem(0).getCount(), 4), "Fabric admission mutated storage");
        helper.assertTrue(
            storage.extractExact(source.sourceIndex(), StackIdentity.of(new ItemStack(Items.OAK_PLANKS)), 1).isEmpty(),
            "Fabric storage ignored exact components"
        );
        helper.assertTrue(java.util.Objects.equals(container.getItem(0).getCount(), 4), "A rejected Fabric extraction mutated storage");
        ItemStack removed = storage.extractExact(source.sourceIndex(), StackIdentity.of(enchanted), 4);
        helper.assertTrue(java.util.Objects.equals(removed.getCount(), 4), "Fabric storage did not extract the planned amount");
        helper.assertTrue(ItemStack.isSameItemSameTags(removed, enchanted), "Fabric extraction changed components");
        container.setItem(0, new ItemStack(Items.STONE, 64));
        helper.assertTrue(java.util.Objects.equals(storage.insertExact(0, removed), 4), "Fabric rollback did not fall back to another slot");
        helper.assertTrue(removed.isEmpty(), "Fabric rollback reported insertion without consuming its input");
        helper.assertTrue(ItemStack.isSameItemSameTags(container.getItem(1), enchanted), "Fabric rollback changed components");
        helper.assertTrue(java.util.Objects.equals(container.getItem(1).getCount(), 4), "Fabric extraction/rollback violated conservation");

        SimpleContainer aggregateContainer = new SimpleContainer(3);
        aggregateContainer.setItem(0, enchanted.copyWithCount(1));
        aggregateContainer.setItem(1, enchanted.copyWithCount(2));
        aggregateContainer.setItem(2, new ItemStack(Items.DIRT, 3));
        FabricNearbyStorage aggregate = new FabricNearbyStorage(
            InventoryStorage.of(aggregateContainer, null),
            new BlockPos(1, 0, 0)
        );
        NearbyStorage.SlotSnapshot enchantedSource = aggregate.snapshot().stream()
            .filter(snapshot -> StackIdentity.of(enchanted).matches(snapshot.stack()))
            .findFirst()
            .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("Fabric identity snapshot omitted enchanted views"));
        helper.assertTrue(java.util.Objects.equals(enchantedSource.amount(), 3L), "Fabric views with one identity were not aggregated");
        int stableSourceIndex = enchantedSource.sourceIndex();
        helper.assertTrue(
            aggregate.extractExact(stableSourceIndex, StackIdentity.of(new ItemStack(Items.DIRT)), 1).isEmpty(),
            "Fabric source index accepted a different identity"
        );
        helper.assertTrue(java.util.Objects.equals(aggregate.snapshot().stream()
                .filter(snapshot -> StackIdentity.of(enchanted).matches(snapshot.stack()))
                .findFirst()
                .orElseThrow()
                .sourceIndex(), stableSourceIndex), "Fabric source indexing changed between snapshots");

        BlockPos capabilityPos = new BlockPos(1, 1, 1);
        helper.setBlock(capabilityPos, Blocks.CHEST.defaultBlockState());
        BlockPos absoluteCapabilityPos = helper.absolutePos(capabilityPos);
        CapabilityOnlyBlockEntity capabilityOnly = new CapabilityOnlyBlockEntity(
            absoluteCapabilityPos,
            helper.getLevel().getBlockState(absoluteCapabilityPos)
        );
        capabilityOnly.items.setItem(0, enchanted.copy());
        helper.getLevel().setBlockEntity(capabilityOnly);
        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        var discovered = NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player);
        helper.assertTrue(java.util.Objects.equals(discovered.size(), 1), "Fabric lookup did not discover a capability-only block entity");
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.collectItemCounts(List.of(discovered.get(0).storage())).get(0).count(), 4L), "Fabric capability-only scan lost or duplicated items");
        helper.assertTrue(java.util.Objects.equals(capabilityOnly.items.getItem(0).getCount(), 4), "Fabric capability discovery mutated storage");
        helper.succeed();
    }

    private static final class CapabilityOnlyBlockEntity extends BlockEntity implements SidedStorageBlockEntity {
        private final SimpleContainer items = new SimpleContainer(1);
        private final Storage<ItemVariant> storage = InventoryStorage.of(items, null);

        private CapabilityOnlyBlockEntity(BlockPos pos, BlockState state) {
            super(net.minecraft.world.level.block.entity.BlockEntityType.CHEST, pos, state);
        }

        @Override
        public Storage<ItemVariant> getItemStorage(Direction side) {
            return storage;
        }
    }
}
