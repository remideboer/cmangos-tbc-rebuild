package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-227 — SPELL_AURA_HASTE_RANGED (218).
 * CMaNGOS uses HandleAuraModRangedHaste, the same path as aura 140.
 */
class AuraEngineHasteRangedTest {
    private static final SpellEngine.SpellInfo HASTE = new SpellEngine.SpellInfo(
            999218, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_HASTE_RANGED, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenHasteRangedShouldShortenRangedAttackTime() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_HASTE_RANGED));
        Player player = new Player();
        eng.apply(player, player, HASTE);
        assertEquals(Math.round(2000 * 100f / 120f), player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    @Test
    void unapplyWhenHasteRangedShouldRestoreRangedAttackTime() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, HASTE);
        eng.auras().unapply(player, HASTE);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }

    @Test
    void applyWhenAmountZeroOrTargetMissingShouldLeaveAttackTime() {
        new AuraEngine().apply(null, HASTE);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999219, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_HASTE_RANGED,
                0, 0, 0, 0, 0f));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }
}
