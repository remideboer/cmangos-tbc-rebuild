package org.tbc.world.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Slice 30: ScriptName registry filled from spec/05-domain/scripts. Missing names log and fall back. */
public final class ScriptRegistry {
    private static final Logger log = LoggerFactory.getLogger(ScriptRegistry.class);
    private final Map<String, BossScript> factories = new HashMap<>();
    private final Set<String> known = new TreeSet<>();
    private final Set<String> missing = new TreeSet<>();

    public ScriptRegistry() {
        for (String n : ScriptNames.ALL) {
            known.add(n);
        }
        registerGruul();
        registerCurator();
        registerMagtheridon();
        registerClassKeys();
    }

    private void registerGruul() {
        // spec/05-domain/scripts/gruuls_lair.md — SPELL_GROWTH 36300 every 30s is P0
        factories.put("boss_gruul", new BossScript("boss_gruul", 565, List.of(
                new BossScript.Action("SPELL_GROWTH", 36300, 30_000, true),
                new BossScript.Action("SPELL_CAVE_IN", 36240, 10_000, false),
                new BossScript.Action("SPELL_GROUND_SLAM", 33525, 35_000, false),
                new BossScript.Action("SPELL_REVERBERATION", 36297, 115_000, false),
                new BossScript.Action("SPELL_HURTFUL_STRIKE", 33812, 6_000, false)
        )));
        factories.put("boss_high_king_maulgar", new BossScript("boss_high_king_maulgar", 565, List.of(
                new BossScript.Action("SPELL_ARCING_SMASH", 39144, 8_000, false),
                new BossScript.Action("SPELL_MIGHTY_BLOW", 33230, 15_000, false),
                new BossScript.Action("SPELL_WHIRLWIND", 33238, 30_000, true)
        )));
    }

    private void registerCurator() {
        // karazhan.md — CURATOR_ACTION_HATEFUL_BOLT 15000u / SPELL_HATEFUL_BOLT 30383 (TP-SL31-002)
        factories.put("boss_curator", new BossScript("boss_curator", 532, List.of(
                new BossScript.Action("SPELL_HATEFUL_BOLT", 30383, 15_000, false),
                new BossScript.Action("SPELL_ASTRAL_FLARE", 30236, 11_000, false)
        )));
    }

    private void registerMagtheridon() {
        // magtheridons_lair.md — SPELL_BLASTNOVA 30616 (TP-SL31-003); interval from spell list (not seeded)
        factories.put("boss_magtheridon", new BossScript("boss_magtheridon", 544, List.of(
                new BossScript.Action("SPELL_BLASTNOVA", 30616, 0, true),
                new BossScript.Action("SPELL_CLEAVE", 30619, 0, false)
        )));
    }

    private void registerClassKeys() {
        // Keys exist; algorithms live in ClassScripts.
        known.add("spell_warrior_execute");
        known.add("spell_warrior_execute_damage");
        known.add("spell_unstable_affliction");
    }

    public boolean knows(String name) {
        return name != null && known.contains(name);
    }

    public BossScript create(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        BossScript proto = factories.get(name);
        if (proto != null) {
            return new BossScript(proto.scriptName, proto.mapId, proto.actions);
        }
        if (!known.contains(name)) {
            if (missing.add(name)) {
                log.warn("unknown ScriptName {}, falling back to generic AI", name);
            }
            return null;
        }
        // Known from spec but not a timer-table boss: empty script still resolves.
        return new BossScript(name, 0, List.of());
    }

    public int size() {
        return known.size();
    }
}
