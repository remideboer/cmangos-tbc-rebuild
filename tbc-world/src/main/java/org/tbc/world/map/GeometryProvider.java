package org.tbc.world.map;

/**
 * Supplies candidate floor heights at a world XY. Empty array = hole / no surface.
 * Used by {@link SurfaceQuery} (injectable in tests; Terrain/VMap in production).
 */
@FunctionalInterface
public interface GeometryProvider {
    float[] floors(int mapId, float x, float y);
}
