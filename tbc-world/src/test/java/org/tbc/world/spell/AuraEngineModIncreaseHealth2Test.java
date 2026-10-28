package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-232 — SPELL_AURA_MOD_INCREASE_HEALTH_2 (250).
 * Same generic handler as aura 34: flat max health, current health is not rescaled.
 */
class AuraEngineModIncreaseHealth2Test {
    private static SpellEngine.SpellInfo health(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH_2, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenIncreaseHealth2ShouldAddMaxWithoutRescalingCurrent() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH_2));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(40);
        SpellEngine.SpellInfo bonus = health(999250, 100);
        eng.apply(player, player, bonus);
        assertEquals(200, player.maxHealth());
        assertEquals(40, player.health());
        eng.auras().unapply(player, bonus);
        assertEquals(100, player.maxHealth());
        assertEquals(40, player.health());
    }

    @Test
    void unapplyWhenCurrentExceedsTheNewMaxShouldClamp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(40);
        SpellEngine.SpellInfo bonus = health(999250, 100);
        eng.apply(player, player, bonus);
        player.setHealth(180);
        eng.auras().unapply(player, bonus);
        assertEquals(100, player.maxHealth());
        assertEquals(100, player.health());
    }
}
