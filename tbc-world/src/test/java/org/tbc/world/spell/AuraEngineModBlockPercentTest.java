package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-157 — SPELL_AURA_MOD_BLOCK_PERCENT (51). Shield Block 2565:
 * EffectBasePoints 74 + 1 = +75% block. CMaNGOS HandleAuraModBlockPercent → UpdateBlockPercentage.
 */
class AuraEngineModBlockPercentTest {
    private static final SpellEngine.SpellInfo SHIELD_BLOCK = new SpellEngine.SpellInfo(
            SpellEngine.SHIELD_BLOCK, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_BLOCK_PERCENT, 0, 0, 75, 75, 0f);

    @Test
    void applyAuraWhenModBlockPercentOnPlayerShouldRaiseBlockPercentage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_BLOCK_PERCENT));
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE, 5f);

        eng.apply(player, player, SHIELD_BLOCK);

        assertTrue(player.hasAura(SpellEngine.SHIELD_BLOCK));
        assertEquals(80f, player.getFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE));
    }

    @Test
    void unapplyWhenModBlockPercentOnPlayerShouldRestoreBlockPercentage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE, 5f);
        eng.apply(player, player, SHIELD_BLOCK);
        eng.unapplyAura(player, SpellEngine.SHIELD_BLOCK);
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveBlockPercentage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999051, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BLOCK_PERCENT,
                0, 0, 0, 0, 0f));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE));
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerBlockField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, SHIELD_BLOCK);
        assertTrue(mob.hasAura(SpellEngine.SHIELD_BLOCK));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, SHIELD_BLOCK);
        new AuraEngine().unapply(null, SHIELD_BLOCK);
    }
}
