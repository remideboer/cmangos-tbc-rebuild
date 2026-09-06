package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-083 — SPELL_EFFECT_SUMMON_PET (56). Summon Imp 688 misc 416.
 * CMaNGOS EffectSummonPet: warlock creates from misc; hunter LoadPetFromDB (no saved pet → no-op).
 */
class SpellEngineSummonPetTest {
    @Test
    void applySummonPetWhenWarlockShouldCreateImpFromMisc() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_PET));
        Player lock = new Player();
        lock.clazz = Player.CLASS_WARLOCK;
        SpellEngine.SpellInfo imp = new SpellEngine.SpellInfo(
                688, SpellEngine.EFFECT_SUMMON_PET, 0, 0, 0, 0, 0, 0f, 416);
        eng.apply(lock, lock, imp);
        assertNotNull(lock.pet);
        assertEquals(416, lock.pet.entry);
        assertTrue(lock.pet.summoned);
    }

    @Test
    void applySummonPetWhenHunterShouldResummonSavedPetOrNoOp() {
        SpellEngine eng = new SpellEngine();
        Player hunter = new Player();
        hunter.clazz = Player.CLASS_HUNTER;
        SpellEngine.SpellInfo call = new SpellEngine.SpellInfo(
                883, SpellEngine.EFFECT_SUMMON_PET, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(hunter, hunter, call);
        assertNull(hunter.pet);
        Pet saved = new Pet();
        saved.entry = 26037;
        saved.summoned = false;
        hunter.pet = saved;
        eng.apply(hunter, hunter, call);
        assertTrue(hunter.pet.summoned);
        assertEquals(26037, hunter.pet.entry);
        eng.apply(hunter, hunter, call);
        assertTrue(hunter.pet.summoned);
        Player lock = new Player();
        lock.clazz = Player.CLASS_WARLOCK;
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                688, SpellEngine.EFFECT_SUMMON_PET, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(lock, lock, none);
        assertNull(lock.pet);
        Creature npc = new Creature();
        eng.apply(npc, npc, call);
        eng.summonPet(null, 416);
    }
}
