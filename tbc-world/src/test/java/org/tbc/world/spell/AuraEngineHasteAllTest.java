package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-204 — SPELL_AURA_HASTE_ALL (193).
 * CMaNGOS HandleModCombatSpeedPct → cast speed plus BASE, OFF, and RANGED.
 */
class AuraEngineHasteAllTest {
    private static final SpellEngine.SpellInfo HASTE = new SpellEngine.SpellInfo(
            999193, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_HASTE_ALL, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenHasteAllShouldShortenAttacksAndCasts() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_HASTE_ALL));
        Player player = new Player();
        eng.apply(player, player, HASTE);
        int expected = Math.round(2000 * 100.0f / 120.0f);
        assertEquals(expected, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(expected, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
        assertEquals(expected, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
        assertEquals(100.0f / 120.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), 1e-5f);
    }

    @Test
    void unapplyWhenHasteAllShouldRestoreAttacksAndCasts() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, HASTE);
        eng.auras().unapply(player, HASTE);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), 1e-5f);
    }

    @Test
    void applyWhenAmountZeroOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HASTE);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999194, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_HASTE_ALL,
                0, 0, 0, 0, 0f));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), 1e-5f);
    }
}
