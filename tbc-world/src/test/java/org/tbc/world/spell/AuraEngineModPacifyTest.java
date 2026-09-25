package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-126 — SPELL_AURA_MOD_PACIFY (25). Spell.dbc 10730 Pacify (Effect1 APPLY_AURA, aura 25).
 * CMaNGOS HandleAuraModPacify → UNIT_FLAG_PACIFIED on UNIT_FIELD_FLAGS.
 */
class AuraEngineModPacifyTest {
    private static final SpellEngine.SpellInfo PACIFY = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_PACIFY, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_PACIFY,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModPacifyShouldSetPacifiedFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_PACIFY));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, PACIFY);
        assertTrue(mob.hasAura(SpellEngine.SPELL_PACIFY));
        assertEquals(Unit.UNIT_FLAG_PACIFIED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PACIFIED);
    }

    @Test
    void applyAuraWhenNotPacifyShouldLeavePacifiedFlagClear() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        SpellEngine.SpellInfo periodic = new SpellEngine.SpellInfo(
                172, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PERIODIC_DAMAGE, 32, 0, 0, 0, 0f);
        eng.apply(new Player(), mob, periodic);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PACIFIED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, PACIFY);
    }

    @Test
    void unapplyWhenModPacifyShouldClearPacifiedFlag() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, PACIFY);
        eng.unapplyAura(mob, SpellEngine.SPELL_PACIFY);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PACIFIED);
    }

    @Test
    void unapplyWhenTargetMissingShouldNoOp() {
        new AuraEngine().unapply(null, PACIFY);
    }
}
