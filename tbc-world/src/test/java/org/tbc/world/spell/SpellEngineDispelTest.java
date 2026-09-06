package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-022 — SPELL_EFFECT_DISPEL (38). Priest Dispel Magic 527. */
class SpellEngineDispelTest {
    @Test
    void applyDispelWhenTargetHasAurasShouldRemoveUpToDamageCount() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DISPEL));
        Player t = new Player();
        t.auras.add(new Unit.Aura(133, 30_000, 1));
        t.auras.add(new Unit.Aura(2050, 30_000, 1));
        SpellEngine.SpellInfo dispel = new SpellEngine.SpellInfo(
                527, SpellEngine.EFFECT_DISPEL, 0, 0, 0, 1, 1, 30f);
        eng.apply(new Player(), t, dispel);
        assertEquals(1, t.auras.size());
        eng.apply(new Player(), t, dispel);
        assertEquals(0, t.auras.size());
    }

    @Test
    void applyDispelWhenNoAurasOrZeroDamageShouldRemoveOneIfPresent() {
        SpellEngine eng = new SpellEngine();
        Player empty = new Player();
        SpellEngine.SpellInfo dispel = new SpellEngine.SpellInfo(
                527, SpellEngine.EFFECT_DISPEL, 0, 0, 0, 1, 1, 30f);
        eng.apply(new Player(), empty, dispel);
        assertEquals(0, empty.auras.size());
        Player t = new Player();
        t.auras.add(new Unit.Aura(133, 30_000, 1));
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                527, SpellEngine.EFFECT_DISPEL, 0, 0, 0, 0, 0, 30f);
        eng.apply(new Player(), t, zero);
        assertEquals(0, t.auras.size());
    }
}
