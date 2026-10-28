package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-238 — SPELL_AURA_MECHANIC_IMMUNITY (77).
 * Stores the mechanic id. Two auras of the same mechanic stay immune until both drop.
 */
class AuraEngineMechanicImmunityTest {
    private static SpellEngine.SpellInfo immune(int id, int mechanic) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MECHANIC_IMMUNITY, 0, 0, 1, 1, 0f, mechanic);
    }

    @Test
    void applyAuraWhenMechanicImmunityShouldRecordTheMechanic() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MECHANIC_IMMUNITY));
        Player player = new Player();
        SpellEngine.SpellInfo stun = immune(999077, 12);
        SpellEngine.SpellInfo stunAgain = immune(999078, 12);
        eng.apply(player, player, stun);
        eng.apply(player, player, stunAgain);
        assertTrue(player.isImmuneToMechanic(12));
        assertFalse(player.isImmuneToMechanic(1));
        eng.auras().unapply(player, stun);
        assertTrue(player.isImmuneToMechanic(12));
        eng.auras().unapply(player, stunAgain);
        assertFalse(player.isImmuneToMechanic(12));
    }
}
