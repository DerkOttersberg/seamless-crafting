package io.github.derkottersberg.seamlesscrafting.forge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

@Mod(SeamlessCraftingForgeGameTests.MOD_ID)
public final class SeamlessCraftingForgeGameTests {
    public static final String MOD_ID = "derk_easy_inventory_crafter_gametest";
    private static final String TEST_NAMESPACE = "derk_easy_inventory_crafter";

    public SeamlessCraftingForgeGameTests(FMLJavaModLoadingContext context) {
        RegisterEvent.getBus(context.getModBusGroup())
            .addListener(SeamlessCraftingForgeGameTests::registerTestFunctions);
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
        register(
            event,
            "rejects_unsafe_storage_handlers",
            () -> ForgeNearbyStorageScenario::rejectsUnsafeStorageHandlers
        );
    }

    private static void register(
        RegisterEvent event,
        String name,
        Supplier<Consumer<GameTestHelper>> function
    ) {
        event.register(
            Registries.TEST_FUNCTION,
            Identifier.fromNamespaceAndPath(TEST_NAMESPACE, name),
            function
        );
    }
}
