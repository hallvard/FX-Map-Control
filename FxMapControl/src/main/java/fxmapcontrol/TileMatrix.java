/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Defines zoom level and tile index ranges of a MapTileLayer.
 */
public record TileMatrix(
    int zoomLevel,
    int xMin,
    int yMin,
    int xMax,
    int yMax
) {
}
