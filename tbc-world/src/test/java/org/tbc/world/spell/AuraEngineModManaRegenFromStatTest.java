package org.tbc.world.spell;

import org.tbc.world.content.LevelStats;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-234 — SPELL_AURA_MOD_MANA_REGEN_FROM_STAT (219).
 * CMaNGOS UpdateManaRegen adds {@code stat * amount / 500} to both regen fields, outside aura 110.
 */
class AuraEngineModManaRegenFromStatTest {
    private static SpellEngine.SpellInfo fromStat(int id, int amount, int stat) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_MANA_REGEN_FROM_STAT, 0, 0, amount, amount, 0f, stat);
    }

    @Test
    void applyAuraWhenManaRegenFromStatShouldAddMp5OutsideThePercentAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_MANA_REGEN_FROM_STAT));
        Player player = new Player();
        player.setHealth(1);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 5000);
        player.setPower(0);
        player.setInt(UpdateFields.UNIT_FIELD_STAT0 + 4, 500);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999110, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_POWER_REGEN_PERCENT, 0, 0, 100, 100, 0f, Player.POWER_MANA));
        SpellEngine.SpellInfo aura = fromStat(999219, 100, 4);
        eng.apply(player, player, aura);
        assertEquals(100f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-3f);
        assertEquals(100f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT), 1e-3f);
        player.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(200, player.power());
        eng.auras().unapply(player, aura);
        assertEquals(0f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-3f);
    }

    @Test
    void applyWhenMageAlreadyRegensShouldAddTheStatTermOnce() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.race = 1;
        player.clazz = 8;
        player.level = 1;
        player.initStatsForLevel(LevelStats.defaults());
        float base = player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN);
        int spirit = player.getInt(UpdateFields.UNIT_FIELD_STAT0 + 4);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999110, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_POWER_REGEN_PERCENT, 0, 0, 100, 100, 0f, Player.POWER_MANA));
        eng.apply(player, player, fromStat(999219, 100, 4));
        float added = spirit * 100 / 500f;
        assertEquals(base * 2f + added, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-3f);
        assertEquals(added, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT), 1e-3f);
    }

    @Test
    void applyWhenCreatureBadStatOrTargetMissingShouldLeaveRegen() {
        new AuraEngine().apply(null, fromStat(999219, 100, 4));
        Player caster = new Player();
        new SpellEngine().apply(caster, new Creature(), fromStat(999219, 100, 4));
        Player player = new Player();
        player.setHealth(1);
        new AuraEngine().apply(player, fromStat(999220, 100, 5));
        new AuraEngine().apply(player, fromStat(999221, 100, -1));
        assertEquals(0f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN), 1e-4f);
    }
}
