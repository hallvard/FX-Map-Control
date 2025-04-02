/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * Displays map tiles from a Web Map Tile Service (WMTS).
 */
public class WmtsTileLayer extends MapTileLayerBase {

    private final StringProperty capabilitiesUrlProperty = new SimpleStringProperty(this, "capabilitiesUrl");
    private final StringProperty layerIdentiferProperty = new SimpleStringProperty(this, "layerIdentifer");
    private final Map<String, WmtsTileMatrixSet> tileMatrixSets = new HashMap<>();

    public WmtsTileLayer(ITileImageLoader tileImageLoader) {
        super(tileImageLoader);
        getStyleClass().add("wmts-tile-layer");
        capabilitiesUrlProperty.addListener((observable, oldValue, newValue) -> tileMatrixSets.clear());
    }

    public WmtsTileLayer() {
        this(new TileImageLoader());
    }

    public WmtsTileLayer(String name, String capabilitiesUrl) {
        this();
        setName(name);
        setCapabilitiesUrl(capabilitiesUrl);
    }

    public WmtsTileLayer(String name, String capabilitiesUrl, String layerIdentifier) {
        this(name, capabilitiesUrl);
        setLayerIdentifier(layerIdentifier);
    }

    public final StringProperty capabilitiesUrlProperty() {
        return capabilitiesUrlProperty;
    }

    public final String getCapabilitiesUrl() {
        return capabilitiesUrlProperty.get();
    }

    public final void setCapabilitiesUrl(String capabilitiesUrl) {
        capabilitiesUrlProperty.set(capabilitiesUrl);
    }

    public final StringProperty layerIdentiferProperty() {
        return layerIdentiferProperty;
    }

    public final String getLayerIdentifier() {
        return layerIdentiferProperty.get();
    }

    public final void setLayerIdentifier(String layerIdentifer) {
        layerIdentiferProperty.set(layerIdentifer);
    }

    @Override
    public void setMap(MapBase map) {
        super.setMap(map);

        if (map != null
                && tileMatrixSets.isEmpty()
                && getCapabilitiesUrl() != null
                && !getCapabilitiesUrl().isEmpty()) {

            new CapabilitiesService().start();
        }
    }

    @Override
    protected void setTransform() {
        ViewTransform viewTransform = getMap().getViewTransform();

        getChildren().stream()
                .map(node -> (WmtsTileMatrixLayer) node)
                .forEach(layer -> layer.setTransform(viewTransform));
    }

    @Override
    protected void updateTileLayer() {
        getUpdateTimeline().stop();

        MapBase map = getMap();
        WmtsTileMatrixSet tileMatrixSet;

        if (map == null
                || (tileMatrixSet = tileMatrixSets.get(map.getProjection().getCrsId())) == null) {
            getChildren().clear();
            updateTiles(null);

        } else if (updateChildLayers(tileMatrixSet)) {
            setTransform();
            updateTiles(tileMatrixSet);
        }
    }

    private boolean updateChildLayers(WmtsTileMatrixSet tileMatrixSet) {
        MapBase map = getMap();
        boolean layersChanged = false;
        double maxScale = 1.001 * map.getViewTransform().getScale(); // avoid rounding issues

        // show all TileMatrix layers with Scale <= maxScale, at least the first layer
        //
        List<WmtsTileMatrix> tileMatrixes = tileMatrixSet.tileMatrixes();
        List<WmtsTileMatrix> currentMatrixes = tileMatrixes.stream()
                .filter(matrix -> matrix.scale() <= maxScale)
                .toList();

        if (currentMatrixes.isEmpty() && !tileMatrixes.isEmpty()) {
            currentMatrixes.add(tileMatrixes.get(0));
        }

        if (this != map.getChildrenUnmodifiable().stream().findFirst().orElse(null)) { // no background tiles
            currentMatrixes = currentMatrixes.stream()
                    .skip(currentMatrixes.size() - 1)
                    .toList(); // last element only

        } else if (currentMatrixes.size() > getMaxBackgroundLevels() + 1) {
            currentMatrixes = currentMatrixes.stream()
                    .skip(currentMatrixes.size() - getMaxBackgroundLevels() - 1)
                    .toList();
        }

        List<WmtsTileMatrix> layerMatrixes = currentMatrixes; // final...

        List<WmtsTileMatrixLayer> currentLayers = getChildren().stream()
                .map(node -> (WmtsTileMatrixLayer) node)
                .filter(layer -> layerMatrixes.contains(layer.getTileMatrix()))
                .toList();

        getChildren().clear();

        for (WmtsTileMatrix tileMatrix : layerMatrixes) {
            WmtsTileMatrixLayer layer = currentLayers.stream()
                    .filter(l -> l.getTileMatrix() == tileMatrix)
                    .findFirst().orElse(null);

            if (layer == null) {
                layer = new WmtsTileMatrixLayer(tileMatrix, tileMatrixSet.tileMatrixes().indexOf(tileMatrix));
                layersChanged = true;
            }

            if (layer.setBounds(map.getViewTransform(), map.getWidth(), map.getHeight())) {
                layersChanged = true;
            }

            getChildren().add(layer);
        }

        return layersChanged;
    }

    private void updateTiles(WmtsTileMatrixSet tileMatrixSet) {
        List<Tile> tiles = new ArrayList<>();

        getChildren().stream()
                .map(node -> (WmtsTileMatrixLayer) node)
                .forEach(layer -> tiles.addAll(layer.updateTiles()));

        WmtsTileSource tileSource = (WmtsTileSource) getTileSource();
        String sourceName = getName();

        if (tileSource != null && tileMatrixSet != null) {
            tileSource.setTileMatrixSet(tileMatrixSet);

            if (sourceName != null && !sourceName.isEmpty()) {
                sourceName += "/" + tileMatrixSet.identifier();
            }
        }

        getTileImageLoader().loadTiles(tiles, tileSource, sourceName);
    }

    private class CapabilitiesService extends Service<WmtsCapabilities> {

        @Override
        protected void succeeded() {
            WmtsCapabilities capabilities = getValue();
            if (capabilities != null) {
                setTileSource(capabilities.tileSource());
                setLayerIdentifier(capabilities.layerIdentifier());

                tileMatrixSets.putAll(capabilities.tileMatrixSets().stream()
                        .collect(Collectors.toMap(s -> s.supportedCrs(), s -> s)));

                updateTileLayer();
            }
        }

        @Override
        protected void failed() {
            Logger.getLogger(WmtsTileLayer.class.getName()).log(
                    Level.WARNING, "{0}: {1}", new Object[]{getCapabilitiesUrl(), getException()});
        }

        @Override
        protected Task<WmtsCapabilities> createTask() {
            return new Task<WmtsCapabilities>() {
                @Override
                protected WmtsCapabilities call() throws Exception {
                    return WmtsCapabilities.readCapabilities(getCapabilitiesUrl(), getLayerIdentifier());
                }
            };
        }
    }
}
