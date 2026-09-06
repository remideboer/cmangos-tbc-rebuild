package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-014 — SPELL_EFFECT_SANCTUARY (79). */
class SpellEngineSanctuaryTest {
    @Test
    void applySanctuaryWhenInCombatShouldStopCombat() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SANCTUARY));
        Player p = new Player();
        p.inCombat = true;
        p.victim = 99L;
        SpellEngine.SpellInfo vanish = new SpellEngine.SpellInfo(1856, SpellEngine.EFFECT_SANCTUARY, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, vanish);
        assertFalse(p.inCombat);
        assertEquals(0L, p.victim);
    }

    @Test
    void applySanctuaryWhenNullTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.inCombat = true;
        p.victim = 7L;
        SpellEngine.SpellInfo vanish = new SpellEngine.SpellInfo(1856, SpellEngine.EFFECT_SANCTUARY, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, null, vanish));
        assertTrue(p.inCombat);
        assertEquals(7L, p.victim);
    }
}
