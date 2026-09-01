package io.github.derkottersberg.seamlesscrafting.forge.gametest;

import com.derk.easyinventorycrafter.StackIdentity;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.NearbyStorageContract;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public final class ForgeNearbyStorageScenario {
    private ForgeNearbyStorageScenario() {
    }

    public static void verifiesStandardStorageAdapter(GameTestHelper helper) {
        ItemStack enchanted = new ItemStack(Items.OAK_PLANKS, 4);
        enchanted.enchant(
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING),
            1
        );
        BlockPos capabilityPos = new BlockPos(1, 1, 1);
        helper.setBlock(capabilityPos, Blocks.TEST_BLOCK.defaultBlockState());
        BlockPos absoluteCapabilityPos = helper.absolutePos(capabilityPos);
        CapabilityOnlyBlockEntity capabilityOnly = new CapabilityOnlyBlockEntity(
            absoluteCapabilityPos,
            helper.getLevel().getBlockState(absoluteCapabilityPos)
        );
        ItemStackHandler handler = capabilityOnly.items;
        handler.setStackInSlot(0, enchanted.copy());
        helper.getLevel().setBlockEntity(capabilityOnly);
        Player player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        var discovered = NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player);
        helper.assertValueEqual(discovered.size(), 1, "Forge lookup did not discover a capability-only block entity");
        NearbyStorage storage = discovered.getFirst().storage();

        NearbyStorage.SlotSnapshot source = storage.snapshot().getFirst();
        helper.assertValueEqual(source.amount(), 4L, "Forge storage snapshot lost items");
        helper.assertTrue(NearbyStorageContract.isUsable(storage), "Forge storage failed reversible-extraction admission");
        helper.assertValueEqual(handler.getStackInSlot(0).getCount(), 4, "Forge admission mutated storage");
        helper.assertTrue(
            storage.extractExact(source.sourceIndex(), StackIdentity.of(new ItemStack(Items.OAK_PLANKS)), 1).isEmpty(),
            "Forge storage ignored exact components"
        );
        helper.assertValueEqual(handler.getStackInSlot(0).getCount(), 4, "A rejected Forge extraction mutated storage");
        ItemStack removed = storage.extractExact(source.sourceIndex(), StackIdentity.of(enchanted), 4);
        helper.assertValueEqual(removed.getCount(), 4, "Forge storage did not extract the planned amount");
        helper.assertTrue(ItemStack.isSameItemSameComponents(removed, enchanted), "Forge extraction changed components");
        handler.setStackInSlot(0, new ItemStack(Items.STONE, 64));
        helper.assertValueEqual(storage.insertExact(0, removed), 4, "Forge rollback did not fall back to another slot");
        helper.assertTrue(removed.isEmpty(), "Forge rollback reported insertion without consuming its input");
        helper.assertTrue(ItemStack.isSameItemSameComponents(handler.getStackInSlot(1), enchanted), "Forge rollback changed components");
        helper.assertValueEqual(handler.getStackInSlot(1).getCount(), 4, "Forge extraction/rollback violated conservation");

        handler.setStackInSlot(0, enchanted.copy());
        handler.setStackInSlot(1, ItemStack.EMPTY);
        helper.assertValueEqual(
            NearbyInventoryScanner.collectItemCounts(List.of(discovered.getFirst().storage())).getFirst().count(),
            4L,
            "Forge capability-only scan lost or duplicated items"
        );
        helper.assertValueEqual(
            capabilityOnly.items.getStackInSlot(0).getCount(),
            4,
            "Forge capability discovery mutated storage"
        );
        helper.succeed();
    }

    private static final class CapabilityOnlyBlockEntity extends BlockEntity {
        private final ItemStackHandler items = new ItemStackHandler(2);
        private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);

        private CapabilityOnlyBlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.TEST_BLOCK, pos, state);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                return itemCapability.cast();
            }
            return super.getCapability(capability, side);
        }
    }
}
