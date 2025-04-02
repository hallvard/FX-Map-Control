/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.geometry.Point2D;

public record WmtsTileMatrix(
    String identifier,
    double scaleDenominator,
    Point2D topLeft,
    int tileWidth,
    int tileHeight,
    int matrixWidth,
    int matrixHeight
) {
    public final double scale() {
        return 1 / (scaleDenominator * 0.00028);
    }
}
