package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-205 — SPELL_AURA_MOD_WATER_BREATHING (155).
 * CMaNGOS HandleModWaterBreathing → breath multiplier (100+amount)/100.
 * Aura 82 infinite breath wins.
 */
class AuraEngineModWaterBreathingTest {
    private static final float EPS = 1e-5f;
    private static final SpellEngine.SpellInfo MOD = new SpellEngine.SpellInfo(
            999155, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_WATER_BREATHING, 0, 0, 100, 100, 0f);
    private static final SpellEngine.SpellInfo INFINITE = new SpellEngine.SpellInfo(
            SpellEngine.UNENDING_BREATH, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_WATER_BREATHING, 0, 50, 0, 0, 0f);

    @Test
    void applyAuraWhenModWaterBreathingShouldScaleBreathTimer() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_WATER_BREATHING));
        Player player = new Player();
        eng.apply(player, player, MOD);
        assertEquals(2.0f, player.waterBreathingIntervalMultiplier(), EPS);
    }

    @Test
    void unapplyWhenModWaterBreathingShouldRestoreBreathTimer() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, MOD);
        eng.auras().unapply(player, MOD);
        assertEquals(1.0f, player.waterBreathingIntervalMultiplier(), EPS);
    }

    @Test
    void applyWhenInfiniteBreathIsUpShouldStayAtZero() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, INFINITE);
        eng.apply(player, player, MOD);
        assertEquals(0f, player.waterBreathingIntervalMultiplier(), EPS);
        eng.auras().unapply(player, MOD);
        assertEquals(0f, player.waterBreathingIntervalMultiplier(), EPS);
        eng.auras().unapply(player, INFINITE);
        assertEquals(1.0f, player.waterBreathingIntervalMultiplier(), EPS);
    }

    @Test
    void applyWhenCreatureOrZeroAmountShouldNoOp() {
        new AuraEngine().apply(null, MOD);
        Creature mob = new Creature();
        new SpellEngine().apply(new Player(), mob, MOD);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999156, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_WATER_BREATHING,
                0, 0, 0, 0, 0f));
        assertEquals(1.0f, player.waterBreathingIntervalMultiplier(), EPS);
    }
}
