package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-073 — SPELL_EFFECT_CREATE_PET (153). Create Tamed Warp Stalker 46686 misc 26037. */
class SpellEngineCreatePetTest {
    @Test
    void applyCreatePetWhenHunterShouldSummonTamedEntry() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_CREATE_PET));
        Player hunter = new Player();
        hunter.clazz = Player.CLASS_HUNTER;
        SpellEngine.SpellInfo create = new SpellEngine.SpellInfo(
                46686, SpellEngine.EFFECT_CREATE_PET, 0, 0, 0, 0, 0, 0f, 26037);
        eng.apply(new Creature(), hunter, create);
        assertNotNull(hunter.pet);
        assertEquals(26037, hunter.pet.entry);
        assertTrue(hunter.pet.summoned);
    }

    @Test
    void applyCreatePetWhenNonHunterMissingEntryOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo create = new SpellEngine.SpellInfo(
                46686, SpellEngine.EFFECT_CREATE_PET, 0, 0, 0, 0, 0, 0f, 26037);
        Player warrior = new Player();
        warrior.clazz = 1;
        eng.apply(new Creature(), warrior, create);
        assertNull(warrior.pet);
        eng.apply(new Creature(), new Creature(), create);
        Player hunter = new Player();
        hunter.clazz = Player.CLASS_HUNTER;
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                46686, SpellEngine.EFFECT_CREATE_PET, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Creature(), hunter, none);
        assertNull(hunter.pet);
        eng.createTamedPet(null, 26037);
        eng.createTamedPet(hunter, 0);
    }
}
