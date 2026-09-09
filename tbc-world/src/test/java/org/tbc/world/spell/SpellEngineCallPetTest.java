package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-113 — SPELL_EFFECT_CALL_PET (135). Call Pet 23498 (one spell).
 * CMaNGOS EffectNULL: the hunter's real Call Pet 883 is SUMMON_PET. Cataloged, summons nothing.
 */
class SpellEngineCallPetTest {
    @Test
    void applyCallPetWhenHunterHasNoPetShouldBeCatalogedAndSummonNothing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_CALL_PET));
        Player hunter = new Player();
        SpellEngine.SpellInfo callPet = new SpellEngine.SpellInfo(
                23498, SpellEngine.EFFECT_CALL_PET, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(hunter, hunter, callPet));
        assertNull(hunter.pet);
        assertEquals(0, hunter.auras.size());
    }
}
