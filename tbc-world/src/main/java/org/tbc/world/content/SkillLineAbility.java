package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.classless.ClasslessCharacterPolicy;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.DbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SkillLineAbility.dbc — spell → profession skill + orange/grey band (min_value / max_value),
 * plus race/class masks for {@code Player::IsSpellFitByClassAndRace}.
 * fmt {@code niiiixxiiiiixxi}: skillId[1], spellId[2], racemask[3], classmask[4], max_value[10], min_value[11].
 */
public final class SkillLineAbility {
    private static final Logger log = LoggerFactory.getLogger(SkillLineAbility.class);

    /** Rough Sharpening Stone — classic/TBC blacksmithing recipe (spell 2660, skill 164). */
    public static final int SPELL_ROUGH_SHARPENING_STONE = 2660;
    public static final int SKILL_BLACKSMITHING = 164;

    public record Entry(int skillId, int spellId, int minValue, int maxValue, int classMask, int raceMask) {
        public Entry(int skillId, int spellId, int minValue, int maxValue) {
            this(skillId, spellId, minValue, maxValue, 0, 0);
        }
    }

    private final Map<Integer, List<Entry>> bySpellId = new HashMap<>();

    public static SkillLineAbility seeded() {
        SkillLineAbility c = new SkillLineAbility();
        // SkillLineAbility.dbc: orange below min_value, grey at/above max_value (Rough Sharpening Stone).
        c.put(SPELL_ROUGH_SHARPENING_STONE, SKILL_BLACKSMITHING, 1, 40);
        return c;
    }

    public void put(int spellId, int skillId, int minValue, int maxValue) {
        put(spellId, skillId, minValue, maxValue, 0, 0);
    }

    public void put(int spellId, int skillId, int minValue, int maxValue, int classMask, int raceMask) {
        if (spellId == 0 || skillId == 0) {
            return;
        }
        bySpellId.computeIfAbsent(spellId, k -> new ArrayList<>())
                .add(new Entry(skillId, spellId, minValue, maxValue, classMask, raceMask));
    }

    public Entry bySpell(int spellId) {
        List<Entry> list = bySpellId.get(spellId);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public int size() {
        return bySpellId.values().stream().mapToInt(List::size).sum();
    }

    /**
     * CMaNGOS Player::IsSpellFitByClassAndRace (trainer list / view mode).
     * No SkillLineAbility rows → true. Classless → always true.
     */
    public boolean fitsClassAndRace(Player p, int spellId) {
        if (p == null || spellId <= 0) {
            return true;
        }
        if (ClasslessCharacterPolicy.isClassless(p)) {
            return true;
        }
        List<Entry> bounds = bySpellId.get(spellId);
        if (bounds == null || bounds.isEmpty()) {
            return true;
        }
        int raceMask = p.race <= 0 ? 0 : 1 << (p.race - 1);
        int classMask = p.clazz <= 0 ? 0 : 1 << (p.clazz - 1);
        for (Entry e : bounds) {
            if (e.raceMask() != 0 && (e.raceMask() & raceMask) == 0) {
                continue;
            }
            if (e.classMask() != 0 && (e.classMask() & classMask) == 0) {
                continue;
            }
            // SkillRaceClassInfo NOT_TRAINABLE not loaded — matching SLA is enough (CMaNGOS returns true).
            return true;
        }
        return false;
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
                int raceMask = row[3];
                int classMask = row[4];
                int maxValue = row[10];
                int minValue = row[11];
                put(spellId, skillId, minValue, maxValue, classMask, raceMask);
            }
            log.info("loaded {} SkillLineAbility rows", size());
        } catch (Exception e) {
            log.warn("SkillLineAbility.dbc load failed: {}", e.getMessage());
        }
    }
}
