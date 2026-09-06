package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-031 — SPELL_EFFECT_DESTROY_ALL_TOTEMS (110). Totemic Call 36936. */
class SpellEngineDestroyAllTotemsTest {
    @Test
    void applyDestroyAllTotemsWhenPlayerCasterShouldClearTotemSlots() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DESTROY_ALL_TOTEMS));
        Player shaman = new Player();
        shaman.totems[0] = 11;
        shaman.totems[1] = 22;
        shaman.totems[2] = 33;
        shaman.totems[3] = 44;
        SpellEngine.SpellInfo recall = new SpellEngine.SpellInfo(
                36936, SpellEngine.EFFECT_DESTROY_ALL_TOTEMS, 0, 0, 0, 0, 0, 0f);
        eng.apply(shaman, shaman, recall);
        assertEquals(0, shaman.totems[0]);
        assertEquals(0, shaman.totems[1]);
        assertEquals(0, shaman.totems[2]);
        assertEquals(0, shaman.totems[3]);
    }

    @Test
    void applyDestroyAllTotemsWhenNonPlayerCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player shaman = new Player();
        shaman.totems[0] = 11;
        Creature c = new Creature();
        SpellEngine.SpellInfo recall = new SpellEngine.SpellInfo(
                36936, SpellEngine.EFFECT_DESTROY_ALL_TOTEMS, 0, 0, 0, 0, 0, 0f);
        eng.apply(c, shaman, recall);
        assertEquals(11, shaman.totems[0]);
    }
}
