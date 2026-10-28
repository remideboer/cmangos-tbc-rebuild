package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-223 — SPELL_AURA_MOD_FLIGHT_SPEED_STACKING (208).
 * CMaNGOS multiplies unmounted flight by each aura's (100+amount)/100.
 */
class AuraEngineModFlightSpeedStackingTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo speed(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_STACKING, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenStackingShouldMultiplyFlightSpeed() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_STACKING));
        Player player = new Player();
        eng.apply(player, player, speed(999208, 100));
        assertEquals(14f, player.flightSpeed(), EPS);
        eng.apply(player, player, speed(999308, 100));
        assertEquals(28f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenMainSpeedIsAlsoPresentShouldScaleThatMain() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999206, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED, 0, 0, 100, 100, 0f));
        eng.apply(player, player, speed(999208, 100));
        assertEquals(28f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenMountedOrTargetMissingShouldLeaveBaseSpeed() {
        Player mounted = new Player();
        mounted.mounted = true;
        new SpellEngine().apply(mounted, mounted, speed(999208, 100));
        assertEquals(Unit.BASE_FLIGHT_SPEED, mounted.flightSpeed(), EPS);
        new AuraEngine().apply(null, speed(999208, 100));
    }
}
