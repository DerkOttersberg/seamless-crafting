package com.derk.easyinventorycrafter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NearbyPanelPresentationTest {
    @Test
    void exposesStableLoadingEmptyAndFilteredStates() {
        assertEquals(
            NearbyPanelPresentation.Status.LOADING,
            NearbyPanelPresentation.status(true, false, 0, 0)
        );
        assertEquals(
            NearbyPanelPresentation.Status.LOADING,
            NearbyPanelPresentation.status(false, false, 0, 0)
        );
        assertEquals(
            NearbyPanelPresentation.Status.EMPTY,
            NearbyPanelPresentation.status(false, true, 0, 0)
        );
        assertEquals(
            NearbyPanelPresentation.Status.NO_MATCHES,
            NearbyPanelPresentation.status(false, true, 3, 0)
        );
        assertEquals(
            NearbyPanelPresentation.Status.CONTENT,
            NearbyPanelPresentation.status(false, true, 3, 2)
        );
    }

    @Test
    void textColorsAreOpaqueArgbValues() {
        assertEquals(0xFF, NearbyPanelPresentation.HEADER_COLOR >>> 24);
        assertEquals(0xFF, NearbyPanelPresentation.PARTIAL_HEADER_COLOR >>> 24);
        assertEquals(0xFF, NearbyPanelPresentation.STATUS_COLOR >>> 24);
    }

    @Test
    void labelsRemainStable() {
        assertEquals("Nearby", NearbyPanelPresentation.header(false));
        assertEquals("Nearby (partial)", NearbyPanelPresentation.header(true));
        assertEquals("Loading...", NearbyPanelPresentation.Status.LOADING.label());
        assertEquals("No nearby items", NearbyPanelPresentation.Status.EMPTY.label());
        assertEquals("No matches", NearbyPanelPresentation.Status.NO_MATCHES.label());
    }
}
