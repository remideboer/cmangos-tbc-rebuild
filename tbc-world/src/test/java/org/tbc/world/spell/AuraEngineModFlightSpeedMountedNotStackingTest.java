package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-226 — SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING (211).
 * CMaNGOS keeps the highest positive mounted non-stack bonus.
 */
class AuraEngineModFlightSpeedMountedNotStackingTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo speed(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenMountedShouldKeepTheHighest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING));
        Player player = new Player();
        player.mounted = true;
        SpellEngine.SpellInfo high = speed(999211, 150);
        eng.apply(player, player, high);
        eng.apply(player, player, speed(999311, 50));
        assertEquals(17.5f, player.flightSpeed(), EPS);
        eng.auras().unapply(player, high);
        assertEquals(Unit.BASE_FLIGHT_SPEED * 1.5f, player.flightSpeed(), EPS);
        eng.auras().unapply(player, speed(999311, 50));
        assertEquals(Unit.BASE_FLIGHT_SPEED, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenOnFootOrTargetMissingShouldLeaveBaseSpeed() {
        Player player = new Player();
        new SpellEngine().apply(player, player, speed(999211, 150));
        assertEquals(Unit.BASE_FLIGHT_SPEED, player.flightSpeed(), EPS);
        new AuraEngine().apply(null, speed(999211, 150));
    }
}
