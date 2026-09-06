package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-043 — SPELL_EFFECT_DISTRACT (69). Distract 1725. CMaNGOS SetFacingTo dest. */
class SpellEngineDistractTest {
    @Test
    void applyDistractWhenOutOfCombatShouldFaceCasterAsDest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DISTRACT));
        Player rogue = new Player();
        rogue.relocate(0f, 0f, 0f, 0f);
        Creature sentry = new Creature();
        sentry.relocate(10f, 0f, 0f, 0f);
        SpellEngine.SpellInfo distract = new SpellEngine.SpellInfo(
                1725, SpellEngine.EFFECT_DISTRACT, 0, 0, 0, 1, 1, 0f);
        eng.apply(rogue, sentry, distract);
        assertEquals((float) Math.PI, sentry.o, 0.01f);
    }

    @Test
    void applyDistractWhenInCombatShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player rogue = new Player();
        rogue.relocate(0f, 0f, 0f, 0f);
        Creature sentry = new Creature();
        sentry.relocate(10f, 0f, 0f, 0f);
        sentry.inCombat = true;
        SpellEngine.SpellInfo distract = new SpellEngine.SpellInfo(
                1725, SpellEngine.EFFECT_DISTRACT, 0, 0, 0, 1, 1, 0f);
        eng.apply(rogue, sentry, distract);
        assertEquals(0f, sentry.o, 0.01f);
    }

    @Test
    void applyDistractWhenNullCasterShouldUseTargetAsDest() {
        SpellEngine eng = new SpellEngine();
        Creature sentry = new Creature();
        sentry.relocate(10f, 0f, 0f, 1.5f);
        SpellEngine.SpellInfo distract = new SpellEngine.SpellInfo(
                1725, SpellEngine.EFFECT_DISTRACT, 0, 0, 0, 1, 1, 0f);
        eng.apply(null, sentry, distract);
        assertEquals(0f, sentry.o, 0.01f);
    }
}
