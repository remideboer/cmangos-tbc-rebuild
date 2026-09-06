package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-056 — SPELL_EFFECT_RESURRECT_NEW (113). Priest Resurrection 2006 Rank 1. */
class SpellEngineResurrectNewTest {
    @Test
    void applyResurrectNewWhenDeadPlayerShouldStoreAbsoluteHp() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_RESURRECT_NEW));
        Player caster = new Player();
        caster.guid = 10;
        caster.mapId = 0;
        caster.relocate(-9465f, 16f, 57f, 0f);
        Player dead = new Player();
        dead.guid = 20;
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo res = new SpellEngine.SpellInfo(
                2006, SpellEngine.EFFECT_RESURRECT_NEW, 0, 0, 0, 69, 69, 0f, 0);
        eng.apply(caster, dead, res);
        assertEquals(caster.guid, dead.resurrectGuid);
        assertEquals(0, dead.resurrectMap);
        assertEquals(-9465f, dead.resurrectX, 0.01f);
        assertEquals(69, dead.resurrectHealth);
        assertEquals(0, dead.resurrectMana);
        Player other = new Player();
        other.guid = 11;
        eng.apply(other, dead, res);
        assertEquals(caster.guid, dead.resurrectGuid);
    }

    @Test
    void applyResurrectNewWhenAliveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.guid = 10;
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(50);
        SpellEngine.SpellInfo res = new SpellEngine.SpellInfo(
                2006, SpellEngine.EFFECT_RESURRECT_NEW, 0, 0, 0, 69, 69, 0f, 0);
        eng.apply(caster, live, res);
        assertEquals(0, live.resurrectGuid);
        Player ghost = new Player();
        ghost.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        ghost.setHealth(1);
        ghost.setGhost(true);
        eng.apply(caster, ghost, res);
        assertEquals(caster.guid, ghost.resurrectGuid);
        assertEquals(69, ghost.resurrectHealth);
    }
}
