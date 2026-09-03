package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineEnergizeTest {
    @Test
    void energizeWhenPositiveShouldIncreasePower() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setPower(10);
        eng.energize(p, 25);
        assertEquals(35, p.power());
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ENERGIZE));
    }

    @Test
    void energizeWhenNullOrNonPositiveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.energize(null, 10);
        Player p = new Player();
        p.setPower(7);
        eng.energize(p, 0);
        eng.energize(p, -1);
        assertEquals(7, p.power());
    }

    @Test
    void applyEnergizeEffectShouldRestorePower() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setPower(5);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(9999, SpellEngine.EFFECT_ENERGIZE, 0, 0, 0, 20, 20, 0f);
        eng.apply(p, p, sp);
        assertEquals(25, p.power());
        assertEquals(0, eng.apply(p, null, sp));
        assertEquals(0, eng.apply(p, p, null));
    }
}
