package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-012 — SPELL_EFFECT_ADD_COMBO_POINTS (80). */
class SpellEngineComboPointsTest {
    @Test
    void applyAddComboPointsWhenPlayerShouldAccumulateAndCapAtFive() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ADD_COMBO_POINTS));
        Player rogue = new Player();
        Creature target = new Creature();
        target.guid = 11L;
        SpellEngine.SpellInfo ss = new SpellEngine.SpellInfo(1752, SpellEngine.EFFECT_ADD_COMBO_POINTS, 0, 0, 0, 1, 1, 5f);
        eng.apply(rogue, target, ss);
        assertEquals(1, rogue.comboPoints());
        eng.apply(rogue, target, ss);
        assertEquals(2, rogue.comboPoints());
        SpellEngine.SpellInfo five = new SpellEngine.SpellInfo(1752, SpellEngine.EFFECT_ADD_COMBO_POINTS, 0, 0, 0, 5, 5, 5f);
        eng.apply(rogue, target, five);
        assertEquals(5, rogue.comboPoints());
    }

    @Test
    void applyAddComboPointsWhenNewTargetShouldResetCount() {
        SpellEngine eng = new SpellEngine();
        Player rogue = new Player();
        Creature a = new Creature();
        a.guid = 11L;
        Creature b = new Creature();
        b.guid = 12L;
        SpellEngine.SpellInfo ss = new SpellEngine.SpellInfo(1752, SpellEngine.EFFECT_ADD_COMBO_POINTS, 0, 0, 0, 2, 2, 5f);
        eng.apply(rogue, a, ss);
        assertEquals(2, rogue.comboPoints());
        eng.apply(rogue, b, ss);
        assertEquals(2, rogue.comboPoints());
        assertEquals(0, eng.apply(new Creature(), b, ss));
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(1752, SpellEngine.EFFECT_ADD_COMBO_POINTS, 0, 0, 0, 0, 0, 5f);
        assertEquals(0, eng.apply(rogue, b, zero));
        assertEquals(2, rogue.comboPoints());
    }
}
