package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-037 — SPELL_EFFECT_DUAL_WIELD (40). Dual Wield 674. */
class SpellEngineDualWieldTest {
    @Test
    void applyDualWieldWhenUnitTargetShouldEnableDualWield() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DUAL_WIELD));
        Player p = new Player();
        assertFalse(p.canDualWield());
        SpellEngine.SpellInfo dw = new SpellEngine.SpellInfo(
                674, SpellEngine.EFFECT_DUAL_WIELD, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, dw);
        assertTrue(p.canDualWield());
    }

    @Test
    void applyDualWieldWhenNullTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo dw = new SpellEngine.SpellInfo(
                674, SpellEngine.EFFECT_DUAL_WIELD, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, null, dw));
        assertFalse(p.canDualWield());
    }
}
