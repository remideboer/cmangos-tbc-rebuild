package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-063 — SPELL_EFFECT_SUMMON_DEAD_PET (109). Revive Pet 982. */
class SpellEngineSummonDeadPetTest {
    @Test
    void applySummonDeadPetWhenDeadPetShouldResummon() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_DEAD_PET));
        Player caster = new Player();
        caster.pet = new Pet();
        caster.pet.summoned = false;
        SpellEngine.SpellInfo revive = new SpellEngine.SpellInfo(982, SpellEngine.EFFECT_SUMMON_DEAD_PET, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), revive);
        assertNotNull(caster.pet);
        assertTrue(caster.pet.summoned);
    }

    @Test
    void applySummonDeadPetWhenLivingMissingOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo revive = new SpellEngine.SpellInfo(982, SpellEngine.EFFECT_SUMMON_DEAD_PET, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), revive);
        assertNull(caster.pet);
        Pet live = new Pet();
        live.summoned = true;
        caster.pet = live;
        eng.apply(caster, new Creature(), revive);
        assertTrue(live.summoned);
        Creature npc = new Creature();
        eng.apply(npc, new Creature(), revive);
    }
}
