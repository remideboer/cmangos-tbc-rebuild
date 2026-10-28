package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-218 — SPELL_AURA_MOD_EXPERTISE (240).
 * CMaNGOS HandleAuraModExpertise → UpdateExpertise for base and off hand.
 */
class AuraEngineModExpertiseTest {
    private static SpellEngine.SpellInfo expertise(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_EXPERTISE, 0, 0, amount, amount, 0f, 0, -1);
    }

    @Test
    void applyAuraWhenExpertiseShouldFillBothHands() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_EXPERTISE));
        Player player = new Player();
        eng.apply(player, player, expertise(999240, 5));
        assertEquals(5, player.getInt(UpdateFields.PLAYER_EXPERTISE));
        assertEquals(5, player.getInt(UpdateFields.PLAYER_OFFHAND_EXPERTISE));
    }

    @Test
    void unapplyWhenExpertiseStacksShouldLeaveTheOtherAura() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo first = expertise(999240, 5);
        eng.apply(player, player, first);
        eng.apply(player, player, expertise(999241, 3));
        eng.auras().unapply(player, first);
        assertEquals(3, player.getInt(UpdateFields.PLAYER_EXPERTISE));
        assertEquals(3, player.getInt(UpdateFields.PLAYER_OFFHAND_EXPERTISE));
    }

    @Test
    void applyWhenItemClassSetCreatureOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, expertise(999240, 5));
        new SpellEngine().apply(new Player(), new Creature(), expertise(999240, 5));
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999242, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_EXPERTISE,
                0, 0, 5, 5, 0f));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_EXPERTISE));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_OFFHAND_EXPERTISE));
    }
}
