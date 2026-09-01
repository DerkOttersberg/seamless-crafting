package io.github.derkottersberg.seamlesscrafting.neoforge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import io.github.derkottersberg.seamlesscrafting.neoforge.NeoForgeNearbyStorageScenario;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SeamlessCraftingNeoForgeGameTests {
    private static final String MOD_ID = "derk_easy_inventory_crafter";
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MOD_ID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DOUBLE_CHEST =
        TEST_FUNCTIONS.register("scans_double_chest_once", () -> SeamlessCraftingGameTestScenario::scansDoubleChestOnce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENCHANTED_ROLLBACK =
        TEST_FUNCTIONS.register(
            "returns_enchanted_ingredients_exactly",
            () -> SeamlessCraftingGameTestScenario::returnsEnchantedIngredientsExactly
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MAXIMUM_EXACT =
        TEST_FUNCTIONS.register(
            "crafts_maximum_exact_components_and_returns_them",
            () -> SeamlessCraftingGameTestScenario::craftsMaximumExactComponentsAndReturnsThem
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PARTIAL_COMMIT_ROLLBACK =
        TEST_FUNCTIONS.register(
            "rolls_back_after_partial_commit_extraction",
            () -> SeamlessCraftingGameTestScenario::rollsBackAfterPartialCommitExtraction
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOCKED_CONTAINER =
        TEST_FUNCTIONS.register(
            "respects_locked_containers",
            () -> SeamlessCraftingGameTestScenario::respectsLockedContainers
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> UNLOADED_CHUNK =
        TEST_FUNCTIONS.register(
            "does_not_load_chunks_while_scanning",
            () -> SeamlessCraftingGameTestScenario::doesNotLoadChunksWhileScanning
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INCOMPLETE_TRANSACTION =
        TEST_FUNCTIONS.register(
            "rejects_incomplete_placement_without_mutation",
            () -> SeamlessCraftingGameTestScenario::rejectsIncompletePlacementWithoutMutation
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COMPONENT_BACKTRACKING =
        TEST_FUNCTIONS.register(
            "chooses_matching_component_variant_before_commit",
            () -> SeamlessCraftingGameTestScenario::choosesMatchingComponentVariantBeforeCommit
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PACKET_BOUNDS =
        TEST_FUNCTIONS.register(
            "preserves_packet_bounds_and_stack_identity",
            () -> SeamlessCraftingGameTestScenario::preservesPacketBoundsAndStackIdentity
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STORAGE_CONTRACTS =
        TEST_FUNCTIONS.register(
            "rejects_broken_storage_contracts_and_deduplicates",
            () -> SeamlessCraftingGameTestScenario::rejectsBrokenStorageContractsAndDeduplicates
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STORAGE_ADAPTER =
        TEST_FUNCTIONS.register(
            "verifies_standard_storage_adapter",
            () -> NeoForgeNearbyStorageScenario::verifiesStandardStorageAdapter
        );

    private SeamlessCraftingNeoForgeGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(SeamlessCraftingNeoForgeGameTests::registerTestCapabilities);
        modEventBus.addListener(SeamlessCraftingNeoForgeGameTests::registerTests);
    }

    private static void registerTestCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.Item.BLOCK,
            (level, pos, state, blockEntity, side) ->
                blockEntity instanceof NeoForgeNearbyStorageScenario.CapabilityOnlyBlockEntity capabilityOnly
                    ? capabilityOnly
                    : null,
            Blocks.TEST_BLOCK
        );
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(MOD_ID, "default_environment"),
            new TestEnvironmentDefinition.AllOf()
        );
        registerTest(event, environment, "scans_double_chest_once", DOUBLE_CHEST);
        registerTest(event, environment, "returns_enchanted_ingredients_exactly", ENCHANTED_ROLLBACK);
        registerTest(event, environment, "crafts_maximum_exact_components_and_returns_them", MAXIMUM_EXACT);
        registerTest(event, environment, "rolls_back_after_partial_commit_extraction", PARTIAL_COMMIT_ROLLBACK);
        registerTest(event, environment, "respects_locked_containers", LOCKED_CONTAINER);
        registerTest(event, environment, "does_not_load_chunks_while_scanning", UNLOADED_CHUNK);
        registerTest(event, environment, "rejects_incomplete_placement_without_mutation", INCOMPLETE_TRANSACTION);
        registerTest(event, environment, "chooses_matching_component_variant_before_commit", COMPONENT_BACKTRACKING);
        registerTest(event, environment, "preserves_packet_bounds_and_stack_identity", PACKET_BOUNDS);
        registerTest(
            event,
            environment,
            "rejects_broken_storage_contracts_and_deduplicates",
            STORAGE_CONTRACTS
        );
        registerTest(event, environment, "verifies_standard_storage_adapter", STORAGE_ADAPTER);
    }

    private static void registerTest(
        RegisterGameTestsEvent event,
        Holder<TestEnvironmentDefinition<?>> environment,
        String name,
        DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("empty"),
            40,
            0,
            true
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(MOD_ID, name),
            new FunctionGameTestInstance(function.getKey(), data)
        );
    }
}
