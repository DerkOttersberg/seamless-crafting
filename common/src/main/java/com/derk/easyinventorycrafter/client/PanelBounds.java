package com.derk.easyinventorycrafter.client;

public record PanelBounds(int x, int y, int width, int height) {
    public PanelBounds {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("bounds must be non-negative");
        }
    }
}
