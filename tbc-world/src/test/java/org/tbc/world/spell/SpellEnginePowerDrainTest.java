package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-011 — SPELL_EFFECT_POWER_DRAIN (8). */
class SpellEnginePowerDrainTest {
    @Test
    void applyPowerDrainWhenLivingShouldStealPower() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_POWER_DRAIN));
        Player caster = new Player();
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(100);
        caster.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        caster.setPower(10);
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(100);
        target.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        target.setPower(80);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(5138, SpellEngine.EFFECT_POWER_DRAIN, 0, 5, 0, 30, 30, 30f);
        int drained = eng.apply(caster, target, sp);
        assertEquals(30, drained);
        assertEquals(50, target.power());
        assertEquals(40, caster.power());
    }

    @Test
    void applyPowerDrainWhenDeadOrSelfOrZeroShouldNoOpOrNotGain() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(100);
        caster.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        caster.setPower(10);
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        dead.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        dead.setPower(80);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(5138, SpellEngine.EFFECT_POWER_DRAIN, 0, 5, 0, 30, 30, 30f);
        assertEquals(0, eng.apply(caster, dead, sp));
        assertEquals(10, caster.power());
        assertEquals(10, eng.apply(caster, caster, sp));
        assertEquals(0, caster.power());
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(5138, SpellEngine.EFFECT_POWER_DRAIN, 0, 5, 0, 0, 0, 30f);
        Creature live = new Creature();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(100);
        live.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        live.setPower(50);
        assertEquals(0, eng.apply(caster, live, zero));
        assertEquals(50, live.power());
    }
}
