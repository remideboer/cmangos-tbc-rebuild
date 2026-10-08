package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-182 — SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT (133).
 * CMaNGOS HandleAuraModIncreaseHealthPercent → TOTAL_PCT on UNIT_MOD_HEALTH (max HP).
 */
class AuraEngineModIncreaseHealthPercentTest {
    /** Synthetic +20% max health. */
    private static final SpellEngine.SpellInfo HEALTH_PCT = new SpellEngine.SpellInfo(
            999133, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenModIncreaseHealthPercentShouldRaiseMaxHealth() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setInt(UpdateFields.UNIT_FIELD_HEALTH, 80);

        eng.apply(player, player, HEALTH_PCT);

        assertTrue(player.hasAura(999133));
        assertEquals(120, player.maxHealth());
        assertEquals(80, player.health());
    }

    @Test
    void unapplyWhenModIncreaseHealthPercentShouldRestoreMaxAndClampCurrent() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setInt(UpdateFields.UNIT_FIELD_HEALTH, 100);
        eng.apply(player, player, HEALTH_PCT);
        player.setInt(UpdateFields.UNIT_FIELD_HEALTH, 120);
        eng.auras().unapply(player, HEALTH_PCT);
        assertEquals(100, player.maxHealth());
        assertEquals(100, player.health());
    }

    @Test
    void applyWhenAmountZeroShouldLeaveMaxHealth() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999134, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT,
                0, 0, 0, 0, 0f));
        assertEquals(50, player.maxHealth());
    }

    @Test
    void applyWhenCreatureShouldRaiseMaxHealth() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        eng.apply(new Player(), mob, HEALTH_PCT);
        assertEquals(240, mob.maxHealth());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HEALTH_PCT);
        new AuraEngine().unapply(null, HEALTH_PCT);
    }
}
