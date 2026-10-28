package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-243 — SPELL_AURA_MECHANIC_IMMUNITY_MASK (147). */
class AuraEngineMechanicImmunityMaskTest {
    @Test
    void applyAuraWhenMechanicMaskShouldMatchTheMechanicBit() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MECHANIC_IMMUNITY_MASK));
        Player player = new Player();
        SpellEngine.SpellInfo stun = aura(999147, 1 << 12);
        SpellEngine.SpellInfo root = aura(999148, 1 << 7);
        eng.apply(player, player, stun);
        eng.apply(player, player, root);
        assertTrue(player.isImmuneToMechanicMask(12));
        assertTrue(player.isImmuneToMechanicMask(7));
        assertFalse(player.isImmuneToMechanicMask(1));
        assertFalse(player.isImmuneToMechanic(12));
        assertFalse(player.isImmuneToMechanicMask(-1));
        assertFalse(player.isImmuneToMechanicMask(32));
        eng.auras().unapply(player, stun);
        assertFalse(player.isImmuneToMechanicMask(12));
        assertTrue(player.isImmuneToMechanicMask(7));
    }

    private static SpellEngine.SpellInfo aura(int id, int mask) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MECHANIC_IMMUNITY_MASK, 0, 0, 0, 0, 0f, mask);
    }
}
