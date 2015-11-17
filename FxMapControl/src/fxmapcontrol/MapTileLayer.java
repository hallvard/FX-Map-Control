/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2015 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableIntegerProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.util.Duration;

/**
 * Fills the map viewport with map tiles from a TileSource.
 */
public class MapTileLayer extends Parent implements IMapNode {

    private static final StyleablePropertyFactory<MapTileLayer> propertyFactory
            = new StyleablePropertyFactory<>(Parent.getClassCssMetaData());

    private static final CssMetaData<MapTileLayer, Number> maxDownloadThreadsCssMetaData
            = propertyFactory.createSizeCssMetaData("-fx-max-download-threads", s -> s.maxDownloadThreadsProperty);

    private static final CssMetaData<MapTileLayer, Duration> updateDelayCssMetaData
            = propertyFactory.createDurationCssMetaData("-fx-update-delay", s -> s.updateDelayProperty);

    private static final CssMetaData<MapTileLayer, Boolean> updateWhileViewportChangingCssMetaData
            = propertyFactory.createBooleanCssMetaData("-fx-update-while-viewport-changing", s -> s.updateWhileViewportChangingProperty);

    private final StyleableIntegerProperty maxDownloadThreadsProperty
            = new SimpleStyleableIntegerProperty(maxDownloadThreadsCssMetaData, this, "maxDownloadThreads", 4);

    private final StyleableObjectProperty<Duration> updateDelayProperty
            = new SimpleStyleableObjectProperty<>(updateDelayCssMetaData, this, "updateDelay", Duration.seconds(0.5));

    private final StyleableBooleanProperty updateWhileViewportChangingProperty
            = new SimpleStyleableBooleanProperty(updateWhileViewportChangingCssMetaData, this, "updateWhileViewportChanging");

    private final ObjectProperty<TileSource> tileSourceProperty = new SimpleObjectProperty<>(this, "tileSource");

    private final ITileImageLoader tileImageLoader;
    private final Timeline updateTimeline = new Timeline();
    private final MapNodeHelper mapNode = new MapNodeHelper(e -> viewportTransformChanged());
    private ArrayList<Tile> tiles = new ArrayList<>();
    private TileGrid tileGrid;
    private double mapOriginX;
    private double zoomLevelOffset;
    private int minZoomLevel;
    private int maxZoomLevel = 18;
    private String name;

    public MapTileLayer() {
        this(new TileImageLoader());
    }

