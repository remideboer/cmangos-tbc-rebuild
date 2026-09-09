package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-108 — SPELL_EFFECT_TRADE_SKILL (47). Blacksmithing 2018 effect 1.
 * CMaNGOS EffectTradeSkill: body commented out — the profession skill (164) comes from
 * Apprentice Blacksmith 2020 SKILL_STEP, not from this effect. Cataloged, mutates nothing.
 */
class SpellEngineTradeSkillTest {
    private static final int SKILL_BLACKSMITHING = 164;

    @Test
    void applyTradeSkillWhenPlayerShouldBeCatalogedAndGrantNoSkill() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TRADE_SKILL));
        Player p = new Player();
        p.spells.add(2018);
        SpellEngine.SpellInfo blacksmithing = new SpellEngine.SpellInfo(
                2018, SpellEngine.EFFECT_TRADE_SKILL, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, p, blacksmithing));
        assertFalse(p.hasSkill(SKILL_BLACKSMITHING));
        assertEquals(1, p.spells.size());
        assertEquals(0, p.auras.size());
    }
}
