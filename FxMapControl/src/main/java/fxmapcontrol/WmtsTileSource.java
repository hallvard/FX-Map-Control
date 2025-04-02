/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

public class WmtsTileSource extends TileSource {

    private WmtsTileMatrixSet tileMatrixSet;

    public WmtsTileSource(String urlFormat) {
        super(urlFormat);
    }

    public WmtsTileMatrixSet getTileMatrixSet() {
        return tileMatrixSet;
    }

    public void setTileMatrixSet(WmtsTileMatrixSet tileMatrixSet) {
        this.tileMatrixSet = tileMatrixSet;
    }

    @Override
    protected String getUrl(int x, int y, int zoomLevel) {
        String url = null;

        if (tileMatrixSet != null && zoomLevel >= 0 && zoomLevel < tileMatrixSet.tileMatrixes().size()) {
            url = getUrlFormat()
                    .replace("{TileMatrixSet}", tileMatrixSet.identifier())
                    .replace("{TileMatrix}", tileMatrixSet.tileMatrixes().get(zoomLevel).identifier())
                    .replace("{TileCol}", Integer.toString(x))
                    .replace("{TileRow}", Integer.toString(y));
        }

        return url;
    }
}
