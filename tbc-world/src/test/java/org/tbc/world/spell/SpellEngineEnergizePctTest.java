package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-026 — SPELL_EFFECT_ENERGIZE_PCT (137). */
class SpellEngineEnergizePctTest {
    @Test
    void applyEnergizePctWhenLivingShouldGainPercentOfMaxPower() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ENERGIZE_PCT));
        Player t = new Player();
        t.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        t.setHealth(50);
        t.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        t.setPower(10);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENERGIZE_PCT, 0, 0, 0, 50, 50, 0f, 0);
        eng.apply(new Player(), t, sp);
        assertEquals(60, t.power());
    }

    @Test
    void applyEnergizePctWhenDeadWrongPowerOrZeroMaxShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        dead.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        dead.setPower(10);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENERGIZE_PCT, 0, 0, 0, 50, 50, 0f, 0);
        eng.apply(new Player(), dead, sp);
        assertEquals(10, dead.power());
        Player rage = new Player();
        rage.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        rage.setHealth(50);
        rage.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        rage.setPower(10);
        rage.powerType = Player.POWER_RAGE;
        eng.apply(new Player(), rage, sp);
        rage.powerType = 0;
        assertEquals(10, rage.power());
        Player empty = new Player();
        empty.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        empty.setHealth(50);
        empty.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 0);
        empty.setPower(0);
        eng.apply(new Player(), empty, sp);
        assertEquals(0, empty.power());
    }
}
