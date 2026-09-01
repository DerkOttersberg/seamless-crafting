package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.StackIdentity;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.NearbyStorageContract;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
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
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricNearbyStorageScenario {
    private FabricNearbyStorageScenario() {
    }

    public static void verifiesStandardStorageAdapter(GameTestHelper helper) {
        ItemStack enchanted = new ItemStack(Items.OAK_PLANKS, 4);
        enchanted.enchant(
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING),
            1
        );
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, enchanted.copy());
        FabricNearbyStorage storage = new FabricNearbyStorage(ContainerStorage.of(container, null), BlockPos.ZERO);

        NearbyStorage.SlotSnapshot source = storage.snapshot().getFirst();
        helper.assertValueEqual(source.amount(), 4L, "Fabric storage snapshot lost items");
        helper.assertTrue(NearbyStorageContract.isUsable(storage), "Fabric storage failed reversible-extraction admission");
        helper.assertValueEqual(container.getItem(0).getCount(), 4, "Fabric admission mutated storage");
        helper.assertTrue(
            storage.extractExact(source.sourceIndex(), StackIdentity.of(new ItemStack(Items.OAK_PLANKS)), 1).isEmpty(),
            "Fabric storage ignored exact components"
        );
        helper.assertValueEqual(container.getItem(0).getCount(), 4, "A rejected Fabric extraction mutated storage");
        ItemStack removed = storage.extractExact(source.sourceIndex(), StackIdentity.of(enchanted), 4);
        helper.assertValueEqual(removed.getCount(), 4, "Fabric storage did not extract the planned amount");
        helper.assertTrue(ItemStack.isSameItemSameComponents(removed, enchanted), "Fabric extraction changed components");
        container.setItem(0, new ItemStack(Items.STONE, 64));
        helper.assertValueEqual(storage.insertExact(0, removed), 4, "Fabric rollback did not fall back to another slot");
        helper.assertTrue(removed.isEmpty(), "Fabric rollback reported insertion without consuming its input");
        helper.assertTrue(ItemStack.isSameItemSameComponents(container.getItem(1), enchanted), "Fabric rollback changed components");
        helper.assertValueEqual(container.getItem(1).getCount(), 4, "Fabric extraction/rollback violated conservation");

        SimpleContainer aggregateContainer = new SimpleContainer(3);
        aggregateContainer.setItem(0, enchanted.copyWithCount(1));
        aggregateContainer.setItem(1, enchanted.copyWithCount(2));
        aggregateContainer.setItem(2, new ItemStack(Items.DIRT, 3));
        FabricNearbyStorage aggregate = new FabricNearbyStorage(
            ContainerStorage.of(aggregateContainer, null),
            new BlockPos(1, 0, 0)
        );
        NearbyStorage.SlotSnapshot enchantedSource = aggregate.snapshot().stream()
            .filter(snapshot -> StackIdentity.of(enchanted).matches(snapshot.stack()))
            .findFirst()
            .orElseThrow(() -> helper.assertionException("Fabric identity snapshot omitted enchanted views"));
        helper.assertValueEqual(enchantedSource.amount(), 3L, "Fabric views with one identity were not aggregated");
        int stableSourceIndex = enchantedSource.sourceIndex();
        helper.assertTrue(
            aggregate.extractExact(stableSourceIndex, StackIdentity.of(new ItemStack(Items.DIRT)), 1).isEmpty(),
            "Fabric source index accepted a different identity"
        );
        helper.assertValueEqual(
            aggregate.snapshot().stream()
                .filter(snapshot -> StackIdentity.of(enchanted).matches(snapshot.stack()))
                .findFirst()
                .orElseThrow()
                .sourceIndex(),
            stableSourceIndex,
            "Fabric source indexing changed between snapshots"
        );

        BlockPos capabilityPos = new BlockPos(1, 1, 1);
        helper.setBlock(capabilityPos, Blocks.TEST_BLOCK.defaultBlockState());
        BlockPos absoluteCapabilityPos = helper.absolutePos(capabilityPos);
        CapabilityOnlyBlockEntity capabilityOnly = new CapabilityOnlyBlockEntity(
            absoluteCapabilityPos,
            helper.getLevel().getBlockState(absoluteCapabilityPos)
        );
        capabilityOnly.items.setItem(0, enchanted.copy());
        helper.getLevel().setBlockEntity(capabilityOnly);
        Player player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        var discovered = NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player);
        helper.assertValueEqual(discovered.size(), 1, "Fabric lookup did not discover a capability-only block entity");
        helper.assertValueEqual(
            NearbyInventoryScanner.collectItemCounts(List.of(discovered.getFirst().storage())).getFirst().count(),
            4L,
            "Fabric capability-only scan lost or duplicated items"
        );
        helper.assertValueEqual(capabilityOnly.items.getItem(0).getCount(), 4, "Fabric capability discovery mutated storage");
        helper.succeed();
    }

    private static final class CapabilityOnlyBlockEntity extends BlockEntity implements SidedStorageBlockEntity {
        private final SimpleContainer items = new SimpleContainer(1);
        private final Storage<ItemVariant> storage = ContainerStorage.of(items, null);

        private CapabilityOnlyBlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.TEST_BLOCK, pos, state);
        }

        @Override
        public Storage<ItemVariant> getItemStorage(Direction side) {
            return storage;
        }
    }
}
