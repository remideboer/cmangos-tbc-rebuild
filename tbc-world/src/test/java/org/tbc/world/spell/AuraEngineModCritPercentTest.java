package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-154 — SPELL_AURA_MOD_CRIT_PERCENT (52). Recklessness 1719 Effect 1:
 * EquippedItemClass −1 → +100% (EffectBasePoints 99 + 1) on melee/offhand/ranged crit fields.
 * CMaNGOS HandleAuraModCritPercent → HandleBaseModValue(CRIT/OFFHAND/RANGED, FLAT_MOD).
 */
class AuraEngineModCritPercentTest {
    private static final SpellEngine.SpellInfo RECKLESSNESS = new SpellEngine.SpellInfo(
            SpellEngine.RECKLESSNESS, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_CRIT_PERCENT, 0, 0, 100, 100, 0f);

    @Test
    void applyAuraWhenModCritPercentOnPlayerShouldRaiseAllWeaponCritFields() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_CRIT_PERCENT));
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE, 5f);
        player.setFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE, 5f);
        player.setFloat(UpdateFields.PLAYER_RANGED_CRIT_PERCENTAGE, 5f);

        eng.apply(player, player, RECKLESSNESS);

        assertTrue(player.hasAura(SpellEngine.RECKLESSNESS));
        assertEquals(105f, player.getFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE));
        assertEquals(105f, player.getFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE));
        assertEquals(105f, player.getFloat(UpdateFields.PLAYER_RANGED_CRIT_PERCENTAGE));
    }

    @Test
    void unapplyWhenModCritPercentOnPlayerShouldRestoreCritFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE, 5f);
        player.setFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE, 5f);
        player.setFloat(UpdateFields.PLAYER_RANGED_CRIT_PERCENTAGE, 5f);
        eng.apply(player, player, RECKLESSNESS);
        eng.unapplyAura(player, SpellEngine.RECKLESSNESS);
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_RANGED_CRIT_PERCENTAGE));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveCritFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999052, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_CRIT_PERCENT,
                0, 0, 0, 0, 0f));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE));
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerCritFields() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, RECKLESSNESS);
        assertTrue(mob.hasAura(SpellEngine.RECKLESSNESS));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, RECKLESSNESS);
        new AuraEngine().unapply(null, RECKLESSNESS);
    }
}
