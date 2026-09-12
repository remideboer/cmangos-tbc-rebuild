package org.tbc.world.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * AreaTable.dbc. CMaNGOS GetAreaIdByAreaFlag / GetZoneIdByAreaFlag: maps store
 * exploreFlag; game_graveyard_zone uses AreaTable ID and parent zone.
 */
public final class AreaTable {
    private static final Logger log = LoggerFactory.getLogger(AreaTable.class);

    /** AreaTable.dbc Goldshire (parent Elwynn Forest 12). */
    public static final int GOLDSHIRE = 87;
    /** AreaTable.dbc / playercreateinfo Elwynn Forest. */
    public static final int ELWYNN_FOREST = 12;
    /** AreaTable.dbc Dun Morogh. */
    public static final int DUN_MOROGH = 1;

    public record Entry(int id, int parentZone, int exploreFlag) {
        int zoneOrSelf() {
            return parentZone != 0 ? parentZone : id;
        }
    }

    private final Map<Integer, Entry> byId = new HashMap<>();
    private final Map<Integer, Entry> byFlag = new HashMap<>();

    public static AreaTable seeded() {
        AreaTable t = new AreaTable();
        t.add(ELWYNN_FOREST, 0, 0);
        t.add(GOLDSHIRE, ELWYNN_FOREST, 0);
        t.add(DUN_MOROGH, 0, 0);
        return t;
    }

    public void add(int id, int parentZone, int exploreFlag) {
        Entry e = new Entry(id, parentZone, exploreFlag);
        byId.put(id, e);
        if (exploreFlag != 0) {
            byFlag.put(exploreFlag, e);
        }
    }

    public int areaId(int flagOrId) {
        Entry e = lookup(flagOrId);
        return e == null ? 0 : e.id;
    }

    public int zoneId(int flagOrId) {
        Entry e = lookup(flagOrId);
        return e == null ? 0 : e.zoneOrSelf();
    }

    public void loadFromDataDir(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        Path file = dataDir.resolve("dbc").resolve("AreaTable.dbc");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            int n = 0;
            for (int[] row : dbc.records) {
                if (row.length < 4 || row[0] == 0) {
                    continue;
                }
                add(row[0], row[2], row[3]);
                n++;
            }
            log.info("AreaTable {} rows from {}", n, file);
        } catch (Exception e) {
            log.warn("AreaTable load failed: {}", e.getMessage());
        }
    }

    private Entry lookup(int flagOrId) {
        if (flagOrId == 0) {
            return null;
        }
        Entry e = byFlag.get(flagOrId);
        return e != null ? e : byId.get(flagOrId);
    }
}
