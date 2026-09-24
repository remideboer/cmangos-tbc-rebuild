package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aura 22 SPELL_AURA_MOD_RESISTANCE — Frost Armor rank 1 (+30 armor, school mask physical).
 */
class AuraEngineModResistanceTest {
    private static final SpellEngine.SpellInfo FROST_ARMOR = new SpellEngine.SpellInfo(
            SpellEngine.FROST_ARMOR, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RESISTANCE,
            16, 60, 30, 30, 0f, 1);

    @Test
    void applyWhenFrostArmorShouldRaiseArmorAndPositiveBuffMod() {
        AuraEngine auras = new AuraEngine();
        assertTrue(auras.knownAura(AuraEngine.SPELL_AURA_MOD_RESISTANCE));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE, 0);
        auras.apply(p, FROST_ARMOR);
        assertEquals(70, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(30, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void unapplyWhenFrostArmorShouldRestoreArmor() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        auras.apply(p, FROST_ARMOR);
        auras.unapply(p, FROST_ARMOR);
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void applyWhenAmountZeroOrNullShouldNoOp() {
        AuraEngine auras = new AuraEngine();
        auras.apply(null, FROST_ARMOR);
        auras.unapply(null, FROST_ARMOR);
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 10);
        auras.apply(p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RESISTANCE, 0, 0, 0, 0, 0f, 1));
        assertEquals(10, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        auras.apply(p, null);
        assertEquals(10, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenSchoolMaskBitShouldTouchOnlyThatSchool() {
        AuraEngine auras = new AuraEngine();
        Unit u = new Player();
        u.setInt(UpdateFields.UNIT_FIELD_RESISTANCES + 4, 5);
        auras.apply(u, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RESISTANCE, 0, 0, 20, 20, 0f, 1 << 4));
        assertEquals(0, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(25, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 4));
    }

    @Test
    void applyWhenNegativeAmountShouldLowerArmorAndNegativeBuffMod() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        auras.apply(p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RESISTANCE, 0, 0, -10, -10, 0f, 1));
        assertEquals(30, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-10, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE));
    }
}
