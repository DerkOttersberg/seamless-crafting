package io.github.derkottersberg.seamlesscrafting.forge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import io.github.derkottersberg.seamlesscrafting.forge.ForgeNearbyStorageScenario;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.RegisterEvent;

public final class SeamlessCraftingForgeGameTests {
    private static final String MOD_ID = "derk_easy_inventory_crafter";
    private SeamlessCraftingForgeGameTests() {
    }

    public static void register(BusGroup modBusGroup) {
        // RegisterEvent fires after Minecraft has bootstrapped its built-in
        // registries. Creating a TEST_FUNCTION DeferredRegister while this
        // source-set-only class is reflectively loaded from the mod
        // constructor can initialize BuiltInRegistries too early in a fresh
        // process.
        RegisterEvent.getBus(modBusGroup).addListener(SeamlessCraftingForgeGameTests::registerTestFunctions);
    }

    private static void registerTestFunctions(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.TEST_FUNCTION) {
            return;
        }
        register(
            event,
            "scans_double_chest_once",
            () -> SeamlessCraftingGameTestScenario::scansDoubleChestOnce
        );
        register(
            event,
            "returns_enchanted_ingredients_exactly",
            () -> SeamlessCraftingGameTestScenario::returnsEnchantedIngredientsExactly
        );
        register(
            event,
            "crafts_maximum_exact_components_and_returns_them",
            () -> SeamlessCraftingGameTestScenario::craftsMaximumExactComponentsAndReturnsThem
        );
        register(
            event,
            "rolls_back_after_partial_commit_extraction",
            () -> SeamlessCraftingGameTestScenario::rollsBackAfterPartialCommitExtraction
        );
        register(
            event,
            "respects_locked_containers",
            () -> SeamlessCraftingGameTestScenario::respectsLockedContainers
        );
        register(
            event,
            "does_not_load_chunks_while_scanning",
            () -> SeamlessCraftingGameTestScenario::doesNotLoadChunksWhileScanning
        );
        register(
            event,
            "rejects_incomplete_placement_without_mutation",
            () -> SeamlessCraftingGameTestScenario::rejectsIncompletePlacementWithoutMutation
        );
        register(
            event,
            "chooses_matching_component_variant_before_commit",
            () -> SeamlessCraftingGameTestScenario::choosesMatchingComponentVariantBeforeCommit
        );
        register(
            event,
            "preserves_packet_bounds_and_stack_identity",
            () -> SeamlessCraftingGameTestScenario::preservesPacketBoundsAndStackIdentity
        );
        register(
            event,
            "rejects_broken_storage_contracts_and_deduplicates",
            () -> SeamlessCraftingGameTestScenario::rejectsBrokenStorageContractsAndDeduplicates
        );
        register(
            event,
            "verifies_standard_storage_adapter",
            () -> ForgeNearbyStorageScenario::verifiesStandardStorageAdapter
        );
    }

    private static void register(
        RegisterEvent event,
        String name,
        Supplier<Consumer<GameTestHelper>> function
    ) {
        event.register(
            Registries.TEST_FUNCTION,
            Identifier.fromNamespaceAndPath(MOD_ID, name),
            function
        );
    }
}
