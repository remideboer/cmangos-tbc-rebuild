package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-029 — SPELL_EFFECT_SELF_RESURRECT (94). Soulstone 20707. */
class SpellEngineSelfResurrectTest {
    @Test
    void applySelfResurrectWhenDeadCasterShouldRestorePercentHp() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SELF_RESURRECT));
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo ss = new SpellEngine.SpellInfo(
                20707, SpellEngine.EFFECT_SELF_RESURRECT, 0, 0, 0, 20, 20, 0f);
        eng.apply(dead, dead, ss);
        assertEquals(20, dead.health());
        assertFalse(dead.ghost);
        Player ghost = new Player();
        ghost.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        ghost.setHealth(1);
        ghost.setGhost(true);
        eng.apply(ghost, ghost, ss);
        assertEquals(20, ghost.health());
        assertFalse(ghost.ghost);
    }

    @Test
    void applySelfResurrectWhenLivingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(50);
        SpellEngine.SpellInfo ss = new SpellEngine.SpellInfo(
                20707, SpellEngine.EFFECT_SELF_RESURRECT, 0, 0, 0, 20, 20, 0f);
        eng.apply(live, live, ss);
        assertEquals(50, live.health());
    }
}
