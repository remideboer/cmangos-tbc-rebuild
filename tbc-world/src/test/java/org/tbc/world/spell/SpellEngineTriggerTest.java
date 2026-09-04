package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineTriggerTest {
    @Test
    void applyWhenTriggerSpellShouldApplyNestedFireballDamage() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player p = new Player();
        Creature c = new Creature();
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        int hp = c.health();
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_TRIGGER_SPELL, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        assertEquals(10, eng.apply(p, c, sp));
        assertEquals(hp - 10, c.health());
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TRIGGER_SPELL));
    }

    @Test
    void applyWhenTriggerSpellUnknownShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player p = new Player();
        Creature c = new Creature();
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        int hp = c.health();
        assertEquals(0, eng.apply(p, c, new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_TRIGGER_SPELL, 0, 0, 0, 0, 0, 0f, 0)));
        assertEquals(0, eng.apply(p, c, new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_TRIGGER_SPELL, 0, 0, 0, 0, 0, 0f, 9)));
        assertEquals(hp, c.health());
    }
}
