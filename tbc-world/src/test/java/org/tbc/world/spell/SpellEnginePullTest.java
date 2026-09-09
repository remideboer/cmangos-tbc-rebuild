package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-114 — SPELL_EFFECT_PULL (70). Distract Move 15051 (one spell).
 * CMaNGOS EffectPull: DEBUG_LOG only (TODO pull toward distract center). Cataloged, target does not move.
 */
class SpellEnginePullTest {
    @Test
    void applyPullWhenCreatureTargetShouldBeCatalogedAndNotMove() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PULL));
        Player rogue = new Player();
        rogue.relocate(0f, 0f, 0f, 0f);
        Creature mob = new Creature();
        mob.relocate(10f, 5f, 0f, 1f);
        SpellEngine.SpellInfo distractMove = new SpellEngine.SpellInfo(
                15051, SpellEngine.EFFECT_PULL, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(rogue, mob, distractMove));
        assertEquals(10f, mob.x, 0.001f);
        assertEquals(5f, mob.y, 0.001f);
        assertEquals(1f, mob.o, 0.001f);
        assertEquals(0, mob.auras.size());
    }
}
