package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-013 — SPELL_EFFECT_INTERRUPT_CAST (68). */
class SpellEngineInterruptCastTest {
    @Test
    void applyInterruptCastWhenChannelingPlayerShouldStopChannel() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_INTERRUPT_CAST));
        Player rogue = new Player();
        Player victim = new Player();
        victim.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        victim.setHealth(100);
        victim.channeling = true;
        SpellEngine.SpellInfo kick = new SpellEngine.SpellInfo(1766, SpellEngine.EFFECT_INTERRUPT_CAST, 0, 0, 0, 0, 0, 5f);
        eng.apply(rogue, victim, kick);
        assertFalse(victim.channeling);
    }

    @Test
    void applyInterruptCastWhenDeadOrNotChannelingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player rogue = new Player();
        Player dead = new Player();
        dead.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        dead.channeling = true;
        SpellEngine.SpellInfo kick = new SpellEngine.SpellInfo(1766, SpellEngine.EFFECT_INTERRUPT_CAST, 0, 0, 0, 0, 0, 5f);
        eng.apply(rogue, dead, kick);
        assertTrue(dead.channeling);
        Creature npc = new Creature();
        npc.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        npc.setHealth(50);
        eng.apply(rogue, npc, kick);
        Player idle = new Player();
        idle.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        idle.setHealth(100);
        idle.channeling = false;
        eng.apply(rogue, idle, kick);
        assertFalse(idle.channeling);
        eng.apply(rogue, null, kick);
    }
}
