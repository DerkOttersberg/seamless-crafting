package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.client.HoveredNearbyStack;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

/** Loaded by JEI's Fabric entrypoint only; it is inert when JEI is absent. */
public final class SeamlessCraftingFabricJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return SeamlessCraftingMod.id("jei_overlay");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CraftingScreen.class, new NearbyHandler<>());
        registration.addGuiContainerHandler(InventoryScreen.class, new NearbyHandler<>());
    }

    private static final class NearbyHandler<T extends AbstractContainerScreen<?>> implements IGuiContainerHandler<T> {
        @Override
        public List<Rect2i> getGuiExtraAreas(T screen) {
            if (!(screen instanceof NearbyPanelAccess access)) {
                return List.of();
            }
            return access.derk$getOverlayExclusionBounds().stream()
                .map(bounds -> new Rect2i(bounds.x(), bounds.y(), bounds.width(), bounds.height()))
                .toList();
        }

        @Override
        public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory factory,
            T screen,
            double mouseX,
            double mouseY
        ) {
            if (!(screen instanceof NearbyPanelAccess access)) {
                return Optional.empty();
            }
            Optional<HoveredNearbyStack> hovered = access.derk$getNearbyStackAt(mouseX, mouseY);
            if (hovered.isEmpty()) {
                return Optional.empty();
            }
            HoveredNearbyStack value = hovered.get();
            return factory.createBuilder(value.stack()).buildWithArea(
                value.bounds().x(),
                value.bounds().y(),
                value.bounds().width(),
                value.bounds().height()
            );
        }
    }
}
