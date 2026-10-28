package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-240 — SPELL_AURA_DAMAGE_IMMUNITY (40).
 * School-mask damage immunity. Does not set UNIT_FLAG_IMMUNE (that flag is aura 39).
 */
class AuraEngineDamageImmunityTest {
    @Test
    void applyAuraWhenDamageImmunityShouldMatchOverlappingSchools() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_DAMAGE_IMMUNITY));
        Player player = new Player();
        int before = player.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        SpellEngine.SpellInfo aura = new SpellEngine.SpellInfo(
                999040, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_DAMAGE_IMMUNITY, 0, 0, 1, 1, 0f, 4);
        eng.apply(player, player, aura);
        assertTrue(player.isImmuneToDamage(4));
        assertTrue(player.isImmuneToDamage(5));
        assertFalse(player.isImmuneToDamage(2));
        assertEquals(before, player.getInt(UpdateFields.UNIT_FIELD_FLAGS));
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_IMMUNE) == 0);
        eng.auras().unapply(player, aura);
        assertFalse(player.isImmuneToDamage(4));
    }
}
