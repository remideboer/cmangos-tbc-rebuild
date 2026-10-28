package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-211 — SPELL_AURA_MOD_STEALTH_LEVEL (154).
 * CMaNGOS HandleModStealthLevel → AddStealthStrength(StealthType).
 */
class AuraEngineModStealthLevelTest {
    private static final SpellEngine.SpellInfo LEVEL = new SpellEngine.SpellInfo(
            999154, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_STEALTH_LEVEL, 0, 0, 15, 15, 0f, 0);

    @Test
    void applyAuraWhenStealthLevelShouldAddStrengthForThatType() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_STEALTH_LEVEL));
        Player player = new Player();
        Creature mob = new Creature();
        eng.apply(player, player, LEVEL);
        eng.apply(player, mob, LEVEL);
        assertEquals(15, player.stealthStrength(0));
        assertEquals(15, mob.stealthStrength(0));
        assertEquals(0, player.stealthStrength(1));
    }

    @Test
    void unapplyWhenStealthLevelShouldRemoveThatTypeOnly() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo trap = new SpellEngine.SpellInfo(
                999155, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_STEALTH_LEVEL, 0, 0, 4, 4, 0f, 1);
        eng.apply(player, player, LEVEL);
        eng.apply(player, player, trap);
        eng.auras().unapply(player, LEVEL);
        assertEquals(0, player.stealthStrength(0));
        assertEquals(4, player.stealthStrength(1));
    }

    @Test
    void applyWhenAmountZeroTypeOutOfRangeOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, LEVEL);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999156, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH_LEVEL,
                0, 0, 0, 0, 0f, 0));
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999157, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH_LEVEL,
                0, 0, 8, 8, 0f, 2));
        assertEquals(0, player.stealthStrength(0));
    }
}
