package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-109 — SPELL_EFFECT_SKILL (118). Blacksmithing 2018 effect 2 misc 164.
 * CMaNGOS EffectSkill: DEBUG_LOG only — skill value comes from SkillLineAbility on learn.
 * Cataloged, mutates nothing.
 */
class SpellEngineSkillTest {
    private static final int SKILL_BLACKSMITHING = 164;

    @Test
    void applySkillWhenPlayerShouldBeCatalogedAndGrantNoSkill() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SKILL));
        Player p = new Player();
        SpellEngine.SpellInfo blacksmithing = new SpellEngine.SpellInfo(
                2018, SpellEngine.EFFECT_SKILL, 0, 0, 0, 0, 0, 0f, SKILL_BLACKSMITHING);
        assertEquals(0, eng.apply(p, p, blacksmithing));
        assertFalse(p.hasSkill(SKILL_BLACKSMITHING));
        assertEquals(0, p.auras.size());
    }
}
