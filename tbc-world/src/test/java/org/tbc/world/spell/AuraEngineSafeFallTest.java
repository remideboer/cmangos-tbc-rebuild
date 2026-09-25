package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-151 — SPELL_AURA_SAFE_FALL (144). Spell.dbc 1860 Safe Fall (+17 yards).
 * CMaNGOS GetTotalAuraModifier(SAFE_FALL) reduces fall height before damage.
 */
class AuraEngineSafeFallTest {
    private static final SpellEngine.SpellInfo SAFE_FALL = new SpellEngine.SpellInfo(
            SpellEngine.SAFE_FALL, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_SAFE_FALL,
            0, 0, 17, 17, 0f);

    @Test
    void applyAuraWhenSafeFallOnPlayerShouldRaiseSafeFallBonus() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_SAFE_FALL));
        Player player = new Player();
        assertEquals(0, player.safeFall());
        eng.apply(player, player, SAFE_FALL);
        assertTrue(player.hasAura(SpellEngine.SAFE_FALL));
        assertEquals(17, player.safeFall());
    }

    @Test
    void unapplyWhenSafeFallOnPlayerShouldClearSafeFallBonus() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, SAFE_FALL);
        eng.unapplyAura(player, SpellEngine.SAFE_FALL);
        assertEquals(0, player.safeFall());
    }

    @Test
    void applyWhenAmountZeroShouldLeaveSafeFallClear() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999144, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_SAFE_FALL,
                0, 0, 0, 0, 0f));
        assertEquals(0, player.safeFall());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, SAFE_FALL);
        new AuraEngine().unapply(null, SAFE_FALL);
    }
}
