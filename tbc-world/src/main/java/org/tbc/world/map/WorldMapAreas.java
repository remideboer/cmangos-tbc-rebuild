package org.tbc.world.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Named WorldMapArea catalog for quest-editor region pick. */
public final class WorldMapAreas {
    private static final Logger log = LoggerFactory.getLogger(WorldMapAreas.class);

    /** 2.4.3 Elwynn loc* (Goldshire −9465, 62). */
    public static final WorldMapArea ELWYNN = new WorldMapArea(
            AreaTable.ELWYNN_FOREST, 0, "Elwynn", "Elwynn Forest",
            1535.42f, -3766.05f, -7943.22f, -11176.3f);
    /** 2.4.3 Eversong loc* (Sunstrider 10349.6, −6357.29). */
    public static final WorldMapArea EVERSONG = new WorldMapArea(
            AreaTable.EVERSONG_WOODS, 530, "EversongWoods", "Eversong Woods",
            3083.96f, -10133.8f, 14848.4f, 5351.3f);

    private final Map<Long, WorldMapArea> byKey = new HashMap<>();
    private final Map<Integer, WorldMapArea> byAreaId = new HashMap<>();

    public static WorldMapAreas seeded() {
        WorldMapAreas c = new WorldMapAreas();
        c.put(new WorldMapArea(0, 0, "Azeroth", "Eastern Kingdoms",
                4230f, -5040f, 14870f, -16000f));
        c.put(new WorldMapArea(0, 1, "Kalimdor", "Kalimdor",
                11700f, -19800f, 12700f, -8500f));
        c.put(new WorldMapArea(0, 530, "Expansion01", "Outland",
                4000f, -12000f, 16000f, 2000f));
        c.put(ELWYNN);
        c.put(EVERSONG);
        return c;
    }

    public static WorldMapAreas fromDbc(Path dataDir) {
        WorldMapAreas c = seeded();
        if (dataDir == null) {
            return c;
        }
        Path wma = dataDir.resolve("dbc").resolve("WorldMapArea.dbc");
        if (!Files.isRegularFile(wma)) {
            return c;
        }
        try {
            Map<Integer, String> areaNames = areaNames(dataDir);
            Map<Integer, String> mapNames = mapNames(dataDir);
            DbcFile dbc = DbcFile.load(wma);
            int n = 0;
            for (int[] row : dbc.records) {
                if (row.length < 8) {
                    continue;
                }
                int mapId = row[1];
                int areaId = row[2];
                String internal = dbc.str(row[3]);
                float locLeft = Float.intBitsToFloat(row[4]);
                float locRight = Float.intBitsToFloat(row[5]);
                float locTop = Float.intBitsToFloat(row[6]);
                float locBottom = Float.intBitsToFloat(row[7]);
                WorldMapArea area = new WorldMapArea(areaId, mapId, internal,
                        displayName(areaId, mapId, internal, areaNames, mapNames),
                        locLeft, locRight, locTop, locBottom);
                if (area.degenerate()) {
                    continue;
                }
                c.put(area);
                n++;
            }
            log.info("WorldMapArea {} rows from {}", n, wma);
        } catch (Exception e) {
            log.warn("WorldMapArea load failed: {}", e.getMessage());
        }
        return c;
    }

    public void put(WorldMapArea area) {
        if (area == null || area.degenerate()) {
            return;
        }
        byKey.put(key(area.mapId(), area.areaId()), area);
        if (area.areaId() != 0) {
            byAreaId.put(area.areaId(), area);
        }
    }

    public WorldMapArea byAreaId(int areaId) {
        return byAreaId.get(areaId);
    }

    public WorldMapArea byMapAndArea(int mapId, int areaId) {
        return byKey.get(key(mapId, areaId));
    }

    public List<WorldMapArea> list() {
        List<WorldMapArea> out = new ArrayList<>(byKey.values());
        out.sort(Comparator.comparing(WorldMapArea::displayName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public WorldMapArea byDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (WorldMapArea a : list()) {
            if (name.equalsIgnoreCase(a.displayName()) || name.equalsIgnoreCase(a.internalName())) {
                return a;
            }
        }
        return null;
    }

    private static long key(int mapId, int areaId) {
        return (((long) mapId) << 32) | (areaId & 0xFFFFFFFFL);
    }

    private static String displayName(int areaId, int mapId, String internal,
                                      Map<Integer, String> areaNames, Map<Integer, String> mapNames) {
        if (areaId == 0) {
            String map = mapNames.get(mapId);
            if (map != null && !map.isBlank()) {
                return map;
            }
        } else {
            String area = areaNames.get(areaId);
            if (area != null && !area.isBlank()) {
                return area;
            }
        }
        if (internal != null && !internal.isBlank()) {
            return internal;
        }
        return "Map " + mapId + " area " + areaId;
    }

    private static Map<Integer, String> areaNames(Path dataDir) {
        Map<Integer, String> names = new HashMap<>();
        Path file = dataDir.resolve("dbc").resolve("AreaTable.dbc");
        if (!Files.isRegularFile(file)) {
            return names;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            for (int[] row : dbc.records) {
                if (row.length > 11 && row[0] != 0) {
                    names.put(row[0], dbc.str(row[11]));
                }
            }
        } catch (Exception e) {
            log.warn("AreaTable names failed: {}", e.getMessage());
        }
        return names;
    }

    private static Map<Integer, String> mapNames(Path dataDir) {
        Map<Integer, String> names = new HashMap<>();
        Path file = dataDir.resolve("dbc").resolve("Map.dbc");
        if (!Files.isRegularFile(file)) {
            return names;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            for (int[] row : dbc.records) {
                if (row.length > 4) {
                    names.put(row[0], dbc.str(row[4]));
                }
            }
        } catch (Exception e) {
            log.warn("Map names failed: {}", e.getMessage());
        }
        return names;
    }
}
