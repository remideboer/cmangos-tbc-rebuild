package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-207 — SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE (143).
 * CMaNGOS HandleAuraModResistanceExclusive: per school, the highest amount wins.
 */
class AuraEngineModResistanceExclusiveTest {
    private static final SpellEngine.SpellInfo LARGER = new SpellEngine.SpellInfo(
            999143, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE, 0, 0, 40, 40, 0f, 1);
    private static final SpellEngine.SpellInfo SMALLER = new SpellEngine.SpellInfo(
            999144, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE, 0, 0, 10, 10, 0f, 1);

    @Test
    void applyAuraWhenExclusiveResistShouldKeepTheHighestAmount() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, SMALLER);
        eng.apply(player, player, LARGER);
        assertEquals(140, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(40, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void unapplyWhenLargerExclusiveResistDropsShouldRevealTheSmaller() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, SMALLER);
        eng.apply(player, player, LARGER);
        eng.auras().unapply(player, LARGER);
        assertEquals(110, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(10, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
        eng.auras().unapply(player, SMALLER);
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void applyWhenZeroAmountOrEmptyMaskShouldNoOp() {
        new AuraEngine().apply(null, LARGER);
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999145, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE,
                0, 0, 0, 0, 0f, 1));
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999146, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE,
                0, 0, 25, 25, 0f, 0));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }
}
