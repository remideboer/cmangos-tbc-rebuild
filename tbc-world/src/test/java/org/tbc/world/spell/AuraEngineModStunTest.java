package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-120 — SPELL_AURA_MOD_STUN (12). Hammer of Justice 853 (effect 6 APPLY_AURA, aura 12).
 * CMaNGOS HandleAuraModStun → Unit::SetStunned → UNIT_FLAG_STUNNED on UNIT_FIELD_FLAGS.
 */
class AuraEngineModStunTest {
    private static final SpellEngine.SpellInfo HAMMER_OF_JUSTICE = new SpellEngine.SpellInfo(
            853, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STUN, 2, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModStunShouldSetStunnedFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_STUN));
        Player paladin = new Player();
        Creature mob = new Creature();
        eng.apply(paladin, mob, HAMMER_OF_JUSTICE);
        assertTrue(mob.hasAura(853));
        assertEquals(Unit.UNIT_FLAG_STUNNED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_STUNNED);
    }

    @Test
    void applyAuraWhenNotStunShouldLeaveStunnedFlagClear() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        SpellEngine.SpellInfo periodic = new SpellEngine.SpellInfo(
                172, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PERIODIC_DAMAGE, 32, 0, 0, 0, 0f);
        eng.apply(new Player(), mob, periodic);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_STUNNED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HAMMER_OF_JUSTICE);
    }
}
