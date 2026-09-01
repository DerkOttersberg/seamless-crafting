package com.derk.easyinventorycrafter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NearbyPanelLayoutTest {
    @Test
    void usesRightSideWhenItFits() {
        NearbyPanelLayout layout = NearbyPanelLayout.calculate(640, 360, 200, 80, 176, true, false, 84);

        assertTrue(layout.expanded());
        assertEquals(382, layout.panelX());
        assertEquals(NearbyPanelLayout.MAX_ROWS, layout.rows());
    }

    @Test
    void fallsBackToLeftSideWhenRightIsOccupied() {
        NearbyPanelLayout layout = NearbyPanelLayout.calculate(400, 300, 200, 60, 176, true, false, 84);

        assertTrue(layout.expanded());
        assertEquals(104, layout.panelX());
    }

    @Test
    void collapsesInsteadOfOverlappingWhenNeitherSideFits() {
        NearbyPanelLayout layout = NearbyPanelLayout.calculate(360, 240, 92, 40, 176, true, true, 84);

        assertFalse(layout.expanded());
        assertEquals(20, layout.visibleBounds().getFirst().height());
    }

    @Test
    void adaptsRowsToShortScreens() {
        NearbyPanelLayout layout = NearbyPanelLayout.calculate(640, 210, 200, 80, 176, true, false, 84);

        assertTrue(layout.expanded());
        assertEquals(2, layout.rows());
    }
}
