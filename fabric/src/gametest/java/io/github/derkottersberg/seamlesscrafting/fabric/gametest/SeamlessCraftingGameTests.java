package io.github.derkottersberg.seamlesscrafting.fabric.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import io.github.derkottersberg.seamlesscrafting.fabric.FabricNearbyStorageScenario;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("removal")
public final class SeamlessCraftingGameTests {
    @GameTest(maxTicks = 40, padding = 24)
    public void scansDoubleChestOnce(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.scansDoubleChestOnce(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void returnsEnchantedIngredientsExactly(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.returnsEnchantedIngredientsExactly(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void craftsMaximumExactComponentsAndReturnsThem(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.craftsMaximumExactComponentsAndReturnsThem(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void rollsBackAfterPartialCommitExtraction(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.rollsBackAfterPartialCommitExtraction(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void respectsLockedContainers(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.respectsLockedContainers(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void doesNotLoadChunksWhileScanning(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.doesNotLoadChunksWhileScanning(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void rejectsIncompletePlacementWithoutMutation(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.rejectsIncompletePlacementWithoutMutation(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void choosesMatchingComponentVariantBeforeCommit(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.choosesMatchingComponentVariantBeforeCommit(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void preservesPacketBoundsAndStackIdentity(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.preservesPacketBoundsAndStackIdentity(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void rejectsBrokenStorageContractsAndDeduplicates(GameTestHelper helper) {
        SeamlessCraftingGameTestScenario.rejectsBrokenStorageContractsAndDeduplicates(helper);
    }

    @GameTest(maxTicks = 40, padding = 24)
    public void verifiesStandardStorageAdapter(GameTestHelper helper) {
        FabricNearbyStorageScenario.verifiesStandardStorageAdapter(helper);
    }
}
