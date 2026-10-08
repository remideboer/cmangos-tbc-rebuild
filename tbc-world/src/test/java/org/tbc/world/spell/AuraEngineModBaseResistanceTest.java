package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-187 — SPELL_AURA_MOD_BASE_RESISTANCE (83).
 * CMaNGOS HandleModBaseResistance → BASE_VALUE on UNIT_MOD_RESISTANCE_START+i
 * (UNIT_FIELD_RESISTANCES only; no sheet buff columns).
 */
class AuraEngineModBaseResistanceTest {
    private static final SpellEngine.SpellInfo BASE_ARMOR = new SpellEngine.SpellInfo(
            999083, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE, 0, 0, 40, 40, 0f, 1);

    @Test
    void applyAuraWhenModBaseResistanceShouldRaiseArmorWithoutBuffColumn() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE, 0);

        eng.apply(player, player, BASE_ARMOR);

        assertTrue(player.hasAura(999083));
        assertEquals(140, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void unapplyWhenModBaseResistanceShouldRestoreArmor() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, BASE_ARMOR);
        eng.auras().unapply(player, BASE_ARMOR);
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenSchoolMaskBitShouldTouchOnlyThatSchool() {
        SpellEngine eng = new SpellEngine();
        Unit u = new Player();
        u.setInt(UpdateFields.UNIT_FIELD_RESISTANCES + 2, 10);
        eng.apply(u, u, new SpellEngine.SpellInfo(
                999084, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE,
                0, 0, 15, 15, 0f, 1 << 2));
        assertEquals(0, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(25, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 2));
    }

    @Test
    void applyWhenAmountZeroOrMaskZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999085, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE,
                0, 0, 0, 0, 0f, 1));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999086, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_BASE_RESISTANCE,
                0, 0, 40, 40, 0f, 0));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BASE_ARMOR);
        new AuraEngine().unapply(null, BASE_ARMOR);
    }
}
