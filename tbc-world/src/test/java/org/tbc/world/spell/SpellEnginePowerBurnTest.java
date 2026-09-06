package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-023 — SPELL_EFFECT_POWER_BURN (62). Mana Burn 8129. */
class SpellEnginePowerBurnTest {
    @Test
    void applyPowerBurnWhenLivingShouldBurnPowerAndDealThatHp() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_POWER_BURN));
        Player t = new Player();
        t.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        t.setHealth(80);
        t.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        t.setPower(50);
        SpellEngine.SpellInfo burn = new SpellEngine.SpellInfo(
                8129, SpellEngine.EFFECT_POWER_BURN, 0, 6, 0, 20, 20, 30f, 0);
        int dmg = eng.apply(new Player(), t, burn);
        assertEquals(20, dmg);
        assertEquals(30, t.power());
        assertEquals(60, t.health());
    }

    @Test
    void applyPowerBurnWhenDeadWrongPowerOrEmptyShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        dead.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        dead.setPower(50);
        SpellEngine.SpellInfo burn = new SpellEngine.SpellInfo(
                8129, SpellEngine.EFFECT_POWER_BURN, 0, 6, 0, 20, 20, 30f, 0);
        assertEquals(0, eng.apply(new Player(), dead, burn));
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(80);
        live.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        live.setPower(50);
        live.powerType = Player.POWER_RAGE;
        assertEquals(0, eng.apply(new Player(), live, burn));
        live.powerType = 0;
        assertEquals(50, live.power());
        live.powerType = 0;
        live.setPower(0);
        assertEquals(0, eng.apply(new Player(), live, burn));
        assertEquals(80, live.health());
    }
}
