package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-025 — SPELL_EFFECT_HEAL_PCT (136). Lay on Hands 633. */
class SpellEngineHealPctTest {
    @Test
    void applyHealPctWhenLivingShouldHealPercentOfMaxHealth() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_HEAL_PCT));
        Player t = new Player();
        t.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        t.setHealth(20);
        SpellEngine.SpellInfo loh = new SpellEngine.SpellInfo(
                633, SpellEngine.EFFECT_HEAL_PCT, 0, 1, 0, 50, 50, 0f);
        eng.apply(new Player(), t, loh);
        assertEquals(70, t.health());
    }

    @Test
    void applyHealPctWhenDeadOrZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo loh = new SpellEngine.SpellInfo(
                633, SpellEngine.EFFECT_HEAL_PCT, 0, 1, 0, 50, 50, 0f);
        eng.apply(new Player(), dead, loh);
        assertEquals(0, dead.health());
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(20);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                633, SpellEngine.EFFECT_HEAL_PCT, 0, 1, 0, 0, 0, 0f);
        eng.apply(new Player(), live, zero);
        assertEquals(20, live.health());
    }
}
