package com.derk.easyinventorycrafter.client;

import java.util.List;
import java.util.Optional;

public interface NearbyPanelAccess {
    List<PanelBounds> derk$getVisiblePanelBounds();

    List<PanelBounds> derk$getOverlayExclusionBounds();

    Optional<HoveredNearbyStack> derk$getNearbyStackAt(double mouseX, double mouseY);

    boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount);

    boolean derk$handleCharTyped(char codePoint, int modifiers);

    boolean derk$handleKeyPressed(int keyCode, int scanCode, int modifiers);

    boolean derk$handleMouseClick(double mouseX, double mouseY, int button);
}
