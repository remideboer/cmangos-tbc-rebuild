package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * DurabilityCosts.dbc × DurabilityQuality.dbc. Player::DurabilityRepair.
 * In-memory seed is 8606 item level 2 (Worn Shortsword) plus the quality table.
 */
public final class DurabilityCosts {
    private static final Logger log = LoggerFactory.getLogger(DurabilityCosts.class);
    public static final int ITEM_CLASS_WEAPON = 2;
    public static final int ITEM_CLASS_ARMOR = 4;

    private static final Map<Integer, int[]> MULTIPLIER = new HashMap<>();
    private static final Map<Integer, Float> QUALITY_MOD = new HashMap<>();

    static {
        seed();
    }

    private DurabilityCosts() {}

    static void seed() {
        MULTIPLIER.clear();
        QUALITY_MOD.clear();
        MULTIPLIER.put(2, new int[] {
                1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0
        });
        QUALITY_MOD.put(1, 1.0f);
        QUALITY_MOD.put(2, 0.6f);
        QUALITY_MOD.put(3, 1.0f);
        QUALITY_MOD.put(4, 0.8f);
        QUALITY_MOD.put(5, 1.0f);
        QUALITY_MOD.put(6, 1.0f);
        QUALITY_MOD.put(7, 1.2f);
        QUALITY_MOD.put(8, 1.25f);
        QUALITY_MOD.put(9, 1.44f);
        QUALITY_MOD.put(10, 2.5f);
        QUALITY_MOD.put(11, 1.728f);
        QUALITY_MOD.put(12, 3.0f);
        QUALITY_MOD.put(13, 0.0f);
        QUALITY_MOD.put(14, 0.0f);
    }

    public static void load(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        Path costs = dataDir.resolve("dbc").resolve("DurabilityCosts.dbc");
        if (Files.isRegularFile(costs)) {
            try {
                DbcFile dbc = DbcFile.load(costs);
                for (int[] row : dbc.records) {
                    if (row.length < 30) {
                        continue;
                    }
                    int[] m = new int[29];
                    System.arraycopy(row, 1, m, 0, 29);
                    MULTIPLIER.put(row[0], m);
                }
            } catch (Exception e) {
                log.warn("DurabilityCosts.dbc load failed: {}", e.getMessage());
            }
        }
        Path quality = dataDir.resolve("dbc").resolve("DurabilityQuality.dbc");
        if (Files.isRegularFile(quality)) {
            try {
                DbcFile dbc = DbcFile.load(quality);
                for (int[] row : dbc.records) {
                    if (row.length < 2) {
                        continue;
                    }
                    QUALITY_MOD.put(row[0], Float.intBitsToFloat(row[1]));
                }
            } catch (Exception e) {
                log.warn("DurabilityQuality.dbc load failed: {}", e.getMessage());
            }
        }
    }

    /** Lost points × subclass multiplier × quality_mod × discount; min 1 when the product truncates to 0. */
    public static int repairCopper(int lost, int itemLevel, int itemClass, int subClass, int quality,
                                   float discount) {
        if (lost <= 0) {
            return 0;
        }
        int[] row = MULTIPLIER.get(itemLevel);
        if (row == null) {
            return 0;
        }
        int idx = multiplierId(itemClass, subClass);
        if (idx < 0 || idx >= row.length) {
            return 0;
        }
        int qid = (quality + 1) * 2;
        Float qmod = QUALITY_MOD.get(qid);
        if (qmod == null) {
            return 0;
        }
        int costs = (int) (lost * row[idx] * (double) qmod);
        costs = (int) (costs * discount);
        if (costs == 0) {
            costs = 1;
        }
        return costs;
    }

    static int multiplierId(int itemClass, int subClass) {
        if (itemClass == ITEM_CLASS_WEAPON) {
            return subClass;
        }
        if (itemClass == ITEM_CLASS_ARMOR) {
            return subClass + 21;
        }
        return 0;
    }
}
