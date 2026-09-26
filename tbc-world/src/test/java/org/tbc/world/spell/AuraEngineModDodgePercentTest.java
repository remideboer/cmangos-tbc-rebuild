package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-155 — SPELL_AURA_MOD_DODGE_PERCENT (49). Evasion 5277:
 * EffectBasePoints 49 + 1 = +50% dodge. CMaNGOS HandleAuraModDodgePercent → UpdateDodgePercentage.
 */
class AuraEngineModDodgePercentTest {
    private static final SpellEngine.SpellInfo EVASION = new SpellEngine.SpellInfo(
            SpellEngine.EVASION, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_DODGE_PERCENT, 0, 0, 50, 50, 0f);

    @Test
    void applyAuraWhenModDodgePercentOnPlayerShouldRaiseDodgePercentage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_DODGE_PERCENT));
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 5f);

        eng.apply(player, player, EVASION);

        assertTrue(player.hasAura(SpellEngine.EVASION));
        assertEquals(55f, player.getFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE));
    }

    @Test
    void unapplyWhenModDodgePercentOnPlayerShouldRestoreDodgePercentage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 5f);
        eng.apply(player, player, EVASION);
        eng.unapplyAura(player, SpellEngine.EVASION);
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveDodgePercentage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999049, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DODGE_PERCENT,
                0, 0, 0, 0, 0f));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE));
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerDodgeField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, EVASION);
        assertTrue(mob.hasAura(SpellEngine.EVASION));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, EVASION);
        new AuraEngine().unapply(null, EVASION);
    }
}
