package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-038 — SPELL_EFFECT_PARRY (22). Parry 3127. */
class SpellEngineParryTest {
    @Test
    void applyParryWhenCasterShouldEnableParryOnCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PARRY));
        Player caster = new Player();
        Creature dummy = new Creature();
        assertFalse(caster.canParry());
        SpellEngine.SpellInfo parry = new SpellEngine.SpellInfo(
                3127, SpellEngine.EFFECT_PARRY, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, dummy, parry);
        assertTrue(caster.canParry());
        assertFalse(dummy.canParry());
    }

    @Test
    void applyParryWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo parry = new SpellEngine.SpellInfo(
                3127, SpellEngine.EFFECT_PARRY, 0, 0, 0, 0, 0, 0f);
        eng.apply(null, p, parry);
        assertFalse(p.canParry());
    }
}
