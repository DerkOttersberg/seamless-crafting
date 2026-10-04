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
            Enchantments.UNBREAKING,
            1
        );
        BlockPos capabilityPos = new BlockPos(1, 1, 1);
        helper.setBlock(capabilityPos, Blocks.CHEST.defaultBlockState());
        BlockPos absoluteCapabilityPos = helper.absolutePos(capabilityPos);
        ItemStackHandler handler = new ItemStackHandler(2);
        CapabilityOnlyBlockEntity capabilityOnly = new CapabilityOnlyBlockEntity(
            absoluteCapabilityPos,
            helper.getLevel().getBlockState(absoluteCapabilityPos),
            handler
        );
        handler.setStackInSlot(0, enchanted.copy());
        helper.getLevel().setBlockEntity(capabilityOnly);
        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        var discovered = NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player);
        helper.assertTrue(java.util.Objects.equals(discovered.size(), 1), "Forge lookup did not discover a capability-only block entity");
        NearbyStorage storage = discovered.get(0).storage();

        NearbyStorage.SlotSnapshot source = storage.snapshot().get(0);
        helper.assertTrue(java.util.Objects.equals(source.amount(), 4L), "Forge storage snapshot lost items");
        helper.assertTrue(NearbyStorageContract.isUsable(storage), "Forge storage failed reversible-extraction admission");
        helper.assertTrue(java.util.Objects.equals(handler.getStackInSlot(0).getCount(), 4), "Forge admission mutated storage");
        helper.assertTrue(
            storage.extractExact(source.sourceIndex(), StackIdentity.of(new ItemStack(Items.OAK_PLANKS)), 1).isEmpty(),
            "Forge storage ignored exact components"
        );
        helper.assertTrue(java.util.Objects.equals(handler.getStackInSlot(0).getCount(), 4), "A rejected Forge extraction mutated storage");
        ItemStack removed = storage.extractExact(source.sourceIndex(), StackIdentity.of(enchanted), 4);
        helper.assertTrue(java.util.Objects.equals(removed.getCount(), 4), "Forge storage did not extract the planned amount");
        helper.assertTrue(ItemStack.isSameItemSameTags(removed, enchanted), "Forge extraction changed components");
        handler.setStackInSlot(0, new ItemStack(Items.STONE, 64));
        helper.assertTrue(java.util.Objects.equals(storage.insertExact(0, removed), 4), "Forge rollback did not fall back to another slot");
        helper.assertTrue(removed.isEmpty(), "Forge rollback reported insertion without consuming its input");
        helper.assertTrue(ItemStack.isSameItemSameTags(handler.getStackInSlot(1), enchanted), "Forge rollback changed components");
        helper.assertTrue(java.util.Objects.equals(handler.getStackInSlot(1).getCount(), 4), "Forge extraction/rollback violated conservation");

        handler.setStackInSlot(0, enchanted.copy());
        handler.setStackInSlot(1, ItemStack.EMPTY);
        helper.assertTrue(java.util.Objects.equals(NearbyInventoryScanner.collectItemCounts(List.of(discovered.get(0).storage())).get(0).count(), 4L), "Forge capability-only scan lost or duplicated items");
        helper.assertTrue(java.util.Objects.equals(handler.getStackInSlot(0).getCount(), 4), "Forge capability discovery mutated storage");
        helper.succeed();
    }

    public static void rejectsUnsafeStorageHandlers(GameTestHelper helper) {
        OutputOnlyHandler outputOnly = new OutputOnlyHandler();
        StatefulSimulationHandler stateful = new StatefulSimulationHandler();
        AmbiguousRemainderHandler ambiguous = new AmbiguousRemainderHandler();
        outputOnly.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        stateful.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        ambiguous.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));

        installCapability(helper, new BlockPos(1, 1, 1), outputOnly);
        installCapability(helper, new BlockPos(2, 1, 1), stateful);
        installCapability(helper, new BlockPos(3, 1, 1), ambiguous);

        Player player = helper.makeMockPlayer();
        player.getInventory().clearContent();
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 1, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        var discovered = NearbyInventoryScanner.scan(helper.getLevel(), player.blockPosition(), 4, player);
        helper.assertTrue(discovered.isEmpty(), "Forge admitted an output-only, stateful, or ambiguous item handler");
        helper.assertTrue(outputOnly.simulatedInsertCalls > 0, "Forge did not probe rollback insertion on an output-only handler");
        helper.assertTrue(stateful.simulatedInsertCalls >= 3, "Forge did not compare repeated rollback insertion plans");
        helper.assertTrue(ambiguous.simulatedInsertCalls > 0, "Forge did not validate the simulated insertion remainder");
        helper.assertTrue(java.util.Objects.equals(outputOnly.getStackInSlot(0).getCount(), 4), "Output-only admission changed live contents");
        helper.assertTrue(java.util.Objects.equals(stateful.getStackInSlot(0).getCount(), 4), "Stateful admission changed live contents");
        helper.assertTrue(java.util.Objects.equals(ambiguous.getStackInSlot(0).getCount(), 4), "Ambiguous admission changed live contents");
        helper.succeed();
    }

    private static void installCapability(GameTestHelper helper, BlockPos relativePos, IItemHandler handler) {
        helper.setBlock(relativePos, Blocks.CHEST.defaultBlockState());
        BlockPos absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockEntity(new CapabilityOnlyBlockEntity(
            absolutePos,
            helper.getLevel().getBlockState(absolutePos),
            handler
        ));
    }

    private static final class CapabilityOnlyBlockEntity extends BlockEntity {
        private final LazyOptional<IItemHandler> itemCapability;

        private CapabilityOnlyBlockEntity(BlockPos pos, BlockState state, IItemHandler handler) {
            super(net.minecraft.world.level.block.entity.BlockEntityType.CHEST, pos, state);
            itemCapability = LazyOptional.of(() -> handler);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                return itemCapability.cast();
            }
            return super.getCapability(capability, side);
        }
    }

    private static final class OutputOnlyHandler extends ItemStackHandler {
        private int simulatedInsertCalls;

        private OutputOnlyHandler() {
            super(1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (simulate) {
                simulatedInsertCalls++;
            }
            return stack;
        }
    }

    private static final class StatefulSimulationHandler extends ItemStackHandler {
        private int simulatedInsertCalls;

        private StatefulSimulationHandler() {
            super(2);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!simulate) {
                return super.insertItem(slot, stack, false);
            }
            simulatedInsertCalls++;
            if (simulatedInsertCalls == 1 || slot == 1) {
                return ItemStack.EMPTY;
            }
            return stack.copyWithCount(Math.max(1, stack.getCount() - 1));
        }
    }

    private static final class AmbiguousRemainderHandler extends ItemStackHandler {
        private int simulatedInsertCalls;

        private AmbiguousRemainderHandler() {
            super(1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!simulate) {
                return super.insertItem(slot, stack, false);
            }
            simulatedInsertCalls++;
            return new ItemStack(Items.STONE, Math.max(1, stack.getCount() - 1));
        }
    }
}
