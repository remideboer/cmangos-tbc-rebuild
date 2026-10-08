package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-184 — SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT (150).
 * CMaNGOS HandleShieldBlockValue PCT_MOD → PLAYER_SHIELD_BLOCK × (100+amount)/100.
 */
class AuraEngineModShieldBlockValuePctTest {
    private static final SpellEngine.SpellInfo BLOCK_PCT = new SpellEngine.SpellInfo(
            999150, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT, 0, 0, 30, 30, 0f);

    @Test
    void applyAuraWhenModShieldBlockValuePctShouldRaiseShieldBlock() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT));
        Player player = new Player();
        player.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 100);

        eng.apply(player, player, BLOCK_PCT);

        assertTrue(player.hasAura(999150));
        assertEquals(130, player.getInt(UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    @Test
    void unapplyWhenModShieldBlockValuePctShouldRestoreShieldBlock() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 100);
        eng.apply(player, player, BLOCK_PCT);
        eng.auras().unapply(player, BLOCK_PCT);
        assertEquals(100, player.getInt(UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveShieldBlock() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999151, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT,
                0, 0, 0, 0, 0f));
        assertEquals(100, player.getInt(UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    @Test
    void applyWhenCreatureShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, BLOCK_PCT);
        assertTrue(mob.hasAura(999150));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BLOCK_PCT);
        new AuraEngine().unapply(null, BLOCK_PCT);
    }
}
