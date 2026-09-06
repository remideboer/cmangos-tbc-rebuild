package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-061 — SPELL_EFFECT_STUCK (84). Stuck 7355 continent nudge 10 yd along facing. */
class SpellEngineStuckTest {
    @Test
    void applyStuckWhenAliveShouldNudgeTenYardsAlongFacing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_STUCK));
        Player p = new Player();
        p.relocate(0f, 0f, 5f, 0f);
        SpellEngine.SpellInfo stuck = new SpellEngine.SpellInfo(7355, SpellEngine.EFFECT_STUCK, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, stuck);
        assertEquals(10f, p.x, 0.01f);
        assertEquals(0f, p.y, 0.01f);
        assertEquals(5f, p.z, 0.01f);
        assertEquals(0f, p.o, 0.01f);
    }

    @Test
    void applyStuckWhenTaxiOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.relocate(0f, 0f, 5f, 0f);
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_TAXI_FLIGHT);
        SpellEngine.SpellInfo stuck = new SpellEngine.SpellInfo(7355, SpellEngine.EFFECT_STUCK, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, stuck);
        assertEquals(0f, p.x, 0.01f);
        Creature npc = new Creature();
        npc.relocate(1f, 2f, 3f, 0f);
        eng.apply(npc, npc, stuck);
        assertEquals(1f, npc.x, 0.01f);
    }
}
