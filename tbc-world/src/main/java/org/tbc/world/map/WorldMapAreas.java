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

    /** 2.4.3 WorldMapArea.dbc Elwynn. Goldshire (−9465, 62) is about 42% across and 66% down. */
    public static final WorldMapArea ELWYNN = new WorldMapArea(
            AreaTable.ELWYNN_FOREST, 0, "Elwynn", "Elwynn Forest",
            1535.4166f, -1935.4166f, -7939.583f, -10254.166f);
    /** 2.4.3 WorldMapArea.dbc Eversong. The create point is about 38% across and 21% down. */
    public static final WorldMapArea EVERSONG = new WorldMapArea(
            AreaTable.EVERSONG_WOODS, 530, "EversongWoods", "Eversong Woods",
            -4487.5f, -9412.5f, 11041.666f, 7758.333f);
    /**
     * Sunstrider Isle (area 3431) is not a WorldMapArea.dbc row. These loc* are the
     * Eversong sheet pixels (187, 0)–(715, 557), the WorldMapOverlay 1127 window
     * (512×512 at 195, 5). The editor refits this from the loaded Eversong row.
     */
    public static final WorldMapArea SUNSTRIDER = new WorldMapArea(
            AreaTable.SUNSTRIDER_ISLE, 530, "SunstriderIsle", "Sunstrider Isle",
            -5386.8896f, -7926.343f, 11041.666f, 8660.395f);

    private final Map<Long, WorldMapArea> byKey = new HashMap<>();
    private final Map<Integer, WorldMapArea> byAreaId = new HashMap<>();

    public static WorldMapAreas seeded() {
        WorldMapAreas c = new WorldMapAreas();
        c.put(new WorldMapArea(0, 0, "Azeroth", "Eastern Kingdoms",
                18171.97f, -22569.21f, 11176.344f, -15973.344f));
        c.put(new WorldMapArea(0, 1, "Kalimdor", "Kalimdor",
                17066.6f, -19733.21f, 12799.9f, -11733.3f));
        c.put(new WorldMapArea(0, 530, "Expansion01", "Outland",
                12996.039f, -4468.039f, 5821.3594f, -5821.3594f));
        c.put(ELWYNN);
        c.put(EVERSONG);
        c.put(SUNSTRIDER);
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
            int n = c.ingest(DbcFile.load(wma), areaNames(dataDir), mapNames(dataDir));
            log.info("WorldMapArea {} rows from {}", n, wma);
        } catch (Exception e) {
            log.warn("WorldMapArea load failed: {}", e.getMessage());
        }
        return c;
    }

    /**
     * Adds WorldMapArea rows parsed from client DBC bytes (MPQ or a loose file).
     * Null world-map bytes add nothing. Name tables are optional.
     */
    public int addClientDbcs(byte[] worldMapArea, byte[] areaTable, byte[] mapDbc) {
        if (worldMapArea == null) {
            return 0;
        }
        try {
            return ingest(DbcFile.read(worldMapArea), areaNames(areaTable), mapNames(mapDbc));
        } catch (Exception e) {
            log.warn("WorldMapArea client bytes failed: {}", e.getMessage());
            return 0;
        }
    }

    private int ingest(DbcFile dbc, Map<Integer, String> areaNames, Map<Integer, String> mapNames) {
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
            put(area);
            n++;
        }
        return n;
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
        return areaNames(readIfPresent(dataDir, "AreaTable.dbc"));
    }

    private static Map<Integer, String> areaNames(byte[] file) {
        Map<Integer, String> names = new HashMap<>();
        if (file == null) {
            return names;
        }
        try {
            DbcFile dbc = DbcFile.read(file);
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
        return mapNames(readIfPresent(dataDir, "Map.dbc"));
    }

    private static Map<Integer, String> mapNames(byte[] file) {
        Map<Integer, String> names = new HashMap<>();
        if (file == null) {
            return names;
        }
        try {
            DbcFile dbc = DbcFile.read(file);
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

    private static byte[] readIfPresent(Path dataDir, String name) {
        if (dataDir == null) {
            return null;
        }
        Path file = dataDir.resolve("dbc").resolve(name);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readAllBytes(file);
        } catch (Exception e) {
            log.warn("{} read failed: {}", name, e.getMessage());
            return null;
        }
    }
}
