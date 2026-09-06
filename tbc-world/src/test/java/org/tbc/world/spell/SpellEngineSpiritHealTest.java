package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-057 — SPELL_EFFECT_SPIRIT_HEAL (117). Spirit Heal 22012 needs aura 2584. */
class SpellEngineSpiritHealTest {
    @Test
    void applySpiritHealWhenDeadWithWaitingAuraShouldResurrectFullHp() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SPIRIT_HEAL));
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        dead.auras.add(new Unit.Aura(2584, 0, 1));
        SpellEngine.SpellInfo heal = new SpellEngine.SpellInfo(
                22012, SpellEngine.EFFECT_SPIRIT_HEAL, 0, 0, 0, 99, 99, 0f);
        eng.apply(new Player(), dead, heal);
        assertEquals(100, dead.health());
        assertFalse(dead.ghost);
        Player ghost = new Player();
        ghost.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        ghost.setHealth(1);
        ghost.setGhost(true);
        ghost.auras.add(new Unit.Aura(2584, 0, 1));
        eng.apply(new Player(), ghost, heal);
        assertEquals(100, ghost.health());
        assertFalse(ghost.ghost);
    }

    @Test
    void applySpiritHealWhenMissingAuraOrAliveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo heal = new SpellEngine.SpellInfo(
                22012, SpellEngine.EFFECT_SPIRIT_HEAL, 0, 0, 0, 99, 99, 0f);
        eng.apply(new Player(), dead, heal);
        assertEquals(0, dead.health());
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(50);
        live.auras.add(new Unit.Aura(2584, 0, 1));
        eng.apply(new Player(), live, heal);
        assertEquals(50, live.health());
    }
}
