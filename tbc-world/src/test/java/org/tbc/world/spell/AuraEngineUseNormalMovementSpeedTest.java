package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-216 — SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED (191).
 * CMaNGOS UpdateSpeed caps a player's run and swim rate at amount / base speed.
 */
class AuraEngineUseNormalMovementSpeedTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo cap(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenCapBelowBaseShouldLimitRunAndSwim() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED));
        Player player = new Player();
        eng.apply(player, player, cap(999191, 4));
        assertEquals(4.0f, player.runSpeed(), EPS);
        assertEquals(4.0f, player.swimSpeed(), EPS);
    }

    @Test
    void applyWhenCapAboveCurrentRateShouldLeaveTheBonus() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999031, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_INCREASE_SPEED, 0, 0, 100, 100, 0f));
        eng.apply(player, player, cap(999192, 14));
        assertEquals(14.0f, player.runSpeed(), EPS);
        eng.auras().unapply(player, cap(999192, 14));
        assertEquals(14.0f, player.runSpeed(), EPS);
    }

    @Test
    void unapplyWhenHigherCapDropsShouldRevealTheLowerCap() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo high = cap(999193, 14);
        SpellEngine.SpellInfo low = cap(999194, 4);
        eng.apply(player, player, high);
        eng.apply(player, player, low);
        assertEquals(Unit.BASE_RUN_SPEED, player.runSpeed(), EPS);
        eng.auras().unapply(player, high);
        assertEquals(4.0f, player.runSpeed(), EPS);
        eng.auras().unapply(player, low);
        assertEquals(Unit.BASE_RUN_SPEED, player.runSpeed(), EPS);
        assertEquals(Unit.BASE_SWIM_SPEED, player.swimSpeed(), EPS);
    }

    @Test
    void applyWhenSlowFollowsTheCapAndCreaturesIgnoreIt() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, cap(999191, 4));
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999033, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_DECREASE_SPEED, 0, 0, -50, -50, 0f));
        assertEquals(2.0f, player.runSpeed(), EPS);
        Creature mob = new Creature();
        eng.apply(player, mob, cap(999191, 4));
        assertEquals(Unit.BASE_RUN_SPEED, mob.runSpeed(), EPS);
        new AuraEngine().apply(null, cap(999191, 4));
    }
}
