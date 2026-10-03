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
    /** AreaTable.dbc Eversong Woods (Blood Elf continent zone). */
    public static final int EVERSONG_WOODS = 3430;
    /** AreaTable.dbc Sunstrider Isle — playercreateinfo zone for race 10; parent Eversong. */
    public static final int SUNSTRIDER_ISLE = 3431;
    /** AreaTable.dbc Teldrassil — playercreateinfo zone for race 4. */
    public static final int TELDRASSIL = 141;
    /** AreaTable.dbc Shadowglen — Night Elf starter sub-area; parent Teldrassil. */
    public static final int SHADOWGLEN = 188;
    /** AreaTable.dbc Azuremyst Isle (Draenei continent zone). */
    public static final int AZUREMYST = 3524;
    /** AreaTable.dbc Ammen Vale — playercreateinfo zone for race 11; parent Azuremyst. */
    public static final int AMMEN_VALE = 3526;
    /** AreaTable.dbc / playercreateinfo Durotar. */
    public static final int DUROTAR = 14;
    /** AreaTable.dbc / playercreateinfo Tirisfal Glades. */
    public static final int TIRISFAL = 85;
    /** AreaTable.dbc / playercreateinfo Mulgore. */
    public static final int MULGORE = 215;

    public record Entry(int id, int parentZone, int exploreFlag) {
        int zoneOrSelf() {
            return parentZone != 0 ? parentZone : id;
        }
    }

    private final Map<Integer, Entry> byId = new HashMap<>();
    private final Map<Integer, Entry> byFlag = new HashMap<>();

    public static AreaTable seeded() {
        AreaTable t = new AreaTable();
        // exploreFlag placeholders until AreaTable.dbc loads (m_AreaBit); enable ZONEUPDATE uncover.
        t.add(ELWYNN_FOREST, 0, 10);
        t.add(GOLDSHIRE, ELWYNN_FOREST, 11);
        t.add(DUN_MOROGH, 0, 1);
        t.add(EVERSONG_WOODS, 0, 0);
        t.add(SUNSTRIDER_ISLE, EVERSONG_WOODS, 0);
        // AreaBit from AreaTable.dbc (Shadowglen 561) so terrain.area → AREALINK without DataDir.
        t.add(TELDRASSIL, 0, 220);
        t.add(SHADOWGLEN, TELDRASSIL, 561);
        t.add(AZUREMYST, 0, 0);
        t.add(AMMEN_VALE, AZUREMYST, 0);
        t.add(DUROTAR, 0, 0);
        t.add(TIRISFAL, 0, 0);
        t.add(MULGORE, 0, 0);
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

    /** AreaTable.dbc exploreFlag (m_AreaBit) for an area id — 0 if unknown. */
    public int exploreFlag(int areaId) {
        if (areaId == 0) {
            return 0;
        }
        Entry e = byId.get(areaId);
        return e == null ? 0 : e.exploreFlag;
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
