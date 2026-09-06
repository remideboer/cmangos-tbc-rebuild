package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-062 — SPELL_EFFECT_LEARN_PET_SPELL (57). Fire Shield 2949 teaches 2947. */
class SpellEngineLearnPetSpellTest {
    @Test
    void applyLearnPetSpellWhenLivingPetShouldAddTriggerSpell() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_LEARN_PET_SPELL));
        Player caster = new Player();
        caster.pet = new Pet();
        caster.pet.summoned = true;
        SpellEngine.SpellInfo teach = new SpellEngine.SpellInfo(
                2949, SpellEngine.EFFECT_LEARN_PET_SPELL, 0, 0, 0, 0, 0, 0f, 2947);
        eng.apply(caster, new Creature(), teach);
        assertTrue(caster.pet.spells.contains(2947));
    }

    @Test
    void applyLearnPetSpellWhenMissingDeadOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo teach = new SpellEngine.SpellInfo(
                2949, SpellEngine.EFFECT_LEARN_PET_SPELL, 0, 0, 0, 0, 0, 0f, 2947);
        eng.apply(caster, new Creature(), teach);
        Pet dead = new Pet();
        dead.summoned = false;
        caster.pet = dead;
        eng.apply(caster, new Creature(), teach);
        assertFalse(dead.spells.contains(2947));
        Creature npc = new Creature();
        eng.apply(npc, new Creature(), teach);
    }
}
