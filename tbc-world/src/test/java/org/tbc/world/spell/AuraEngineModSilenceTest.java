package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-124 — SPELL_AURA_MOD_SILENCE (27). Counterspell 2139 (APPLY_AURA, aura 27).
 * CMaNGOS HandleAuraModSilence → UNIT_FLAG_SILENCED on UNIT_FIELD_FLAGS.
 */
class AuraEngineModSilenceTest {
    private static final SpellEngine.SpellInfo COUNTERSPELL = new SpellEngine.SpellInfo(
            2139, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SILENCE, 6, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModSilenceShouldSetSilencedFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SILENCE));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, COUNTERSPELL);
        assertTrue(mob.hasAura(2139));
        assertEquals(Unit.UNIT_FLAG_SILENCED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SILENCED);
    }

    @Test
    void applyAuraWhenNotSilenceShouldLeaveSilencedFlagClear() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        SpellEngine.SpellInfo periodic = new SpellEngine.SpellInfo(
                172, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PERIODIC_DAMAGE, 32, 0, 0, 0, 0f);
        eng.apply(new Player(), mob, periodic);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SILENCED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, COUNTERSPELL);
    }

    @Test
    void unapplyWhenModSilenceShouldClearSilencedFlag() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, COUNTERSPELL);
        eng.unapplyAura(mob, SpellEngine.COUNTERSPELL);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SILENCED);
    }

    @Test
    void unapplyWhenTargetMissingShouldNoOp() {
        new AuraEngine().unapply(null, COUNTERSPELL);
    }
}
