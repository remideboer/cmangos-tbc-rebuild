package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-231 — SPELL_AURA_MOD_INCREASE_MAX_HEALTH (230).
 * CMaNGOS HandleAuraModIncreaseMaxHealth keeps the current health percentage.
 */
class AuraEngineModIncreaseMaxHealthTest {
    private static SpellEngine.SpellInfo shout(int amount) {
        return new SpellEngine.SpellInfo(
                999230, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_INCREASE_MAX_HEALTH, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenIncreaseMaxHealthShouldKeepTheHealthPercentage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_MAX_HEALTH));
        Player player = pool(new Player(), 100, 40);
        eng.apply(player, player, shout(100));
        assertEquals(200, player.maxHealth());
        assertEquals(80, player.health());
        eng.auras().unapply(player, shout(100));
        assertEquals(100, player.maxHealth());
        assertEquals(40, player.health());
    }

    @Test
    void applyWhenHealthIsFullOrFractionalShouldCeilTheNewHealth() {
        SpellEngine eng = new SpellEngine();
        Player full = pool(new Player(), 100, 100);
        eng.apply(full, full, shout(50));
        assertEquals(150, full.maxHealth());
        assertEquals(150, full.health());

        Player fractional = pool(new Player(), 100, 1);
        SpellEngine.SpellInfo one = new SpellEngine.SpellInfo(
                999231, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_INCREASE_MAX_HEALTH, 0, 0, 1, 1, 0f);
        eng.apply(fractional, fractional, one);
        assertEquals(101, fractional.maxHealth());
        assertEquals(2, fractional.health());
    }

    @Test
    void applyWhenHealthOrOldMaxIsZeroShouldLeaveHealthAlone() {
        SpellEngine eng = new SpellEngine();
        Player empty = pool(new Player(), 100, 0);
        eng.apply(empty, empty, shout(50));
        assertEquals(150, empty.maxHealth());
        assertEquals(0, empty.health());

        Creature fresh = new Creature();
        eng.apply(new Player(), fresh, shout(80));
        assertEquals(80, fresh.maxHealth());
        assertEquals(0, fresh.health());
        new AuraEngine().apply(null, shout(80));
    }

    private static Player pool(Player player, int max, int health) {
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, max);
        player.setHealth(health);
        return player;
    }
}
