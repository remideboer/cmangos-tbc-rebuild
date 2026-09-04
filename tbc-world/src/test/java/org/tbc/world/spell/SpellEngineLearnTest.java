package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineLearnTest {
    @Test
    void applyWhenLearnSpellShouldAddTriggerSpellToBook() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_LEARN_SPELL, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        assertEquals(0, eng.apply(p, p, sp));
        assertTrue(p.spells.contains(SpellEngine.FIREBALL));
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_LEARN_SPELL));
    }

    @Test
    void learnSpellWhenCreatureOrNonPositiveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.learnSpell(null, SpellEngine.FIREBALL);
        Creature c = new Creature();
        eng.learnSpell(c, SpellEngine.FIREBALL);
        Player p = new Player();
        eng.learnSpell(p, 0);
        eng.learnSpell(p, -1);
        assertFalse(p.spells.contains(SpellEngine.FIREBALL));
        assertEquals(0, p.spells.size());
        assertEquals(0, eng.apply(p, c, new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_LEARN_SPELL, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL)));
    }

    @Test
    void applyWhenLearnSpellAlreadyKnownShouldNotDuplicate() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.spells.add(SpellEngine.FIREBALL);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_LEARN_SPELL, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        eng.apply(p, p, sp);
        assertEquals(1, p.spells.size());
    }
}
