package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-110 — SPELL_EFFECT_WEAPON (25). Axes 196 (spell per weapon type).
 * CMaNGOS EffectEmpty: usability comes from the skill line, not this effect. Cataloged, mutates nothing.
 */
class SpellEngineWeaponTest {
    private static final int SKILL_AXES = 44;

    @Test
    void applyWeaponWhenPlayerShouldBeCatalogedAndGrantNoSkill() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_WEAPON));
        Player p = new Player();
        p.spells.add(196);
        SpellEngine.SpellInfo axes = new SpellEngine.SpellInfo(
                196, SpellEngine.EFFECT_WEAPON, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, p, axes));
        assertFalse(p.hasSkill(SKILL_AXES));
        assertEquals(1, p.spells.size());
        assertEquals(0, p.auras.size());
    }
}
