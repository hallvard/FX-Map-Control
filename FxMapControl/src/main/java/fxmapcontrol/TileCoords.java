package fxmapcontrol;

public record TileCoords(int x, int y, int zoomLevel) {
  public final int xIndex() {
    int numTiles = 1 << zoomLevel();
    return ((x % numTiles) + numTiles) % numTiles;
  }
}
