package io.github.derkottersberg.seamlesscrafting.fabric.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import io.github.derkottersberg.seamlesscrafting.fabric.FabricNearbyStorageScenario;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("removal")
public final class SeamlessCraftingGameTests {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void scansDoubleChestOnce(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.scansDoubleChestOnce(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void returnsEnchantedIngredientsExactly(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.returnsEnchantedIngredientsExactly(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void craftsMaximumExactComponentsAndReturnsThem(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.craftsMaximumExactComponentsAndReturnsThem(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void rollsBackAfterPartialCommitExtraction(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.rollsBackAfterPartialCommitExtraction(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void respectsLockedContainers(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.respectsLockedContainers(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void doesNotLoadChunksWhileScanning(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.doesNotLoadChunksWhileScanning(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void rejectsIncompletePlacementWithoutMutation(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.rejectsIncompletePlacementWithoutMutation(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void choosesMatchingComponentVariantBeforeCommit(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.choosesMatchingComponentVariantBeforeCommit(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void preservesPacketBoundsAndStackIdentity(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.preservesPacketBoundsAndStackIdentity(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void rejectsBrokenStorageContractsAndDeduplicates(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.rejectsBrokenStorageContractsAndDeduplicates(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void verifiesStandardStorageAdapter(GameTestHelper helper) {
        FabricNearbyStorageScenario.verifiesStandardStorageAdapter(helper);
    }
}
