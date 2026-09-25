package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-148 — SPELL_AURA_MOD_ATTACK_POWER (99). Spell.dbc 2048 Battle Shout (+305).
 * CMaNGOS HandleAuraModAttackPower → UNIT_FIELD_ATTACK_POWER_MODS.
 */
class AuraEngineModAttackPowerTest {
    private static final SpellEngine.SpellInfo BATTLE_SHOUT = new SpellEngine.SpellInfo(
            SpellEngine.BATTLE_SHOUT, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_ATTACK_POWER, 0, 0, 305, 305, 0f);

    @Test
    void applyAuraWhenAttackPowerOnPlayerShouldRaiseAttackPowerMods() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACK_POWER));
        Player player = new Player();
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
        eng.apply(player, player, BATTLE_SHOUT);
        assertTrue(player.hasAura(SpellEngine.BATTLE_SHOUT));
        assertEquals(305, player.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
    }

    @Test
    void unapplyWhenAttackPowerOnPlayerShouldClearAttackPowerMods() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, BATTLE_SHOUT);
        eng.unapplyAura(player, SpellEngine.BATTLE_SHOUT);
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveAttackPowerMods() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999099, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_ATTACK_POWER,
                0, 0, 0, 0, 0f));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
    }

    @Test
    void applyWhenNegativeAmountShouldLowerAttackPowerMods() {
        AuraEngine auras = new AuraEngine();
        Player player = new Player();
        SpellEngine.SpellInfo demo = new SpellEngine.SpellInfo(
                999100, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_ATTACK_POWER,
                0, 0, -35, -35, 0f);
        auras.apply(player, demo);
        assertEquals(-35, player.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
        auras.unapply(player, demo);
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BATTLE_SHOUT);
        new AuraEngine().unapply(null, BATTLE_SHOUT);
    }
}
