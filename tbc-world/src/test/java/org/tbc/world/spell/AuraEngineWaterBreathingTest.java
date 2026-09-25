package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-145 — SPELL_AURA_WATER_BREATHING (82). Spell.dbc 5697 Unending Breath.
 * CMaNGOS HandleWaterBreathing → SetWaterBreathingIntervalMultiplier(0 / restore).
 */
class AuraEngineWaterBreathingTest {
    private static final float EPS = 1e-5f;
    private static final SpellEngine.SpellInfo UNENDING_BREATH = new SpellEngine.SpellInfo(
            SpellEngine.UNENDING_BREATH, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_WATER_BREATHING, 0, 50, 0, 0, 0f);

    @Test
    void applyAuraWhenWaterBreathingOnPlayerShouldZeroBreathMultiplier() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_WATER_BREATHING));
        Player player = new Player();
        assertEquals(1.0f, player.waterBreathingIntervalMultiplier(), EPS);
        eng.apply(player, player, UNENDING_BREATH);
        assertTrue(player.hasAura(SpellEngine.UNENDING_BREATH));
        assertEquals(0f, player.waterBreathingIntervalMultiplier(), EPS);
    }

    @Test
    void unapplyWhenWaterBreathingOnPlayerShouldRestoreBreathMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, UNENDING_BREATH);
        eng.unapplyAura(player, SpellEngine.UNENDING_BREATH);
        assertEquals(1.0f, player.waterBreathingIntervalMultiplier(), EPS);
    }

    @Test
    void applyWhenCreatureShouldNoOpBreathMultiplier() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, UNENDING_BREATH);
        assertTrue(mob.hasAura(SpellEngine.UNENDING_BREATH));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, UNENDING_BREATH);
        new AuraEngine().unapply(null, UNENDING_BREATH);
    }
}
