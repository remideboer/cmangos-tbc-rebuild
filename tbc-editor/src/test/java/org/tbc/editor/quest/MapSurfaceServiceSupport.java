package org.tbc.editor.quest;

import org.tbc.world.map.MapSurfaceService;

final class MapSurfaceServiceSupport {
    private MapSurfaceServiceSupport() {}

    static MapSurfaceService uniqueAdt(float z) {
        return MapSurfaceService.of((map, x, y) -> new float[]{z}, (map, x, y) -> new float[0]);
    }
}
