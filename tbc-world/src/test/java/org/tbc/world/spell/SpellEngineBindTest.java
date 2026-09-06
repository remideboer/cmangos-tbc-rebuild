package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-016 — SPELL_EFFECT_BIND (11). */
class SpellEngineBindTest {
    @Test
    void applyBindWhenPlayerShouldSetHomebindToCurrentLoc() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_BIND));
        Player p = new Player();
        p.mapId = 0;
        p.zoneId = 12;
        p.relocate(-9465f, 16f, 57f, 0f);
        SpellEngine.SpellInfo bind = new SpellEngine.SpellInfo(3286, SpellEngine.EFFECT_BIND, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, bind);
        assertEquals(-9465f, p.bindX, 0.01f);
        assertEquals(16f, p.bindY, 0.01f);
        assertEquals(57f, p.bindZ, 0.01f);
        assertEquals(0, p.bindMap);
        assertEquals(12, p.bindZone);
    }

    @Test
    void applyBindWhenNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature c = new Creature();
        SpellEngine.SpellInfo bind = new SpellEngine.SpellInfo(3286, SpellEngine.EFFECT_BIND, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(new Player(), c, bind));
        Player p = new Player();
        p.bindX = 1f;
        assertEquals(0, eng.apply(p, null, bind));
        assertEquals(1f, p.bindX, 0.01f);
    }
}
