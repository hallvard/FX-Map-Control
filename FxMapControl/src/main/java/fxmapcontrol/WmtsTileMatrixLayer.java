/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;

public class WmtsTileMatrixLayer extends Parent {

    private final WmtsTileMatrix tileMatrix;
    private final int zoomLevel; // index of TileMatrix in WmtsTileMatrixSet.TileMatrixes

    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private List<Tile> tiles = new ArrayList<>();

    public WmtsTileMatrixLayer(WmtsTileMatrix tileMatrix, int zoomLevel) {
        this.tileMatrix = tileMatrix;
        this.zoomLevel = zoomLevel;
        getTransforms().add(new Affine());
    }

    public final WmtsTileMatrix getTileMatrix() {
        return tileMatrix;
    }

    public final void setTransform(ViewTransform viewTransform) {
        // tile matrix origin in pixels
        //
        Point2D tileMatrixOrigin = new Point2D(tileMatrix.tileWidth() * xMin, tileMatrix.tileHeight() * yMin);

        getTransforms().set(0,
                viewTransform.getTileLayerTransform(tileMatrix.scale(), tileMatrix.topLeft(), tileMatrixOrigin));
    }

    public final boolean setBounds(ViewTransform viewTransform, double viewWidth, double viewHeight) {
        // bounds in tile pixels from view size
        //
        Bounds bounds = viewTransform.getTileMatrixBounds(tileMatrix.scale(), tileMatrix.topLeft(), viewWidth, viewHeight);

        // tile column and row index bounds
        //
        int minX = (int) Math.floor(bounds.getMinX() / tileMatrix.tileWidth());
        int minY = (int) Math.floor(bounds.getMinY() / tileMatrix.tileHeight());
        int maxX = (int) Math.floor(bounds.getMaxX() / tileMatrix.tileWidth());
        int maxY = (int) Math.floor(bounds.getMaxY() / tileMatrix.tileHeight());

        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        maxX = Math.min(Math.max(maxX, 0), tileMatrix.matrixWidth() - 1);
        maxY = Math.min(Math.max(maxY, 0), tileMatrix.matrixHeight() - 1);

        if (xMin == minX && yMin == minY && xMax == maxX && yMax == maxY) {
            return false;
        }

        xMin = minX;
        yMin = minY;
        xMax = maxX;
        yMax = maxY;
        return true;
    }

    public final List<Tile> updateTiles() {
        List<Tile> newTiles = new ArrayList<>();

        for (int ty = yMin; ty <= yMax; ty++) {
            for (int tx = xMin; tx <= xMax; tx++) {
                int x = tx;
                int y = ty;
                newTiles.add(tiles.stream()
                        .filter(t -> t.getX() == x && t.getY() == y).findAny()
                        .orElse(new Tile(zoomLevel, x, y)));
            }
        }

        tiles = newTiles;

        if (tiles.isEmpty()) {
            getChildren().clear();

        } else {
            getChildren().setAll(tiles.stream()
                    .map(tile -> {
                        ImageView imageView = tile.getImageView();
                        imageView.setX(tileMatrix.tileWidth() * (tile.getX() - xMin));
                        imageView.setY(tileMatrix.tileHeight() * (tile.getY() - yMin));
                        imageView.setFitWidth(tileMatrix.tileWidth());
                        imageView.setFitHeight(tileMatrix.tileHeight());
                        return imageView;
                    })
                    .toList());
        }

        return tiles;
    }
}
