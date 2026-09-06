package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-097 — SPELL_EFFECT_SKILL_STEP (44). Apprentice Blacksmith 2020 misc 164 step 1.
 * CMaNGOS EffectLearnSkill / SetSkillStep.
 */
class SpellEngineSkillStepTest {
    @Test
    void applySkillStepWhenPlayerShouldLearnBlacksmithingStep() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SKILL_STEP));
        Player p = new Player();
        SpellEngine.SpellInfo apprentice = new SpellEngine.SpellInfo(
                2020, SpellEngine.EFFECT_SKILL_STEP, 0, 0, 0, 1, 1, 0f, 164);
        eng.apply(p, p, apprentice);
        assertTrue(p.hasSkill(164));
        assertEquals(1, p.skillStep(164));
    }

    @Test
    void applySkillStepWhenNonPlayerOrInvalidShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo apprentice = new SpellEngine.SpellInfo(
                2020, SpellEngine.EFFECT_SKILL_STEP, 0, 0, 0, 1, 1, 0f, 164);
        Creature npc = new Creature();
        eng.apply(npc, npc, apprentice);
        Player p = new Player();
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                2020, SpellEngine.EFFECT_SKILL_STEP, 0, 0, 0, 1, 1, 0f, 0);
        eng.apply(p, p, none);
        assertFalse(p.hasSkill(164));
        SpellEngine.SpellInfo zeroStep = new SpellEngine.SpellInfo(
                2020, SpellEngine.EFFECT_SKILL_STEP, 0, 0, 0, 0, 0, 0f, 164);
        eng.apply(p, p, zeroStep);
        assertFalse(p.hasSkill(164));
        eng.skillStep(null, 164, 1);
        eng.skillStep(p, 164, 17);
        assertFalse(p.hasSkill(164));
    }
}
