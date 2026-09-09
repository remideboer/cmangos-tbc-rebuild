package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * CMaNGOS ObjectMgr::GetPlayerClassLevelInfo / GetPlayerLevelInfo — tbc-db `player_classlevelstats`
 * (class, level → basehp, basemana) and `player_levelstats` (race, class, level → str/agi/sta/inte/spi).
 * create-self.md "Stats (level 1)". In-memory worlds carry the level-1 rows for the starter races.
 */
public final class LevelStats {
    private static final Logger log = LoggerFactory.getLogger(LevelStats.class);

    public record ClassLevel(int baseHealth, int baseMana) {}

    public record Stats(int str, int agi, int sta, int inte, int spi) {
        public int stat(int index) {
            return switch (index) {
                case 0 -> str;
                case 1 -> agi;
                case 2 -> sta;
                case 3 -> inte;
                default -> spi;
            };
        }
    }

    private final Map<Integer, ClassLevel> classLevels = new HashMap<>();
    private final Map<Integer, Stats> raceClassLevels = new HashMap<>();
    private final Map<Integer, Integer> xpForLevel = new HashMap<>();
    /** gtOCTRegenHP / gtRegenHPPerSpt / gtRegenMPPerSpt ratios keyed by class+level (GT_MAX_LEVEL 100 rows per class). */
    private final Map<Integer, Float> hpRegenBase = new HashMap<>();
    private final Map<Integer, Float> hpRegenMore = new HashMap<>();
    private final Map<Integer, Float> mpRegenPerSpirit = new HashMap<>();
    private static final int GT_MAX_LEVEL = 100;

    private static final int[] GT_CLASSES = {1, 2, 3, 4, 5, 7, 8, 9, 11};
    private static final float[] GT_OCT_REGEN_HP_L1 =
        {0.131579f, 0.092593f, 0.089286f, 0.121951f, 0.119048f, 0.086207f, 0.079365f, 0.080645f, 0.081967f};
    private static final float[] GT_REGEN_HP_PER_SPT_L1 =
        {0.5f, 0.125f, 0.125f, 0.333333f, 0.041667f, 0.071429f, 0.041667f, 0.045455f, 0.0625f};
    private static final float[] GT_REGEN_MP_PER_SPT_L1 =
        {0f, 0.034965f, 0.034965f, 0f, 0.034965f, 0.034965f, 0.034965f, 0.034965f, 0.034965f};

    /** tbc-db player_xp_for_level 1..69 (XP needed to leave that level; level 70 is the cap). */
    private static final int[] XP_FOR_LEVEL = {
        400, 900, 1400, 2100, 2800, 3600, 4500, 5400, 6500, 7600, 8700, 9800, 11000, 12300, 13600, 15000,
        16400, 17800, 19300, 20800, 22400, 24000, 25500, 27200, 28900, 30500, 32200, 33900, 36300, 38800,
        41600, 44600, 48000, 51400, 55000, 58700, 62400, 66200, 70200, 74300, 78500, 82800, 87100, 91600,
        96300, 101000, 105800, 110700, 115700, 120900, 126100, 131500, 137000, 142500, 148200, 154000,
        159900, 165800, 172000, 494000, 574700, 614400, 650300, 682300, 710200, 734100, 753700, 768900, 779700,
    };

    private static LevelStats defaults;

    /** Shared seeded instance for callers without an ObjectMgr (null-mgr create/load paths). */
    public static synchronized LevelStats defaults() {
        if (defaults == null) {
            LevelStats ls = new LevelStats();
            ls.seedDefaults();
            defaults = ls;
        }
        return defaults;
    }

    /** Level-1 rows from tbc-db mangos.sql so an in-memory world creates real characters. */
    public void seedDefaults() {
        for (int i = 0; i < XP_FOR_LEVEL.length; i++) {
            xpForLevel.put(i + 1, XP_FOR_LEVEL[i]);
        }
        for (int i = 0; i < GT_CLASSES.length; i++) {
            hpRegenBase.put(classKey(GT_CLASSES[i], 1), GT_OCT_REGEN_HP_L1[i]);
            hpRegenMore.put(classKey(GT_CLASSES[i], 1), GT_REGEN_HP_PER_SPT_L1[i]);
            mpRegenPerSpirit.put(classKey(GT_CLASSES[i], 1), GT_REGEN_MP_PER_SPT_L1[i]);
        }
        putClassLevel(1, 1, 20, 0);
        putClassLevel(2, 1, 28, 60);
        putClassLevel(3, 1, 46, 65);
        putClassLevel(4, 1, 25, 0);
        putClassLevel(5, 1, 52, 73);
        putClassLevel(7, 1, 37, 85);
        putClassLevel(8, 1, 32, 100);
        putClassLevel(9, 1, 23, 90);
        putClassLevel(11, 1, 44, 60);
        putStats(1, 1, 1, 23, 20, 22, 20, 20);
        putStats(1, 2, 1, 22, 20, 22, 20, 21);
        putStats(1, 4, 1, 21, 23, 21, 20, 20);
        putStats(1, 5, 1, 20, 20, 20, 22, 23);
        putStats(1, 8, 1, 20, 20, 20, 23, 22);
        putStats(1, 9, 1, 20, 20, 21, 22, 22);
        putStats(2, 1, 1, 26, 17, 24, 17, 23);
        putStats(2, 3, 1, 23, 20, 23, 17, 24);
        putStats(2, 4, 1, 24, 20, 23, 17, 23);
        putStats(2, 7, 1, 24, 17, 23, 18, 25);
        putStats(2, 9, 1, 23, 17, 23, 19, 25);
        putStats(3, 1, 1, 25, 16, 25, 19, 19);
        putStats(3, 2, 1, 24, 16, 25, 19, 20);
        putStats(3, 3, 1, 22, 19, 24, 19, 20);
        putStats(3, 4, 1, 23, 19, 24, 19, 19);
        putStats(3, 5, 1, 22, 16, 23, 21, 22);
        putStats(7, 1, 1, 18, 23, 21, 24, 20);
        putStats(7, 4, 1, 16, 26, 20, 24, 20);
        putStats(7, 8, 1, 15, 23, 19, 27, 22);
        putStats(7, 9, 1, 15, 23, 20, 26, 22);
    }

