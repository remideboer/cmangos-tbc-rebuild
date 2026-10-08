package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-181 — SPELL_AURA_MOD_SHIELD_BLOCKVALUE (158).
 * CMaNGOS HandleShieldBlockValue FLAT_MOD → PLAYER_SHIELD_BLOCK.
 */
class AuraEngineModShieldBlockValueTest {
    private static final SpellEngine.SpellInfo BLOCK_VAL = new SpellEngine.SpellInfo(
            999158, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SHIELD_BLOCKVALUE, 0, 0, 30, 30, 0f);

    @Test
    void applyAuraWhenModShieldBlockValueOnPlayerShouldRaiseShieldBlock() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SHIELD_BLOCKVALUE));
        Player player = new Player();
        player.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 10);

        eng.apply(player, player, BLOCK_VAL);

        assertTrue(player.hasAura(999158));
        assertEquals(40, player.getInt(UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    @Test
    void unapplyWhenModShieldBlockValueShouldRestoreShieldBlock() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 10);
        eng.apply(player, player, BLOCK_VAL);
        eng.auras().unapply(player, BLOCK_VAL);
        assertEquals(10, player.getInt(UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveShieldBlock() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 10);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999159, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SHIELD_BLOCKVALUE,
                0, 0, 0, 0, 0f));
        assertEquals(10, player.getInt(UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    @Test
    void applyWhenCreatureShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, BLOCK_VAL);
        assertTrue(mob.hasAura(999158));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BLOCK_VAL);
        new AuraEngine().unapply(null, BLOCK_VAL);
    }
}
