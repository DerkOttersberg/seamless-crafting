package io.github.derkottersberg.seamlesscrafting.forge.gametest;

import com.derk.easyinventorycrafter.gametest.SeamlessCraftingGameTestScenario;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("derk_easy_inventory_crafter")
@net.minecraftforge.fml.common.Mod("derk_easy_inventory_crafter_gametest")
@PrefixGameTestTemplate(false)
public final class SeamlessCraftingForgeGameTests {
    public SeamlessCraftingForgeGameTests() {}
    static {
        // A packet-capable in-process player: no client, desktop or network socket.
        SeamlessCraftingGameTestScenario.usePlayerFactory(helper -> {
            var level = helper.getLevel();
            var channel = new io.netty.channel.embedded.EmbeddedChannel();
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel.pipeline().addLast("packet_handler", connection);
            channel.pipeline().fireChannelActive();
            channel.attr(io.netty.util.AttributeKey.<String>valueOf("fml:netversion"))
                .set(net.minecraftforge.network.NetworkConstants.NETVERSION);
            var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Craft-QA"));
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player);
            return player;
        });
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
    public static void verifies_standard_storage_adapter(GameTestHelper helper) { ForgeNearbyStorageScenario.verifiesStandardStorageAdapter(helper); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rejects_unsafe_storage_handlers(GameTestHelper helper) { ForgeNearbyStorageScenario.rejectsUnsafeStorageHandlers(helper); }

}
