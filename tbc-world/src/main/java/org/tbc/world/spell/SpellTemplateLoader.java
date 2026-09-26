package org.tbc.world.spell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.DbPool;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/** spell_template (CMaNGOS export of Spell.dbc) into {@link SpellEngine}. */
public final class SpellTemplateLoader {
    private static final Logger log = LoggerFactory.getLogger(SpellTemplateLoader.class);

    private SpellTemplateLoader() {
    }

    public static void load(DbPool world, Path dataDir, SpellEngine spells) {
        if (world == null || spells == null) {
            return;
        }
        Map<Integer, Integer> castMs = castTimes(dataDir);
        Map<Integer, Integer> durationMs = durations(dataDir);
        Map<Integer, Float> range = ranges(dataDir);
        try (Connection c = world.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT Id, CastingTimeIndex, RecoveryTime, StartRecoveryTime, DurationIndex, ManaCost, RangeIndex, "
                             + "Effect1, Effect2, Effect3, EffectDieSides1, EffectDieSides2, EffectDieSides3, "
                             + "EffectBasePoints1, EffectBasePoints2, EffectBasePoints3, "
                             + "EffectApplyAuraName1, EffectApplyAuraName2, EffectApplyAuraName3, "
                             + "EffectMiscValue1, EffectMiscValue2, EffectMiscValue3, "
                             + "ProcFlags, EffectTriggerSpell1, SchoolMask FROM spell_template")) {
            int n = 0;
            while (rs.next()) {
                int id = rs.getInt("Id");
                spells.putTemplate(id,
                        rs.getInt("Effect1"), rs.getInt("EffectApplyAuraName1"), rs.getInt("SchoolMask"),
                        rs.getInt("ManaCost"),
                        points(rs.getInt("EffectBasePoints1"), rs.getInt("EffectDieSides1"), true),
                        points(rs.getInt("EffectBasePoints1"), rs.getInt("EffectDieSides1"), false),
                        range.getOrDefault(rs.getInt("RangeIndex"), 0f),
                        castMs.getOrDefault(rs.getInt("CastingTimeIndex"), 0),
                        rs.getInt("StartRecoveryTime"),
                        rs.getInt("RecoveryTime"),
                        durationMs.getOrDefault(rs.getInt("DurationIndex"), 0),
                        rs.getInt("Effect2"), rs.getInt("EffectApplyAuraName2"),
                        points(rs.getInt("EffectBasePoints2"), rs.getInt("EffectDieSides2"), true),
                        points(rs.getInt("EffectBasePoints2"), rs.getInt("EffectDieSides2"), false),
                        rs.getInt("Effect3"), rs.getInt("EffectApplyAuraName3"),
                        points(rs.getInt("EffectBasePoints3"), rs.getInt("EffectDieSides3"), true),
                        points(rs.getInt("EffectBasePoints3"), rs.getInt("EffectDieSides3"), false),
                        rs.getInt("ProcFlags"), rs.getInt("EffectTriggerSpell1"),
                        rs.getInt("EffectMiscValue1"), rs.getInt("EffectMiscValue2"), rs.getInt("EffectMiscValue3"));
                n++;
            }
            log.info("loaded {} spell_template rows", n);
        } catch (Exception e) {
            log.warn("spell_template load failed: {}", e.getMessage());
        }
    }

    /** EffectBasePoints is the minimum minus 1. Die sides widen the max. */
    static int points(int basePoints, int dieSides, boolean min) {
        int low = basePoints + 1;
        if (min || dieSides <= 1) {
            return low;
        }
        return basePoints + dieSides;
    }

    private static Map<Integer, Integer> castTimes(Path dataDir) {
        Map<Integer, Integer> m = new HashMap<>();
        m.put(1, 0);
        m.put(7, 10_000);
        m.put(16, 1500);
        loadIntDbc(dataDir, "SpellCastTimes.dbc", 1, m);
        return m;
    }

    private static Map<Integer, Integer> durations(Path dataDir) {
        Map<Integer, Integer> m = new HashMap<>();
        // SpellDuration.dbc Duration[0] (ms). Seeds when DataDir / DBC is absent.
        m.put(21, 15_000);
        m.put(28, SpellEngine.DRAIN_LIFE_DURATION_MS);
        m.put(30, SpellEngine.FROST_ARMOR_DURATION_MS);
        m.put(85, SpellEngine.FOOD_DURATION_MS);
        loadDurationDbc(dataDir, m);
        return m;
    }

    /** SpellDuration.dbc: col0 id, col1 Duration[0]; −1 = permanent → 0 ms in our model. */
    private static void loadDurationDbc(Path dataDir, Map<Integer, Integer> into) {
        if (dataDir == null) {
            return;
        }
        try {
            DbcFile f = DbcFile.load(dataDir.resolve("dbc").resolve("SpellDuration.dbc"));
            for (int[] row : f.records) {
                if (row.length > 1) {
                    int v = row[1];
                    into.put(row[0], v == -1 ? 0 : Math.abs(v));
                }
            }
        } catch (Exception ignored) {
            // DataDir without this file keeps the seed.
        }
    }

    private static void loadIntDbc(Path dataDir, String name, int column, Map<Integer, Integer> into) {
        if (dataDir == null) {
            return;
        }
        try {
            DbcFile f = DbcFile.load(dataDir.resolve("dbc").resolve(name));
            for (int[] row : f.records) {
                if (row.length > column) {
                    into.put(row[0], row[column]);
                }
            }
        } catch (Exception ignored) {
            // DataDir without this file keeps the seed.
        }
    }

    private static Map<Integer, Float> ranges(Path dataDir) {
        Map<Integer, Float> m = new HashMap<>();
        if (dataDir == null) {
            return m;
        }
        try {
            DbcFile f = DbcFile.load(dataDir.resolve("dbc").resolve("SpellRange.dbc"));
            for (int[] row : f.records) {
                if (row.length > 2) {
                    m.put(row[0], Float.intBitsToFloat(row[2]));
                }
            }
        } catch (Exception ignored) {
        }
        return m;
    }
}
