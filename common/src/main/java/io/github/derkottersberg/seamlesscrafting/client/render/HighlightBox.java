package io.github.derkottersberg.seamlesscrafting.client.render;

/** Immutable camera-relative bounds used by the nearby-container highlight renderer. */
public record HighlightBox(
    float minX,
    float minY,
    float minZ,
    float maxX,
    float maxY,
    float maxZ
) {
}
