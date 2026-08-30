package com.derk.easyinventorycrafter.client;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public interface NearbyPanelAccess {
    boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount);

    boolean derk$handleCharTyped(CharacterEvent input);

    boolean derk$handleKeyPressed(KeyEvent input);

    boolean derk$handleMouseClick(MouseButtonEvent click, boolean doubleClick);
}
