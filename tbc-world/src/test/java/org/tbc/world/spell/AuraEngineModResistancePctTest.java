package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-185 — SPELL_AURA_MOD_RESISTANCE_PCT (101).
 * CMaNGOS HandleModResistancePercent → TOTAL_PCT per school bit + resistance buff columns.
 */
class AuraEngineModResistancePctTest {
    /** Synthetic +30% physical armor. */
    private static final SpellEngine.SpellInfo ARMOR_PCT = new SpellEngine.SpellInfo(
            999101, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_RESISTANCE_PCT, 0, 0, 30, 30, 0f, 1);

    @Test
    void applyAuraWhenModResistancePctShouldRaiseArmorAndPositiveBuff() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RESISTANCE_PCT));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE, 0);

        eng.apply(player, player, ARMOR_PCT);

        assertTrue(player.hasAura(999101));
        assertEquals(130, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(30, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void unapplyWhenModResistancePctShouldRestoreArmorAndBuff() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, ARMOR_PCT);
        eng.auras().unapply(player, ARMOR_PCT);
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void applyWhenSchoolMaskBitShouldTouchOnlyThatSchool() {
        SpellEngine eng = new SpellEngine();
        Unit u = new Player();
        u.setInt(UpdateFields.UNIT_FIELD_RESISTANCES + 4, 50);
        eng.apply(u, u, new SpellEngine.SpellInfo(
                999102, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE_PCT,
                0, 0, 20, 20, 0f, 1 << 4));
        assertEquals(0, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(60, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 4));
    }

    @Test
    void applyWhenNegativeAmountShouldLowerArmorAndNegativeBuff() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999103, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE_PCT,
                0, 0, -20, -20, 0f, 1));
        assertEquals(80, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-20, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE));
    }

    @Test
    void applyWhenAmountZeroOrMaskZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999104, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE_PCT,
                0, 0, 0, 0, 0f, 1));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999105, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE_PCT,
                0, 0, 30, 30, 0f, 0));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, ARMOR_PCT);
        new AuraEngine().unapply(null, ARMOR_PCT);
    }
}
