package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-134 — SPELL_AURA_MOD_DISARM (67). Spell.dbc 676 Disarm.
 * CMaNGOS HandleAuraModDisarm → UNIT_FLAG_DISARMED on UNIT_FIELD_FLAGS.
 */
class AuraEngineModDisarmTest {
    private static final SpellEngine.SpellInfo DISARM = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_DISARM, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DISARM,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModDisarmShouldSetDisarmedFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_DISARM));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, DISARM);
        assertTrue(mob.hasAura(SpellEngine.SPELL_DISARM));
        assertEquals(Unit.UNIT_FLAG_DISARMED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_DISARMED);
    }

    @Test
    void unapplyWhenModDisarmShouldClearDisarmedFlag() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, DISARM);
        eng.unapplyAura(mob, SpellEngine.SPELL_DISARM);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_DISARMED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, DISARM);
        new AuraEngine().unapply(null, DISARM);
    }
}
