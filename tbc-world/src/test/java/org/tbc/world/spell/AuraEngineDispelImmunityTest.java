package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-242 — SPELL_AURA_DISPEL_IMMUNITY (41). */
class AuraEngineDispelImmunityTest {
    @Test
    void applyAuraWhenDispelImmunityShouldMatchTheDispelType() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_DISPEL_IMMUNITY));
        Player player = new Player();
        SpellEngine.SpellInfo magic = aura(999041, 1);
        SpellEngine.SpellInfo curse = aura(999042, 2);
        eng.apply(player, player, magic);
        eng.apply(player, player, curse);
        assertTrue(player.isImmuneToDispel(1));
        assertTrue(player.isImmuneToDispel(2));
        assertFalse(player.isImmuneToDispel(3));
        eng.auras().unapply(player, magic);
        assertFalse(player.isImmuneToDispel(1));
        assertTrue(player.isImmuneToDispel(2));
    }

    private static SpellEngine.SpellInfo aura(int id, int dispelType) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_DISPEL_IMMUNITY, 0, 0, 0, 0, 0f, dispelType);
    }
}
