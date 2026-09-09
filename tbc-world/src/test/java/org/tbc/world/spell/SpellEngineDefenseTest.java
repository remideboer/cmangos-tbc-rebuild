package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-111 — SPELL_EFFECT_DEFENSE (26). Defense 204 (one spell).
 * CMaNGOS EffectEmpty: the Defense skill line owns the value. Cataloged, mutates nothing.
 */
class SpellEngineDefenseTest {
    private static final int SKILL_DEFENSE = 95;

    @Test
    void applyDefenseWhenPlayerShouldBeCatalogedAndGrantNoSkill() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DEFENSE));
        Player p = new Player();
        p.spells.add(204);
        SpellEngine.SpellInfo defense = new SpellEngine.SpellInfo(
                204, SpellEngine.EFFECT_DEFENSE, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, p, defense));
        assertFalse(p.hasSkill(SKILL_DEFENSE));
        assertEquals(1, p.spells.size());
        assertEquals(0, p.auras.size());
    }
}
