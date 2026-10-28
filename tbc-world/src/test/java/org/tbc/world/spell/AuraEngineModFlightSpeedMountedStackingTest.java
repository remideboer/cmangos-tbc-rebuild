package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-224 — SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING (209).
 * CMaNGOS multiplies mounted flight by each aura's (100+amount)/100.
 */
class AuraEngineModFlightSpeedMountedStackingTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo speed(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenMountedShouldMultiplyFlightSpeed() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING));
        Player player = new Player();
        player.mounted = true;
        SpellEngine.SpellInfo first = speed(999209, 50);
        eng.apply(player, player, first);
        eng.apply(player, player, speed(999309, 50));
        assertEquals(15.75f, player.flightSpeed(), EPS);
        eng.auras().unapply(player, first);
        assertEquals(10.5f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenOnFootOrTargetMissingShouldLeaveBaseSpeed() {
        Player player = new Player();
        new SpellEngine().apply(player, player, speed(999209, 100));
        assertEquals(Unit.BASE_FLIGHT_SPEED, player.flightSpeed(), EPS);
        new AuraEngine().apply(null, speed(999209, 100));
    }
}
