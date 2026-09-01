package io.github.derkottersberg.seamlesscrafting.forge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import io.github.derkottersberg.seamlesscrafting.forge.ForgeNearbyStorageScenario;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestHelper;

public final class SeamlessCraftingForgeGameTests {
    private SeamlessCraftingForgeGameTests() {
    }

    public static void registerAll(BiConsumer<String, Supplier<?>> registrar) {
        register(
            registrar,
            "scans_double_chest_once",
            () -> SeamlessCraftingGameTestScenario::scansDoubleChestOnce
        );
        register(
            registrar,
            "returns_enchanted_ingredients_exactly",
            () -> SeamlessCraftingGameTestScenario::returnsEnchantedIngredientsExactly
        );
        register(
            registrar,
            "crafts_maximum_exact_components_and_returns_them",
            () -> SeamlessCraftingGameTestScenario::craftsMaximumExactComponentsAndReturnsThem
        );
        register(
            registrar,
            "rolls_back_after_partial_commit_extraction",
            () -> SeamlessCraftingGameTestScenario::rollsBackAfterPartialCommitExtraction
        );
        register(
            registrar,
            "respects_locked_containers",
            () -> SeamlessCraftingGameTestScenario::respectsLockedContainers
        );
        register(
            registrar,
            "does_not_load_chunks_while_scanning",
            () -> SeamlessCraftingGameTestScenario::doesNotLoadChunksWhileScanning
        );
        register(
            registrar,
            "rejects_incomplete_placement_without_mutation",
            () -> SeamlessCraftingGameTestScenario::rejectsIncompletePlacementWithoutMutation
        );
        register(
            registrar,
            "chooses_matching_component_variant_before_commit",
            () -> SeamlessCraftingGameTestScenario::choosesMatchingComponentVariantBeforeCommit
        );
        register(
            registrar,
            "preserves_packet_bounds_and_stack_identity",
            () -> SeamlessCraftingGameTestScenario::preservesPacketBoundsAndStackIdentity
        );
        register(
            registrar,
            "rejects_broken_storage_contracts_and_deduplicates",
            () -> SeamlessCraftingGameTestScenario::rejectsBrokenStorageContractsAndDeduplicates
        );
        register(
            registrar,
            "verifies_standard_storage_adapter",
            () -> ForgeNearbyStorageScenario::verifiesStandardStorageAdapter
        );
    }

    private static void register(
        BiConsumer<String, Supplier<?>> registrar,
        String name,
        Supplier<Consumer<GameTestHelper>> function
    ) {
        registrar.accept(name, function);
    }
}
