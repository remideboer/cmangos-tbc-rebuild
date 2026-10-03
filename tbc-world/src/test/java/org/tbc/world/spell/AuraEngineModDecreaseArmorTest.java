package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aura 15 SPELL_AURA_MOD_DECREASE_ARMOR — Faerie Fire 25602 (physical armor).
 */
class AuraEngineModDecreaseArmorTest {
    private static final SpellEngine.SpellInfo FAERIE = new SpellEngine.SpellInfo(
            25602, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DECREASE_ARMOR,
            0, 0, -175, -175, 30f);

    @Test
    void applyWhenFaerieFireShouldLowerPhysicalArmor() {
        AuraEngine auras = new AuraEngine();
        assertTrue(auras.knownAura(AuraEngine.SPELL_AURA_MOD_DECREASE_ARMOR));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 200);
        auras.apply(p, FAERIE);
        assertEquals(25, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-175, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE));
    }

    @Test
    void unapplyWhenFaerieFireShouldRestoreArmor() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 200);
        auras.apply(p, FAERIE);
        auras.unapply(p, FAERIE);
        assertEquals(200, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE));
    }

    @Test
    void applyWhenAmountZeroOrNullShouldNoOp() {
        AuraEngine auras = new AuraEngine();
        auras.apply(null, FAERIE);
        auras.unapply(null, FAERIE);
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        auras.apply(p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_DECREASE_ARMOR, 0, 0, 0, 0, 0f));
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        auras.apply(p, null);
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void applyWhenPositiveAmountShouldRaiseArmorAndPositiveBuff() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        auras.apply(p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_DECREASE_ARMOR, 0, 0, 10, 10, 0f));
        assertEquals(50, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(10, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }
}
