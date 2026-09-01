package com.derk.easyinventorycrafter.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public interface NearbyPanelAccess {
    List<PanelBounds> derk$getVisiblePanelBounds();

    Optional<HoveredNearbyStack> derk$getNearbyStackAt(double mouseX, double mouseY);

    boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount);

    boolean derk$handleCharTyped(CharacterEvent input);

    boolean derk$handleKeyPressed(KeyEvent input);

    boolean derk$handleMouseClick(MouseButtonEvent click, boolean doubleClick);
}
