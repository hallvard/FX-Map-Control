/*
 * FX Map Control - https://github.com/ClemensFischer/FX-Map-Control
 * © 2020 Clemens Fischer
 */
package fxmapcontrol;

/**
 * Provides methods for caching tile image buffers.
 */
public interface ITileCache {

    public record CacheItem(
        byte[] buffer,
        long expiration // milliseconds since 1970/01/01 00:00:00 UTC
    ) {
    }

    CacheItem get(String key);

    void set(String key, byte[] buffer, long expiration);
}
