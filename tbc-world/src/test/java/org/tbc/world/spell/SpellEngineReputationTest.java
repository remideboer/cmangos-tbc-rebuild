package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-035 — SPELL_EFFECT_REPUTATION (103). Stormpike Reputation +5 21187, faction 730. */
class SpellEngineReputationTest {
    static final int FACTION_STORMPIKE = 730;

    @Test
    void applyReputationWhenPlayerShouldAddStandingForFaction() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_REPUTATION));
        Player p = new Player();
        SpellEngine.SpellInfo plus5 = new SpellEngine.SpellInfo(
                21187, SpellEngine.EFFECT_REPUTATION, 0, 0, 0, 5, 5, 0f, FACTION_STORMPIKE);
        eng.apply(p, p, plus5);
        assertEquals(5, p.reputationStanding(FACTION_STORMPIKE));
        eng.apply(p, p, plus5);
        assertEquals(10, p.reputationStanding(FACTION_STORMPIKE));
    }

    @Test
    void applyReputationWhenNonPlayerOrZeroFactionShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature c = new Creature();
        SpellEngine.SpellInfo plus5 = new SpellEngine.SpellInfo(
                21187, SpellEngine.EFFECT_REPUTATION, 0, 0, 0, 5, 5, 0f, FACTION_STORMPIKE);
        eng.apply(new Player(), c, plus5);
        Player p = new Player();
        SpellEngine.SpellInfo zeroFac = new SpellEngine.SpellInfo(
                21187, SpellEngine.EFFECT_REPUTATION, 0, 0, 0, 5, 5, 0f, 0);
        eng.apply(p, p, zeroFac);
        assertEquals(0, p.reputationStanding(FACTION_STORMPIKE));
        assertEquals(0, p.reputationStanding(0));
    }
}
