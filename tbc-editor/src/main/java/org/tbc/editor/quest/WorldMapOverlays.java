package org.tbc.editor.quest;

import org.tbc.content.dbc.WdbcFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** WorldMapOverlay rows that the 8606 map frame stamps on a zone sheet. */
public final class WorldMapOverlays {
    public record Overlay(String texture, int width, int height, int offsetX, int offsetY) {}

    private static String cachedKey;
    private static WdbcFile cachedOverlays;
    private static Map<Integer, Integer> cachedParents = Map.of();

    private WorldMapOverlays() {}

    public static List<Overlay> forZone(Path dataDir, int zoneAreaId) {
        if (dataDir == null || zoneAreaId == 0) {
            return List.of();
        }
        String key = dataDir.toAbsolutePath().normalize().toString();
        if (!key.equals(cachedKey)) {
            cachedKey = key;
            cachedOverlays = null;
            cachedParents = Map.of();
            byte[] overlayBytes = WorldMapBlp.readNamed(dataDir, "DBFilesClient\\WorldMapOverlay.dbc");
            byte[] areaBytes = WorldMapBlp.readNamed(dataDir, "DBFilesClient\\AreaTable.dbc");
            if (overlayBytes != null && areaBytes != null) {
                try {
                    cachedOverlays = WdbcFile.read(overlayBytes);
                    cachedParents = parents(WdbcFile.read(areaBytes));
                } catch (IOException ignored) {
                    cachedOverlays = null;
                    cachedParents = Map.of();
                }
            }
        }
        if (cachedOverlays == null) {
            return List.of();
        }
        return matching(cachedOverlays, cachedParents, zoneAreaId);
    }

    public static List<Overlay> matching(WdbcFile overlays, Map<Integer, Integer> parentByArea, int zoneAreaId) {
        if (overlays == null || zoneAreaId == 0) {
            return List.of();
        }
        Map<Integer, Integer> parents = parentByArea == null ? Map.of() : parentByArea;
        List<Overlay> out = new ArrayList<>();
        for (int[] row : overlays.records()) {
            if (row.length < 13) {
                continue;
            }
            boolean hit = false;
            for (int i = 2; i <= 5 && i < row.length; i++) {
                if (row[i] != 0 && belongs(row[i], parents, zoneAreaId)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                continue;
            }
            String texture = overlays.str(row[8]);
            if (texture.isBlank() || row[9] <= 0 || row[10] <= 0) {
                continue;
            }
            out.add(new Overlay(texture, row[9], row[10], row[11], row[12]));
        }
        return List.copyOf(out);
    }

    private static Map<Integer, Integer> parents(WdbcFile areaTable) {
        Map<Integer, Integer> parents = new HashMap<>();
        for (int[] row : areaTable.records()) {
            if (row.length > 2 && row[0] != 0) {
                parents.put(row[0], row[2]);
            }
        }
        return parents;
    }

    private static boolean belongs(int areaId, Map<Integer, Integer> parents, int zoneAreaId) {
        int id = areaId;
        for (int hop = 0; hop < 8 && id > 0; hop++) {
            if (id == zoneAreaId) {
                return true;
            }
            Integer parent = parents.get(id);
            if (parent == null || parent == 0 || parent == id) {
                return false;
            }
            id = parent;
        }
        return false;
    }
}
