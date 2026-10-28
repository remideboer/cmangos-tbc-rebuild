package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-233 — SPELL_AURA_MOD_SCALE_2 (239) with aura 61.
 * Scale is the product of both sums, floored at 0.1.
 */
class AuraEngineModScale2Test {
    private static SpellEngine.SpellInfo scale(int id, int aura, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA, aura, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenScale2ShouldMultiplyTheScaleAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SCALE_2));
        Creature mob = new Creature();
        SpellEngine.SpellInfo second = scale(999239, AuraEngine.SPELL_AURA_MOD_SCALE_2, 100);
        SpellEngine.SpellInfo first = scale(999061, AuraEngine.SPELL_AURA_MOD_SCALE, 100);
        eng.apply(new Player(), mob, second);
        assertEquals(2.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
        eng.apply(new Player(), mob, first);
        assertEquals(4.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
        eng.auras().unapply(mob, second);
        assertEquals(2.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
        eng.auras().unapply(mob, first);
        assertEquals(1.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
    }

    @Test
    void applyWhenBothScalesAreTinyShouldFloorAtOneTenth() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, scale(1, AuraEngine.SPELL_AURA_MOD_SCALE, -95));
        eng.apply(new Player(), mob, scale(2, AuraEngine.SPELL_AURA_MOD_SCALE_2, -95));
        assertEquals(0.1f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
    }

    @Test
    void applyWhenAmountZeroOrTargetMissingShouldLeaveScale() {
        Creature mob = new Creature();
        SpellEngine.SpellInfo zero = scale(3, AuraEngine.SPELL_AURA_MOD_SCALE_2, 0);
        new SpellEngine().apply(new Player(), mob, zero);
        assertEquals(1.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
        new AuraEngine().apply(null, zero);
    }
}
