package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-190 — SPELL_AURA_MOD_ATTACKSPEED (9).
 * CMaNGOS HandleModAttackSpeed → ApplyAttackTimePercentMod(BASE_ATTACK) only.
 */
class AuraEngineModAttackSpeedTest {
    private static final SpellEngine.SpellInfo ATTACK_SPEED = new SpellEngine.SpellInfo(
            999009, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_ATTACKSPEED, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenModAttackSpeedShouldShortenMainhandOnly() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACKSPEED));
        Player player = new Player();
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));

        eng.apply(player, player, ATTACK_SPEED);

        assertTrue(player.hasAura(999009));
        assertEquals(Math.round(2000 * 100.0f / 120.0f),
                player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
    }

    @Test
    void unapplyWhenModAttackSpeedShouldRestoreMainhand() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, ATTACK_SPEED);
        eng.auras().unapply(player, ATTACK_SPEED);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveAttackTime() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999010, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_ATTACKSPEED,
                0, 0, 0, 0, 0f));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    @Test
    void applyWhenNegativeAmountShouldLengthenMainhand() {
        AuraEngine auras = new AuraEngine();
        Player player = new Player();
        SpellEngine.SpellInfo slow = new SpellEngine.SpellInfo(
                999011, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_ATTACKSPEED,
                0, 0, -25, -25, 0f);
        auras.apply(player, slow);
        assertEquals(Math.round(2000 * 1.25f), player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
        auras.unapply(player, slow);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, ATTACK_SPEED);
        new AuraEngine().unapply(null, ATTACK_SPEED);
    }
}
