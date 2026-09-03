package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.pvp.Honor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineHonorTest {
    @Test
    void addHonorWhenPlayerAndPositiveShouldIncreasePoints() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.addHonor(p, 19);
        assertEquals(19, p.honorPoints);
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ADD_HONOR));
    }

    @Test
    void addHonorWhenNullCreatureOrNonPositiveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.addHonor(null, 10);
        Creature c = new Creature();
        eng.addHonor(c, 10);
        Player p = new Player();
        p.honorPoints = 7;
        eng.addHonor(p, 0);
        eng.addHonor(p, -1);
        assertEquals(7, p.honorPoints);
    }

    @Test
    void applyAddHonorEffectShouldRewardAndClamp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.honorPoints = Honor.MAX_HONOR_POINTS - 5;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(9998, SpellEngine.EFFECT_ADD_HONOR, 0, 0, 0, 20, 20, 0f);
        eng.apply(p, p, sp);
        assertEquals(Honor.MAX_HONOR_POINTS, p.honorPoints);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(9998, SpellEngine.EFFECT_ADD_HONOR, 0, 0, 0, 0, 0, 0f);
        int before = p.honorPoints;
        eng.apply(p, p, zero);
        assertEquals(before, p.honorPoints);
        Creature c = new Creature();
        assertEquals(0, eng.apply(p, c, sp));
    }
}
