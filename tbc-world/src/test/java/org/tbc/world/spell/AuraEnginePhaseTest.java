package org.tbc.world.spell;

import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-244 — SPELL_AURA_PHASE (261). */
class AuraEnginePhaseTest {
    @Test
    void applyAuraWhenPhaseShouldReplaceTheMaskAndResetOnUnapply() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PHASE));
        Player player = new Player();
        player.pet = new Pet();
        assertEquals(1, player.phaseMask());
        SpellEngine.SpellInfo first = aura(999261, 2);
        SpellEngine.SpellInfo second = aura(999262, 4);
        eng.apply(player, player, first);
        assertEquals(2, player.phaseMask());
        assertEquals(2, player.pet.asUnit().phaseMask());
        eng.apply(player, player, second);
        assertEquals(4, player.phaseMask());
        assertEquals(4, player.pet.asUnit().phaseMask());
        eng.auras().unapply(player, second);
        assertEquals(1, player.phaseMask());
        assertEquals(1, player.pet.asUnit().phaseMask());
    }

    private static SpellEngine.SpellInfo aura(int id, int mask) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_PHASE, 0, 0, 0, 0, 0f, mask);
    }
}
