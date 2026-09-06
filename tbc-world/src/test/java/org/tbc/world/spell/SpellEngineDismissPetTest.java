package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-048 — SPELL_EFFECT_DISMISS_PET (102). Dismiss Pet 2641 unsummons a living pet. */
class SpellEngineDismissPetTest {
    @Test
    void applyDismissPetWhenLivingPetShouldUnsummon() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DISMISS_PET));
        Player caster = new Player();
        caster.pet = new Pet();
        caster.pet.summoned = true;
        SpellEngine.SpellInfo dismiss = new SpellEngine.SpellInfo(
                2641, SpellEngine.EFFECT_DISMISS_PET, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), dismiss);
        assertNull(caster.pet);
    }

    @Test
    void applyDismissPetWhenMissingDeadOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo dismiss = new SpellEngine.SpellInfo(
                2641, SpellEngine.EFFECT_DISMISS_PET, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), dismiss);
        assertNull(caster.pet);
        Pet dead = new Pet();
        dead.summoned = false;
        caster.pet = dead;
        eng.apply(caster, new Creature(), dismiss);
        assertNotNull(caster.pet);
        Creature npc = new Creature();
        eng.apply(npc, new Creature(), dismiss);
    }
}
