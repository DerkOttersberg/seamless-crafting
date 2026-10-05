package io.github.derkottersberg.seamlesscrafting.neoforge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import net.minecraft.gametest.framework.GameTest;
import io.github.derkottersberg.seamlesscrafting.neoforge.NeoForgeNearbyStorageScenario;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("derk_easy_inventory_crafter")

@PrefixGameTestTemplate(false)
public final class SeamlessCraftingNeoForgeGameTests {
    public SeamlessCraftingNeoForgeGameTests() {}
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) ->
            event.register(SeamlessCraftingNeoForgeGameTests.class));
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            event.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> blockEntity instanceof NeoForgeNearbyStorageScenario.CapabilityOnlyBlockEntity test
                    ? test.handler() : null, net.minecraft.world.level.block.Blocks.CHEST));
    }


    @GameTest(template = "empty", timeoutTicks = 40)
    public static void scans_double_chest_once(GameTestHelper helper) { SeamlessCraftingGameTestScenario.scansDoubleChestOnce(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void returns_enchanted_ingredients_exactly(GameTestHelper helper) { SeamlessCraftingGameTestScenario.returnsEnchantedIngredientsExactly(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void crafts_maximum_exact_components_and_returns_them(GameTestHelper helper) { SeamlessCraftingGameTestScenario.craftsMaximumExactComponentsAndReturnsThem(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rolls_back_after_partial_commit_extraction(GameTestHelper helper) { SeamlessCraftingGameTestScenario.rollsBackAfterPartialCommitExtraction(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void respects_locked_containers(GameTestHelper helper) { SeamlessCraftingGameTestScenario.respectsLockedContainers(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void does_not_load_chunks_while_scanning(GameTestHelper helper) { SeamlessCraftingGameTestScenario.doesNotLoadChunksWhileScanning(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rejects_incomplete_placement_without_mutation(GameTestHelper helper) { SeamlessCraftingGameTestScenario.rejectsIncompletePlacementWithoutMutation(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void chooses_matching_component_variant_before_commit(GameTestHelper helper) { SeamlessCraftingGameTestScenario.choosesMatchingComponentVariantBeforeCommit(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void preserves_packet_bounds_and_stack_identity(GameTestHelper helper) { SeamlessCraftingGameTestScenario.preservesPacketBoundsAndStackIdentity(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rejects_broken_storage_contracts_and_deduplicates(GameTestHelper helper) { SeamlessCraftingGameTestScenario.rejectsBrokenStorageContractsAndDeduplicates(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void verifies_standard_storage_adapter(GameTestHelper helper) { NeoForgeNearbyStorageScenario.verifiesStandardStorageAdapter(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rejects_unsafe_storage_handlers(GameTestHelper helper) { NeoForgeNearbyStorageScenario.rejectsUnsafeStorageHandlers(helper); }

}
