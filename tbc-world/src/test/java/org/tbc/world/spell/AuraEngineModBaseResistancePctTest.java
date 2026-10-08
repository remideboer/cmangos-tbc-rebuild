package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-188 — SPELL_AURA_MOD_BASE_RESISTANCE_PCT (142).
 * CMaNGOS HandleAuraModBaseResistancePercent → BASE_PCT on school resist
 * (UNIT_FIELD_RESISTANCES only; no sheet buff columns).
 */
class AuraEngineModBaseResistancePctTest {
    private static final SpellEngine.SpellInfo BASE_ARMOR_PCT = new SpellEngine.SpellInfo(
            999142, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE_PCT, 0, 0, 25, 25, 0f, 1);

    @Test
    void applyAuraWhenModBaseResistancePctShouldRaiseArmorWithoutBuffColumn() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE_PCT));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE, 0);

        eng.apply(player, player, BASE_ARMOR_PCT);

        assertTrue(player.hasAura(999142));
        assertEquals(125, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void unapplyWhenModBaseResistancePctShouldRestoreArmor() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, BASE_ARMOR_PCT);
        eng.auras().unapply(player, BASE_ARMOR_PCT);
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenSchoolMaskBitShouldTouchOnlyThatSchool() {
        SpellEngine eng = new SpellEngine();
        Unit u = new Player();
        u.setInt(UpdateFields.UNIT_FIELD_RESISTANCES + 3, 80);
        eng.apply(u, u, new SpellEngine.SpellInfo(
                999143, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE_PCT,
                0, 0, 50, 50, 0f, 1 << 3));
        assertEquals(0, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(120, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 3));
    }

    @Test
    void applyWhenAmountZeroOrMaskZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999144, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE_PCT,
                0, 0, 0, 0, 0f, 1));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999145, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE_PCT,
                0, 0, 25, 25, 0f, 0));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BASE_ARMOR_PCT);
        new AuraEngine().unapply(null, BASE_ARMOR_PCT);
    }
}
