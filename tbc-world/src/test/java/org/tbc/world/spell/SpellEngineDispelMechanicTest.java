package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-044 — SPELL_EFFECT_DISPEL_MECHANIC (108). Escape Artist 20589 mechanic ROOT 7. */
class SpellEngineDispelMechanicTest {
    @Test
    void applyDispelMechanicWhenRootAuraShouldRemoveMatchingMechanic() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DISPEL_MECHANIC));
        Player t = new Player();
        t.auras.add(new Unit.Aura(133, 30_000, 1, 0));
        t.auras.add(new Unit.Aura(122, 8_000, 1, 7));
        SpellEngine.SpellInfo escape = new SpellEngine.SpellInfo(
                20589, SpellEngine.EFFECT_DISPEL_MECHANIC, 0, 0, 0, 1, 1, 0f, 7);
        eng.apply(new Player(), t, escape);
        assertEquals(1, t.auras.size());
        assertEquals(133, t.auras.get(0).spellId());
    }

    @Test
    void applyDispelMechanicWhenNoMatchOrZeroMiscShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player t = new Player();
        t.auras.add(new Unit.Aura(133, 30_000, 1, 0));
        SpellEngine.SpellInfo escape = new SpellEngine.SpellInfo(
                20589, SpellEngine.EFFECT_DISPEL_MECHANIC, 0, 0, 0, 1, 1, 0f, 7);
        eng.apply(new Player(), t, escape);
        assertEquals(1, t.auras.size());
        Player rooted = new Player();
        rooted.auras.add(new Unit.Aura(122, 8_000, 1, 7));
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                20589, SpellEngine.EFFECT_DISPEL_MECHANIC, 0, 0, 0, 1, 1, 0f, 0);
        eng.apply(new Player(), rooted, zero);
        assertEquals(1, rooted.auras.size());
    }
}
