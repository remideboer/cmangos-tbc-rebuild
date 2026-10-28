package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-225 — SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING (210).
 * CMaNGOS keeps the highest positive unmounted non-stack bonus.
 */
class AuraEngineModFlightSpeedNotStackingTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo speed(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenNotStackingShouldKeepTheHighest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING));
        Player player = new Player();
        SpellEngine.SpellInfo high = speed(999210, 150);
        eng.apply(player, player, high);
        eng.apply(player, player, speed(999310, 50));
        assertEquals(17.5f, player.flightSpeed(), EPS);
        eng.auras().unapply(player, high);
        assertEquals(10.5f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenMainSpeedIsPresentShouldScaleThatMainByTheBonus() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999206, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED, 0, 0, 100, 100, 0f));
        eng.apply(player, player, speed(999210, 100));
        assertEquals(28f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenMountedOrTargetMissingShouldLeaveBaseSpeed() {
        Player mounted = new Player();
        mounted.mounted = true;
        new SpellEngine().apply(mounted, mounted, speed(999210, 150));
        assertEquals(Unit.BASE_FLIGHT_SPEED, mounted.flightSpeed(), EPS);
        new AuraEngine().apply(null, speed(999210, 150));
    }
}
