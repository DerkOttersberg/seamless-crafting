package com.derk.easyinventorycrafter.client;

import java.util.List;
import net.minecraft.util.Mth;

/** Pure adaptive layout shared by the inventory and crafting-table panels. */
public record NearbyPanelLayout(
    int panelX,
    int panelY,
    int rows,
    boolean expanded,
    int controlsWidth
) {
    public static final int MARGIN = 6;
    public static final int COLUMNS = 4;
    public static final int SLOT_SIZE = 21;
    public static final int PANEL_WIDTH = COLUMNS * SLOT_SIZE + 6;
    public static final int MAX_ROWS = 6;
    public static final int PANEL_HEADER_HEIGHT = 16;

    public static NearbyPanelLayout calculate(
        int screenWidth,
        int screenHeight,
        int leftPos,
        int topPos,
        int imageWidth,
        boolean requestedOpen,
        boolean recipeBookOpen,
        int controlsWidth
    ) {
        int rightX = leftPos + imageWidth + MARGIN;
        int leftX = leftPos - PANEL_WIDTH - MARGIN;
        boolean rightFits = rightX + PANEL_WIDTH <= screenWidth - 4;
        boolean leftFits = !recipeBookOpen && leftX >= 4;
        int collapsedLeftX = leftPos - controlsWidth - MARGIN;
        boolean collapsedRightFits = rightX + controlsWidth <= screenWidth - 4;
        boolean collapsedLeftFits = !recipeBookOpen && collapsedLeftX >= 4;
        int x;
        if (requestedOpen && rightFits) {
            x = rightX;
        } else if (requestedOpen && leftFits) {
            x = leftX;
        } else if (collapsedRightFits) {
            x = rightX;
        } else if (collapsedLeftFits) {
            x = collapsedLeftX;
        } else {
            x = Mth.clamp(rightX, 4, Math.max(4, screenWidth - controlsWidth - 4));
        }
        int panelY = topPos + 48;
        int availableRows = (screenHeight - panelY - 4 - PANEL_HEADER_HEIGHT) / SLOT_SIZE;
        int rows = Math.max(0, Math.min(MAX_ROWS, availableRows));
        boolean expanded = requestedOpen && rows > 0 && (rightFits || leftFits);
        return new NearbyPanelLayout(x, panelY, rows, expanded, controlsWidth);
    }

    public int buttonX() {
        return panelX;
    }

    public int buttonY() {
        return panelY - 42;
    }

    public int searchY() {
        return panelY - 18;
    }

    public int panelHeight() {
        return rows * SLOT_SIZE + PANEL_HEADER_HEIGHT;
    }

    public List<PanelBounds> visibleBounds() {
        if (!expanded) {
            return List.of(new PanelBounds(panelX, buttonY(), controlsWidth, 20));
        }
        return List.of(
            new PanelBounds(panelX, buttonY(), controlsWidth, 20),
            new PanelBounds(panelX, searchY(), Math.min(PANEL_WIDTH, 84), 14),
            new PanelBounds(panelX, panelY, PANEL_WIDTH, panelHeight())
        );
    }
}
