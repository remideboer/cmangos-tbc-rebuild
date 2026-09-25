package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-149 — SPELL_AURA_MOD_RANGED_ATTACK_POWER (124). Spell.dbc 21013 (+60).
 * CMaNGOS HandleAuraModRangedAttackPower; wand-users skip.
 */
class AuraEngineModRangedAttackPowerTest {
    private static final SpellEngine.SpellInfo RAP_60 = new SpellEngine.SpellInfo(
            SpellEngine.ATTACK_POWER_RANGED_60, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER, 0, 0, 60, 60, 0f);

    @Test
    void applyAuraWhenRangedAttackPowerOnHunterShouldRaiseRangedMods() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER));
        Player player = new Player();
        player.clazz = 3; // CLASS_HUNTER
        eng.apply(player, player, RAP_60);
        assertTrue(player.hasAura(SpellEngine.ATTACK_POWER_RANGED_60));
        assertEquals(60, player.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
    }

    @Test
    void unapplyWhenRangedAttackPowerOnHunterShouldClearRangedMods() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.clazz = 3;
        eng.apply(player, player, RAP_60);
        eng.unapplyAura(player, SpellEngine.ATTACK_POWER_RANGED_60);
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
    }

    @Test
    void applyWhenWandUserShouldLeaveRangedModsClear() {
        SpellEngine eng = new SpellEngine();
        Player mage = new Player();
        mage.clazz = Player.CLASS_MAGE;
        eng.apply(mage, mage, RAP_60);
        assertEquals(0, mage.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveRangedMods() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.clazz = 3;
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999124, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER,
                0, 0, 0, 0, 0f));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, RAP_60);
        new AuraEngine().unapply(null, RAP_60);
    }
}
