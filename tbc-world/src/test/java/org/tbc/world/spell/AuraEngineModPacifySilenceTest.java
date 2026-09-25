package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-131 — SPELL_AURA_MOD_PACIFY_SILENCE (60). The Black Sleep 17446.
 * CMaNGOS HandleAuraModPacifyAndSilence → HandleAuraModPacify + HandleAuraModSilence.
 */
class AuraEngineModPacifySilenceTest {
    private static final SpellEngine.SpellInfo BLACK_SLEEP = new SpellEngine.SpellInfo(
            SpellEngine.THE_BLACK_SLEEP, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_PACIFY_SILENCE, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModPacifySilenceShouldSetBothFlags() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_PACIFY_SILENCE));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, BLACK_SLEEP);
        assertTrue(mob.hasAura(SpellEngine.THE_BLACK_SLEEP));
        assertEquals(Unit.UNIT_FLAG_PACIFIED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PACIFIED);
        assertEquals(Unit.UNIT_FLAG_SILENCED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SILENCED);
    }

    @Test
    void unapplyWhenModPacifySilenceShouldClearBothFlags() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, BLACK_SLEEP);
        eng.unapplyAura(mob, SpellEngine.THE_BLACK_SLEEP);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PACIFIED);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SILENCED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BLACK_SLEEP);
        new AuraEngine().unapply(null, BLACK_SLEEP);
    }
}
