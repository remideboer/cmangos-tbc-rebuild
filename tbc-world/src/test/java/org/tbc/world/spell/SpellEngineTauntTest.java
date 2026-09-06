package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-017 — SPELL_EFFECT_ATTACK_ME (114). Warrior Taunt 355. */
class SpellEngineTauntTest {
    @Test
    void applyAttackMeWhenCreatureHasThreatListShouldEqualizeAndForceVictim() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ATTACK_ME));
        Player tank = new Player();
        tank.guid = 1;
        Player warrior = new Player();
        warrior.guid = 2;
        Creature mob = new Creature();
        mob.guid = 10;
        mob.threatManager.add(tank, 50f);
        mob.threatManager.add(warrior, 10f);
        mob.victim = tank.guid;
        SpellEngine.SpellInfo taunt = new SpellEngine.SpellInfo(355, SpellEngine.EFFECT_ATTACK_ME, 0, 0, 0, 0, 0, 0f);
        eng.apply(warrior, mob, taunt);
        assertEquals(50f, mob.threatManager.threatOf(warrior), 0.01f);
        assertEquals(warrior.guid, mob.victim);
    }

    @Test
    void applyAttackMeWhenAlreadyAttackingCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player warrior = new Player();
        warrior.guid = 2;
        Creature mob = new Creature();
        mob.guid = 10;
        mob.threatManager.add(warrior, 10f);
        mob.victim = warrior.guid;
        SpellEngine.SpellInfo taunt = new SpellEngine.SpellInfo(355, SpellEngine.EFFECT_ATTACK_ME, 0, 0, 0, 0, 0, 0f);
        eng.apply(warrior, mob, taunt);
        assertEquals(10f, mob.threatManager.threatOf(warrior), 0.01f);
        assertEquals(warrior.guid, mob.victim);
        Player p = new Player();
        p.guid = 3;
        p.victim = 99;
        eng.apply(warrior, p, taunt);
        assertEquals(99, p.victim);
    }
}
