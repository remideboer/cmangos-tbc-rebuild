package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-039 — SPELL_EFFECT_BLOCK (23). Block 107. */
class SpellEngineBlockTest {
    @Test
    void applyBlockWhenCasterShouldEnableBlockOnCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_BLOCK));
        Player caster = new Player();
        Creature dummy = new Creature();
        assertFalse(caster.canBlock());
        SpellEngine.SpellInfo block = new SpellEngine.SpellInfo(
                107, SpellEngine.EFFECT_BLOCK, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, dummy, block);
        assertTrue(caster.canBlock());
        assertFalse(dummy.canBlock());
    }

    @Test
    void applyBlockWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo block = new SpellEngine.SpellInfo(
                107, SpellEngine.EFFECT_BLOCK, 0, 0, 0, 0, 0, 0f);
        eng.apply(null, p, block);
        assertFalse(p.canBlock());
    }
}
