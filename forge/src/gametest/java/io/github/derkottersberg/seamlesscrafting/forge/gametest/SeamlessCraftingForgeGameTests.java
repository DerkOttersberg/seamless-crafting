package io.github.derkottersberg.seamlesscrafting.forge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import io.github.derkottersberg.seamlesscrafting.forge.ForgeNearbyStorageScenario;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

public final class SeamlessCraftingForgeGameTests {
    private static final String MOD_ID = "derk_easy_inventory_crafter";
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);

    static {
        TEST_FUNCTIONS.register(
            "scans_double_chest_once",
            () -> SeamlessCraftingGameTestScenario::scansDoubleChestOnce
        );
        TEST_FUNCTIONS.register(
            "returns_enchanted_ingredients_exactly",
            () -> SeamlessCraftingGameTestScenario::returnsEnchantedIngredientsExactly
        );
        TEST_FUNCTIONS.register(
            "crafts_maximum_exact_components_and_returns_them",
            () -> SeamlessCraftingGameTestScenario::craftsMaximumExactComponentsAndReturnsThem
        );
        TEST_FUNCTIONS.register(
            "rolls_back_after_partial_commit_extraction",
            () -> SeamlessCraftingGameTestScenario::rollsBackAfterPartialCommitExtraction
        );
        TEST_FUNCTIONS.register(
            "respects_locked_containers",
            () -> SeamlessCraftingGameTestScenario::respectsLockedContainers
        );
        TEST_FUNCTIONS.register(
            "does_not_load_chunks_while_scanning",
            () -> SeamlessCraftingGameTestScenario::doesNotLoadChunksWhileScanning
        );
        TEST_FUNCTIONS.register(
            "rejects_incomplete_placement_without_mutation",
            () -> SeamlessCraftingGameTestScenario::rejectsIncompletePlacementWithoutMutation
        );
        TEST_FUNCTIONS.register(
            "chooses_matching_component_variant_before_commit",
            () -> SeamlessCraftingGameTestScenario::choosesMatchingComponentVariantBeforeCommit
        );
        TEST_FUNCTIONS.register(
            "preserves_packet_bounds_and_stack_identity",
            () -> SeamlessCraftingGameTestScenario::preservesPacketBoundsAndStackIdentity
        );
        TEST_FUNCTIONS.register(
            "rejects_broken_storage_contracts_and_deduplicates",
            () -> SeamlessCraftingGameTestScenario::rejectsBrokenStorageContractsAndDeduplicates
        );
        TEST_FUNCTIONS.register(
            "verifies_standard_storage_adapter",
            () -> ForgeNearbyStorageScenario::verifiesStandardStorageAdapter
        );
    }

    private SeamlessCraftingForgeGameTests() {
    }

    public static void register(BusGroup modBusGroup) {
        TEST_FUNCTIONS.register(modBusGroup);
    }
}
