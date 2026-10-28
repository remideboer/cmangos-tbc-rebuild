package org.tbc.world.spell;

import org.tbc.world.content.LevelStats;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-215 — SPELL_AURA_MOD_POWER_REGEN_PERCENT (110).
 * CMaNGOS UpdateManaRegen multiplies spirit regen; UpdateEnergyRegen multiplies energy ticks.
 */
class AuraEngineModPowerRegenPercentTest {
    private static SpellEngine.SpellInfo regen(int id, int amount, int power) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_POWER_REGEN_PERCENT, 0, 0, amount, amount, 0f, power);
    }

    @Test
    void applyAuraWhenManaPercentShouldScaleSpiritRegen() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_POWER_REGEN_PERCENT));
        Player player = mage();
        float base = player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN);
        assertTrue(base > 0f);
        eng.apply(player, player, regen(999110, 100, Player.POWER_MANA));
        assertEquals(base * 2f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-4f);
        eng.apply(player, player, regen(999111, 100, Player.POWER_MANA));
        assertEquals(base * 4f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-4f);
    }

    @Test
    void unapplyWhenManaPercentShouldRestoreThePreviousProduct() {
        SpellEngine eng = new SpellEngine();
        Player player = mage();
        float base = player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN);
        SpellEngine.SpellInfo first = regen(999110, 100, Player.POWER_MANA);
        eng.apply(player, player, first);
        eng.apply(player, player, regen(999111, 100, Player.POWER_MANA));
        eng.auras().unapply(player, first);
        assertEquals(base * 2f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-4f);
    }

    @Test
    void applyWhenEnergyPercentShouldSpeedEnergyRegen() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setHealth(1);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        player.setInt(UpdateFields.UNIT_FIELD_POWER4, 0);
        eng.apply(player, player, regen(999112, 100, Player.POWER_ENERGY));
        player.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(40, player.getInt(UpdateFields.UNIT_FIELD_POWER4));
    }

    @Test
    void applyWhenCreatureRageOrTargetMissingShouldLeaveManaRegen() {
        new AuraEngine().apply(null, regen(999110, 100, Player.POWER_MANA));
        Player caster = new Player();
        new SpellEngine().apply(caster, new Creature(), regen(999110, 100, Player.POWER_MANA));
        Player player = mage();
        float base = player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN);
        new AuraEngine().apply(player, regen(999113, 50, Player.POWER_RAGE));
        assertEquals(base, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-4f);
    }

    private static Player mage() {
        Player player = new Player();
        player.race = 1;
        player.clazz = 8;
        player.level = 1;
        player.initStatsForLevel(LevelStats.defaults());
        return player;
    }
}
