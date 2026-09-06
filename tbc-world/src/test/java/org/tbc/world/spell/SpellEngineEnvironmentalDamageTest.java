package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-020 — SPELL_EFFECT_ENVIRONMENTAL_DAMAGE (7). CMaNGOS self-cast fire. */
class SpellEngineEnvironmentalDamageTest {
    @Test
    void applyEnvironmentalDamageWhenLivingPlayerCasterShouldDamageCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ENVIRONMENTAL_DAMAGE));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(80);
        SpellEngine.SpellInfo fire = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENVIRONMENTAL_DAMAGE, 0, 4, 0, 20, 20, 0f);
        int dmg = eng.apply(p, p, fire);
        assertEquals(20, dmg);
        assertEquals(60, p.health());
    }

    @Test
    void applyEnvironmentalDamageWhenDeadOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo fire = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENVIRONMENTAL_DAMAGE, 0, 4, 0, 20, 20, 0f);
        assertEquals(0, eng.apply(dead, dead, fire));
        Creature c = new Creature();
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        c.setHealth(80);
        assertEquals(0, eng.apply(c, c, fire));
        assertEquals(80, c.health());
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(50);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENVIRONMENTAL_DAMAGE, 0, 4, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(live, live, zero));
        assertEquals(50, live.health());
    }
}
