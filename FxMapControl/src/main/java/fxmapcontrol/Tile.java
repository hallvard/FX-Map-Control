/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import javafx.animation.FadeTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Provides the ImageView that displays a map tile image.
 */
public class Tile {

    private final TileCoords coords;
    private final ImageView imageView;
    private boolean pending;

    public Tile(int zoomLevel, int x, int y) {
        this.coords = new TileCoords(x, y, zoomLevel);
        imageView = new ImageView();
        imageView.setOpacity(0d);
        pending = true;
    }

    public final TileCoords getCoords() {
        return coords;
    }

    public final int getZoomLevel() {
        return coords.zoomLevel();
    }

    public final int getX() {
        return coords.x();
    }

    public final int getY() {
        return coords.y();
    }

    public final int getXIndex() {
      return coords.xIndex();
    }

    public final boolean isPending() {
        return pending;
    }

    public final ImageView getImageView() {
        return imageView;
    }

    public final Image getImage() {
        return imageView.getImage();
    }

    public final void setImage(Image image, boolean fade) {
        pending = false;

        if (image != null) {
            imageView.setImage(image);
            Duration fadeDuration;
            if (fade && (fadeDuration = MapBase.getImageFadeDuration()).greaterThan(Duration.ZERO)) {
                FadeTransition fadeTransition = new FadeTransition(fadeDuration, imageView);
                fadeTransition.setToValue(1d);
                fadeTransition.play();
            } else {
                imageView.setOpacity(1d);
            }
        }
    }
}
