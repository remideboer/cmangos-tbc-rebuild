package org.tbc.world.combat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** FactionTemplate.dbc store. Seed covers in-memory tests; DataDir overwrites. */
public final class Factions {
    private static final Logger log = LoggerFactory.getLogger(Factions.class);
    private final Map<Integer, FactionTemplate> byId = new HashMap<>();

    public static Factions seeded() {
        Factions f = new Factions();
        f.seed();
        return f;
    }

    public FactionTemplate get(int id) {
        return byId.get(id);
    }

    public void add(FactionTemplate t) {
        if (t != null) {
            byId.put(t.id, t);
        }
    }

    public int reaction(Unit a, Unit b) {
        FactionTemplate fa = template(a);
        FactionTemplate fb = template(b);
        if (fa == null || fb == null) {
            return FactionTemplate.REP_NEUTRAL;
        }
        return fa.reactionTo(fb);
    }

    public boolean isHostile(Unit a, Unit b) {
        return reaction(a, b) < FactionTemplate.REP_UNFRIENDLY;
    }

    public boolean isFriend(Unit a, Unit b) {
        return reaction(a, b) >= FactionTemplate.REP_FRIENDLY;
    }

    public FactionTemplate template(Unit u) {
        if (u == null) {
            return null;
        }
        int id = u.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_FACTIONTEMPLATE);
        if (id == 0) {
            id = u.faction;
        }
        return byId.get(id);
    }

    public void loadFromDataDir(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        Path file = dataDir.resolve("dbc").resolve("FactionTemplate.dbc");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            int n = 0;
            for (int[] row : dbc.records) {
                if (row.length < 14) {
                    continue;
                }
                byId.put(row[0], new FactionTemplate(row[0], row[1], row[2], row[3], row[4], row[5],
                        new int[]{row[6], row[7], row[8], row[9]},
                        new int[]{row[10], row[11], row[12], row[13]}));
                n++;
            }
            log.info("FactionTemplate {} rows from {}", n, file);
        } catch (Exception e) {
            log.warn("FactionTemplate load failed: {}", e.getMessage());
        }
    }

    private void seed() {
        alliancePlayer(1, 1);
        alliancePlayer(3, 3);
        alliancePlayer(4, 4);
        alliancePlayer(115, 115);
        alliancePlayer(1629, 1629);
        hordePlayer(2, 2);
        hordePlayer(5, 5);
        hordePlayer(6, 6);
        hordePlayer(116, 116);
        hordePlayer(1610, 1610);
        monster(7, 7);
        monster(14, 14);
        monster(16, 16);
        monster(21, 21);
        allianceNpc(12, 72);
        // FactionTemplate.dbc 2.4.3 rows used by starter-zone SQL (overwritten when DataDir loads).
        put(25, 25, 0, FactionTemplate.GROUP_MONSTER, 0, 0, new int[4], new int[]{25, 0, 0, 0});
        put(32, 29, 16, 0, 0, 0, new int[]{28, 0, 0, 0}, new int[4]);
        put(38, 29, 17, FactionTemplate.GROUP_MONSTER, 0, FactionTemplate.GROUP_PLAYER,
                new int[]{28, 0, 0, 0}, new int[]{29, 0, 0, 0});
    }

    private void alliancePlayer(int templateId, int factionId) {
        put(templateId, factionId, 0, FactionTemplate.GROUP_PLAYER | FactionTemplate.GROUP_ALLIANCE,
                FactionTemplate.GROUP_ALLIANCE, FactionTemplate.GROUP_HORDE | FactionTemplate.GROUP_MONSTER);
    }

    private void hordePlayer(int templateId, int factionId) {
        put(templateId, factionId, 0, FactionTemplate.GROUP_PLAYER | FactionTemplate.GROUP_HORDE,
                FactionTemplate.GROUP_HORDE, FactionTemplate.GROUP_ALLIANCE | FactionTemplate.GROUP_MONSTER);
    }

    private void monster(int templateId, int factionId) {
        put(templateId, factionId, 0, FactionTemplate.GROUP_MONSTER, 0, FactionTemplate.GROUP_PLAYER);
    }

    private void allianceNpc(int templateId, int factionId) {
        put(templateId, factionId, 0, FactionTemplate.GROUP_ALLIANCE,
                FactionTemplate.GROUP_PLAYER | FactionTemplate.GROUP_ALLIANCE, FactionTemplate.GROUP_HORDE);
    }

    private void put(int id, int faction, int flags, int group, int friendGroup, int enemyGroup) {
        put(id, faction, flags, group, friendGroup, enemyGroup, new int[4], new int[4]);
    }

    private void put(int id, int faction, int flags, int group, int friendGroup, int enemyGroup,
            int[] enemyFaction, int[] friendFaction) {
        byId.put(id, new FactionTemplate(id, faction, flags, group, friendGroup, enemyGroup, enemyFaction, friendFaction));
    }
}
