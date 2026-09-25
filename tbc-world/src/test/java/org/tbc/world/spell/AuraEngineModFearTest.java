package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-132 — SPELL_AURA_MOD_FEAR (7). Spell.dbc 5782 Fear.
 * CMaNGOS HandleModFear → SetFleeing → UNIT_FLAG_FLEEING on UNIT_FIELD_FLAGS.
 */
class AuraEngineModFearTest {
    private static final SpellEngine.SpellInfo FEAR = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_FEAR, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_FEAR,
            5, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModFearShouldSetFleeingFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FEAR));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FEAR);
        assertTrue(mob.hasAura(SpellEngine.SPELL_FEAR));
        assertEquals(Unit.UNIT_FLAG_FLEEING,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_FLEEING);
    }

    @Test
    void unapplyWhenModFearShouldClearFleeingFlag() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FEAR);
        eng.unapplyAura(mob, SpellEngine.SPELL_FEAR);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_FLEEING);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FEAR);
        new AuraEngine().unapply(null, FEAR);
    }
}
