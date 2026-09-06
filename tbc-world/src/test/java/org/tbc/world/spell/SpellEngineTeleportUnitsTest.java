package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-060 — SPELL_EFFECT_TELEPORT_UNITS (5). Hearthstone 8690 to homebind. */
class SpellEngineTeleportUnitsTest {
    @Test
    void applyTeleportUnitsWhenHearthstoneShouldNearTeleportToHomebind() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TELEPORT_UNITS));
        Player p = new Player();
        p.mapId = 530;
        p.relocate(100f, 200f, 10f, 1.5f);
        p.setHomebindToLocation(0, 12, -9115.27f, 423.261f, 92.5f);
        SpellEngine.SpellInfo hs = new SpellEngine.SpellInfo(8690, SpellEngine.EFFECT_TELEPORT_UNITS, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, hs);
        assertEquals(0, p.mapId);
        assertEquals(-9115.27f, p.x, 0.01f);
        assertEquals(423.261f, p.y, 0.01f);
        assertEquals(92.5f, p.z, 0.01f);
        assertEquals(1.5f, p.o, 0.01f);
    }

    @Test
    void applyTeleportUnitsWhenTaxiFlyingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.mapId = 0;
        p.relocate(100f, 200f, 10f, 0f);
        p.setHomebindToLocation(0, 12, -9115.27f, 423.261f, 92.5f);
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_TAXI_FLIGHT);
        SpellEngine.SpellInfo hs = new SpellEngine.SpellInfo(8690, SpellEngine.EFFECT_TELEPORT_UNITS, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, hs);
        assertEquals(100f, p.x, 0.01f);
        assertEquals(200f, p.y, 0.01f);
        Creature npc = new Creature();
        npc.relocate(1f, 2f, 3f, 0f);
        eng.apply(p, npc, hs);
        assertEquals(1f, npc.x, 0.01f);
        SpellEngine.SpellInfo other = new SpellEngine.SpellInfo(
                3561, SpellEngine.EFFECT_TELEPORT_UNITS, 0, 0, 0, 0, 0, 0f);
        Player stay = new Player();
        stay.relocate(7f, 8f, 9f, 0f);
        stay.setHomebindToLocation(0, 12, -9115.27f, 423.261f, 92.5f);
        eng.apply(stay, stay, other);
        assertEquals(7f, stay.x, 0.01f);
        assertEquals(8f, stay.y, 0.01f);
    }
}
