package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-245 — SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT (212). */
class AuraEngineModRangedAttackPowerOfStatPercentTest {
    @Test
    void applyAuraWhenRangedAttackPowerOfStatShouldKeepFlatMods() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT));
        Player player = new Player();
        player.clazz = Player.CLASS_HUNTER;
        player.setInt(UpdateFields.UNIT_FIELD_STAT1, 200);
        SpellEngine.SpellInfo flat = new SpellEngine.SpellInfo(999124, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER, 0, 0, 10, 10, 0f);
        SpellEngine.SpellInfo half = aura(999212, 50, 1);
        SpellEngine.SpellInfo quarter = aura(999213, 25, 1);
        eng.apply(player, player, flat);
        eng.apply(player, player, half);
        eng.apply(player, player, quarter);
        assertEquals(160, player.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
        eng.auras().unapply(player, half);
        assertEquals(60, player.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
        eng.auras().unapply(player, flat);
        assertEquals(50, player.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));

        Player wand = new Player();
        wand.clazz = Player.CLASS_MAGE;
        wand.setInt(UpdateFields.UNIT_FIELD_STAT1, 200);
        eng.apply(wand, wand, half);
        assertEquals(0, wand.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));

        Creature creature = new Creature();
        creature.setInt(UpdateFields.UNIT_FIELD_STAT1, 200);
        eng.apply(creature, creature, half);
        assertEquals(0, creature.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));

        Player ignored = new Player();
        ignored.clazz = Player.CLASS_HUNTER;
        ignored.setInt(UpdateFields.UNIT_FIELD_STAT1, 200);
        eng.apply(ignored, ignored, aura(999214, 0, 1));
        eng.apply(ignored, ignored, aura(999215, 50, 5));
        assertEquals(0, ignored.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));

        Player copy = new Player();
        copy.copyCreateStatsFrom(player);
        assertEquals(50, copy.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
    }

    private static SpellEngine.SpellInfo aura(int id, int amount, int stat) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT, 0, 0, amount, amount, 0f, stat);
    }
}
