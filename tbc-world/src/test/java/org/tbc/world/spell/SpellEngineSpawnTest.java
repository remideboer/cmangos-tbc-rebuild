package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-040 — SPELL_EFFECT_SPAWN (46). Rookery Whelp Spawn-in Spell 15750. */
class SpellEngineSpawnTest {
    @Test
    void applySpawnWhenCasterShouldClearSpawningFlagOnCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SPAWN));
        Player caster = new Player();
        Creature dummy = new Creature();
        caster.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                caster.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_SPAWNING);
        dummy.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SPAWNING);
        SpellEngine.SpellInfo spawn = new SpellEngine.SpellInfo(
                15750, SpellEngine.EFFECT_SPAWN, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, dummy, spawn);
        assertEquals(0, caster.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SPAWNING);
        assertEquals(Unit.UNIT_FLAG_SPAWNING,
                dummy.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SPAWNING);
    }

    @Test
    void applySpawnWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SPAWNING);
        SpellEngine.SpellInfo spawn = new SpellEngine.SpellInfo(
                15750, SpellEngine.EFFECT_SPAWN, 0, 0, 0, 0, 0, 0f);
        eng.apply(null, p, spawn);
        assertEquals(Unit.UNIT_FLAG_SPAWNING,
                p.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SPAWNING);
    }
}