    public void load(Connection c) {
        try {
            PreparedStatement ps = c.prepareStatement("SELECT class, level, basehp, basemana FROM player_classlevelstats");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                putClassLevel(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4));
            }
            PreparedStatement ls = c.prepareStatement(
                    "SELECT race, class, level, str, agi, sta, inte, spi FROM player_levelstats");
            ResultSet lr = ls.executeQuery();
            while (lr.next()) {
                putStats(lr.getInt(1), lr.getInt(2), lr.getInt(3), lr.getInt(4), lr.getInt(5), lr.getInt(6),
                        lr.getInt(7), lr.getInt(8));
            }
            PreparedStatement xs = c.prepareStatement("SELECT lvl, xp_for_next_level FROM player_xp_for_level");
            ResultSet xr = xs.executeQuery();
            while (xr.next()) {
                xpForLevel.put(xr.getInt(1), xr.getInt(2));
            }
            log.info("loaded {} player_classlevelstats, {} player_levelstats, {} player_xp_for_level",
                    classLevels.size(), raceClassLevels.size(), xpForLevel.size());
        } catch (Exception e) {
            log.warn("player level stats load failed, using defaults: {}", e.getMessage());
        }
        if (classLevels.isEmpty()) {
            seedDefaults();
        }
    }

    /** basehp/basemana for the class at that level; missing rows fall back to the level-1 warrior row. */
    public ClassLevel classLevel(int clazz, int level) {
        ClassLevel cl = classLevels.get(classKey(clazz, level));
        if (cl == null) {
            cl = classLevels.get(classKey(clazz, 1));
        }
        return cl != null ? cl : new ClassLevel(20, 0);
    }

    /** str/agi/sta/inte/spi for race+class at that level; missing rows fall back to the level-1 row or 20s. */
    public Stats stats(int race, int clazz, int level) {
        Stats s = raceClassLevels.get(raceKey(race, clazz, level));
        if (s == null) {
            s = raceClassLevels.get(raceKey(race, clazz, 1));
        }
        return s != null ? s : new Stats(20, 20, 20, 20, 20);
    }

    /** DataDir/dbc gt*.dbc: one float per row, row = (class - 1) * 100 + level - 1. Missing files keep the seeds. */
    public void loadGt(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        loadGt(dataDir.resolve("dbc").resolve("gtOCTRegenHP.dbc"), hpRegenBase);
        loadGt(dataDir.resolve("dbc").resolve("gtRegenHPPerSpt.dbc"), hpRegenMore);
        loadGt(dataDir.resolve("dbc").resolve("gtRegenMPPerSpt.dbc"), mpRegenPerSpirit);
    }

    private static void loadGt(Path file, Map<Integer, Float> into) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            for (int row = 0; row < dbc.records.size(); row++) {
                into.put(classKey(row / GT_MAX_LEVEL + 1, row % GT_MAX_LEVEL + 1),
                        Float.intBitsToFloat(dbc.records.get(row)[0]));
            }
        } catch (Exception e) {
            log.warn("{} load failed: {}", file.getFileName(), e.getMessage());
        }
    }

    /** CMaNGOS Unit::OCTRegenHPPerSpirit: health per second out of combat from spirit (first 50 at base ratio). */
    public float hpRegenPerSpirit(int clazz, int level, int spirit) {
        float baseSpirit = Math.min(spirit, 50);
        float moreSpirit = spirit - baseSpirit;
        return baseSpirit * gt(hpRegenBase, clazz, level) + moreSpirit * gt(hpRegenMore, clazz, level);
    }

    /** CMaNGOS Unit::OCTRegenMPPerSpirit: spirit * ratio; UpdateManaRegen multiplies by sqrt(intellect). */
    public float manaRegenPerSpirit(int clazz, int level, int spirit) {
        return spirit * gt(mpRegenPerSpirit, clazz, level);
    }

    private static float gt(Map<Integer, Float> table, int clazz, int level) {
        Float v = table.get(classKey(clazz, level));
        if (v == null) {
            v = table.get(classKey(clazz, 1));
        }
        return v != null ? v : 0f;
    }

    /** CMaNGOS ObjectMgr::GetXPForLevel: XP needed to leave {@code level}; 0 when the table has no row (cap). */
    public int xpForLevel(int level) {
        return xpForLevel.getOrDefault(level, 0);
    }

    private void putClassLevel(int clazz, int level, int hp, int mana) {
        classLevels.put(classKey(clazz, level), new ClassLevel(hp, mana));
    }

    private void putStats(int race, int clazz, int level, int str, int agi, int sta, int inte, int spi) {
        raceClassLevels.put(raceKey(race, clazz, level), new Stats(str, agi, sta, inte, spi));
    }

    private static int classKey(int clazz, int level) {
        return (clazz << 8) | (level & 0xFF);
    }

    private static int raceKey(int race, int clazz, int level) {
        return (race << 16) | (clazz << 8) | (level & 0xFF);
    }
}
