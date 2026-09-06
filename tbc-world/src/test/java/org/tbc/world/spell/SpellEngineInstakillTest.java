package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-009 — SPELL_EFFECT_INSTAKILL (1). */
class SpellEngineInstakillTest {
    @Test
    void applyInstakillWhenLivingTargetShouldSetHealthToZero() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_INSTAKILL));
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(80);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(5, SpellEngine.EFFECT_INSTAKILL, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, target, sp);
        assertEquals(0, target.health());
        assertFalse(target.alive());
    }

    @Test
    void applyInstakillWhenAlreadyDeadShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature target = new Creature();
        target.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(0);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(5, SpellEngine.EFFECT_INSTAKILL, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(new Player(), target, sp));
        assertEquals(0, target.health());
        assertEquals(0, eng.apply(new Player(), null, sp));
    }
}