    public MapTileLayer(ITileImageLoader tileImageLoader) {
        getStyleClass().add("map-tile-layer");
        getTransforms().add(new Affine());
        setMouseTransparent(true);

        this.tileImageLoader = tileImageLoader;

        tileSourceProperty.addListener((observable, oldValue, newValue) -> updateTiles(true));

        updateTimeline.getKeyFrames().add(new KeyFrame(getUpdateDelay(), e -> updateTileGrid()));

        updateDelayProperty.addListener((observable, oldValue, newValue) -> {
            updateTimeline.getKeyFrames().set(0, new KeyFrame(getUpdateDelay(), e -> updateTileGrid()));
        });
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return propertyFactory.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public final MapBase getMap() {
        return mapNode.getMap();
    }

    @Override
    public final void setMap(MapBase map) {
        mapNode.setMap(map);

        if (map != null) {
            mapOriginX = map.getMapOrigin().getX();
        }

        updateTileGrid();
    }

    public final IntegerProperty maxDownloadThreadsProperty() {
        return maxDownloadThreadsProperty;
    }

    public final int getMaxDownloadThreads() {
        return maxDownloadThreadsProperty.get();
    }

    public final void setMaxDownloadThreads(int maxDownloadThreads) {
        maxDownloadThreadsProperty.set(maxDownloadThreads);
    }

    public final ObjectProperty<Duration> updateDelayProperty() {
        return updateDelayProperty;
    }

    public final Duration getUpdateDelay() {
        return updateDelayProperty.get();
    }

    public final void setUpdateDelay(Duration updateDelay) {
        updateDelayProperty.set(updateDelay);
    }

    public final BooleanProperty updateWhileViewportChangingProperty() {
        return updateWhileViewportChangingProperty;
    }

    public final boolean getUpdateWhileViewportChanging() {
        return updateWhileViewportChangingProperty.get();
    }

    public final void setUpdateWhileViewportChanging(boolean updateWhileViewportChanging) {
        updateWhileViewportChangingProperty.set(updateWhileViewportChanging);
    }

    public final ObjectProperty<TileSource> tileSourceProperty() {
        return tileSourceProperty;
    }

    public final TileSource getTileSource() {
        return tileSourceProperty.get();
    }

    public final void setTileSource(TileSource tileSource) {
        tileSourceProperty.set(tileSource);
    }

    public final double getZoomLevelOffset() {
        return zoomLevelOffset;
    }

    public final void setZoomLevelOffset(double zoomLevelOffset) {
        this.zoomLevelOffset = zoomLevelOffset;
    }

    public final int getMinZoomLevel() {
        return minZoomLevel;
    }

    public final void setMinZoomLevel(int minZoomLevel) {
        this.minZoomLevel = minZoomLevel;
    }

    public final int getMaxZoomLevel() {
        return maxZoomLevel;
    }

    public final void setMaxZoomLevel(int maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    private void viewportTransformChanged() {
        final double originX = getMap().getMapOrigin().getX();

        if (tileGrid == null || Math.abs(originX - mapOriginX) > 180d) {
            // immediately handle map origin leap when map center moves across 180° longitude
            updateTileGrid();

        } else {
            setTransform();

            if (getUpdateWhileViewportChanging()) {
                updateTimeline.play();
            } else {
                updateTimeline.playFromStart();
            }
        }

        mapOriginX = originX;
    }

    private void updateTileGrid() {
        updateTimeline.stop();

        final MapBase map = getMap();

        if (map != null) {
            final Affine transform;

            try {
                transform = map.getViewportTransform().createInverse();
            } catch (NonInvertibleTransformException ex) {
                Logger.getLogger(MapBase.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            final int zoomLevel = Math.max(0, (int) Math.floor(map.getZoomLevel() + zoomLevelOffset));
            final double scale = (double) (1 << zoomLevel) / 360d;

            transform.prependTranslation(180d, -180d);
            transform.prependScale(scale, -scale);

            // tile indices of visible rectangle
            final Point2D p1 = transform.transform(new Point2D(0d, 0d));
            final Point2D p2 = transform.transform(new Point2D(map.getWidth(), 0d));
            final Point2D p3 = transform.transform(new Point2D(0d, map.getHeight()));
            final Point2D p4 = transform.transform(new Point2D(map.getWidth(), map.getHeight()));

            final TileGrid grid = new TileGrid(zoomLevel,
                    (int) Math.floor(Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()))),
                    (int) Math.floor(Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()))),
                    (int) Math.floor(Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()))),
                    (int) Math.floor(Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()))));

            if (!grid.equals(tileGrid)) {
                tileGrid = grid;
                setTransform();
                updateTiles(false);
            }
        } else {
            tileGrid = null;
            updateTiles(true);
        }
    }

    private void setTransform() {
        final MapBase map = getMap();
        final double scale = Math.pow(2d, map.getZoomLevel() - tileGrid.getZoomLevel());
        final double offsetX = map.getViewportOrigin().getX() - (180d + map.getMapOrigin().getX()) * map.getViewportScale();
        final double offsetY = map.getViewportOrigin().getY() - (180d - map.getMapOrigin().getY()) * map.getViewportScale();

        final Affine transform = new Affine();
        transform.prependTranslation(TileSource.TILE_SIZE * tileGrid.getXMin(), TileSource.TILE_SIZE * tileGrid.getYMin());
        transform.prependScale(scale, scale);
        transform.prependTranslation(offsetX, offsetY);
        transform.prependRotation(map.getHeading(), map.getViewportOrigin());

        getTransforms().set(0, transform);
    }

    private void updateTiles(boolean clearTiles) {
        if (!tiles.isEmpty()) {
            tileImageLoader.cancelLoadTiles(this);
        }

        if (clearTiles) {
            tiles.clear();
        }

        final MapBase map = getMap();
        final ArrayList<Tile> newTiles = new ArrayList<>();

        if (tileGrid != null && map != null && getTileSource() != null) {
            final int maxZoom = Math.min(tileGrid.getZoomLevel(), maxZoomLevel);
            int minZoom = minZoomLevel;

            if (minZoom < maxZoom && this != map.getChildrenUnmodifiable().stream().findFirst().orElse(null)) {
                // do not load background tiles if this is not the base layer
                minZoom = maxZoom;
            }

            for (int tz = minZoom; tz <= maxZoom; tz++) {
                final int tileSize = 1 << (tileGrid.getZoomLevel() - tz);
                final int x1 = (int) Math.floor((double) tileGrid.getXMin() / tileSize); // may be negative
                final int x2 = tileGrid.getXMax() / tileSize;
                final int y1 = Math.max(tileGrid.getYMin() / tileSize, 0);
                final int y2 = Math.min(tileGrid.getYMax() / tileSize, (1 << tz) - 1);

                for (int ty = y1; ty <= y2; ty++) {
                    for (int tx = x1; tx <= x2; tx++) {
                        final int z = tz;
                        final int x = tx;
                        final int y = ty;
                        Tile tile = tiles.stream()
                                .filter(t -> t.getZoomLevel() == z && t.getX() == x && t.getY() == y)
                                .findAny().orElse(null);

                        if (tile == null) {
                            tile = new Tile(z, x, y);
                            final int xIndex = tile.getXIndex();

                            Tile equivalentTile = tiles.stream()
                                    .filter(t -> t.getZoomLevel() == z && t.getXIndex() == xIndex && t.getY() == y && t.getImage() != null)
                                    .findAny().orElse(null);

                            if (equivalentTile != null) {
                                tile.setImage(equivalentTile.getImage(), false);
                            }
                        }

                        newTiles.add(tile);
                    }
                }
            }
        }

        tiles = newTiles;

        if (tiles.isEmpty()) {
            getChildren().clear();

        } else {
            getChildren().setAll(tiles.stream()
                    .map(tile -> {
                        final ImageView imageView = tile.getImageView();
                        final int size = TileSource.TILE_SIZE << (tileGrid.getZoomLevel() - tile.getZoomLevel());
                        imageView.setX(size * tile.getX() - TileSource.TILE_SIZE * tileGrid.getXMin());
                        imageView.setY(size * tile.getY() - TileSource.TILE_SIZE * tileGrid.getYMin());
                        imageView.setFitWidth(size);
                        imageView.setFitHeight(size);
                        return imageView;
                    })
                    .collect(Collectors.toList()));

            tileImageLoader.beginLoadTiles(this, tiles.stream()
                    .filter(tile -> tile.isPending())
                    .collect(Collectors.toList()));
        }
    }
}
