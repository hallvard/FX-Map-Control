/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

import java.util.List;

public record WmtsTileMatrixSet(
    String identifier,
    String supportedCrs,
    List<WmtsTileMatrix> tileMatrixes
 ) {

    public WmtsTileMatrixSet {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("identifier must not be null or empty.");
        }

        if (supportedCrs == null || supportedCrs.isEmpty()) {
            throw new IllegalArgumentException("supportedCrs must not be null or empty.");
        }

        if (tileMatrixes == null || tileMatrixes.isEmpty()) {
            throw new IllegalArgumentException("tileMatrixes must not be null or an empty collection.");
        }
        tileMatrixes = tileMatrixes.stream()
            .sorted((m1, m2) -> m1.scale() < m2.scale() ? -1 : 1)
            .toList();
    }
}
