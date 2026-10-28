package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-241 — SPELL_AURA_STATE_IMMUNITY (38). */
class AuraEngineStateImmunityTest {
    @Test
    void applyAuraWhenStateImmunityShouldTrackEachAuraType() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_STATE_IMMUNITY));
        Player player = new Player();
        SpellEngine.SpellInfo stun = aura(999038, 12);
        SpellEngine.SpellInfo root = aura(999039, 26);
        eng.apply(player, player, stun);
        eng.apply(player, player, root);
        assertTrue(player.isImmuneToAuraState(12));
        assertTrue(player.isImmuneToAuraState(26));
        assertFalse(player.isImmuneToMechanic(12));
        eng.auras().unapply(player, stun);
        assertFalse(player.isImmuneToAuraState(12));
        assertTrue(player.isImmuneToAuraState(26));
        eng.auras().apply(null, stun);
        eng.auras().unapply(player, root);
        assertFalse(player.isImmuneToAuraState(26));
    }

    private static SpellEngine.SpellInfo aura(int id, int auraType) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_STATE_IMMUNITY, 0, 0, 0, 0, 0f, auraType);
    }
}
