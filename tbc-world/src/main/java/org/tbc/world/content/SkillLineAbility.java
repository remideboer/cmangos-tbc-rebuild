package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * SkillLineAbility.dbc — spell → profession skill + orange/grey band (min_value / max_value).
 * fmt {@code niiiixxiiiiixxi}: skillId[1], spellId[2], max_value[10], min_value[11].
 */
public final class SkillLineAbility {
    private static final Logger log = LoggerFactory.getLogger(SkillLineAbility.class);

    /** Rough Sharpening Stone — classic/TBC blacksmithing recipe (spell 2660, skill 164). */
    public static final int SPELL_ROUGH_SHARPENING_STONE = 2660;
    public static final int SKILL_BLACKSMITHING = 164;

    public record Entry(int skillId, int spellId, int minValue, int maxValue) {
    }

    private final Map<Integer, Entry> bySpellId = new HashMap<>();

    public static SkillLineAbility seeded() {
        SkillLineAbility c = new SkillLineAbility();
        // SkillLineAbility.dbc: orange below min_value, grey at/above max_value (Rough Sharpening Stone).
        c.put(SPELL_ROUGH_SHARPENING_STONE, SKILL_BLACKSMITHING, 1, 40);
        return c;
    }

    public void put(int spellId, int skillId, int minValue, int maxValue) {
        if (spellId == 0 || skillId == 0) {
            return;
        }
        bySpellId.put(spellId, new Entry(skillId, spellId, minValue, maxValue));
    }

    public Entry bySpell(int spellId) {
        return bySpellId.get(spellId);
    }

    public int size() {
        return bySpellId.size();
    }

    /** Load DataDir/dbc/SkillLineAbility.dbc when present; keeps seeds for missing file. */
    public void loadFromDataDir(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        Path file = dataDir.resolve("dbc").resolve("SkillLineAbility.dbc");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            bySpellId.clear();
            for (int[] row : dbc.records) {
                if (row.length < 12) {
                    continue;
                }
                int skillId = row[1];
                int spellId = row[2];
                int maxValue = row[10];
                int minValue = row[11];
                put(spellId, skillId, minValue, maxValue);
            }
            log.info("loaded {} SkillLineAbility rows", bySpellId.size());
        } catch (Exception e) {
            log.warn("SkillLineAbility.dbc load failed: {}", e.getMessage());
        }
    }
}
