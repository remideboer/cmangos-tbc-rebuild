package org.tbc;

import org.tbc.common.WowBuffer;
import org.tbc.bdd.WowClientDouble;
import org.tbc.world.ai.EventAi;
import org.tbc.world.ai.FactorySelector;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** EventAI catalog deepen: TIMER_IN_COMBAT / TIMER_OOC over World.tick. Keep TP-SL11-001 Gherkin. */
class Slice11EventAiTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void timerInCombatWhenWindowElapsedShouldSendSpellGo() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Timer");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(EventAi.Script.timerInCombat(0, 1000, 7164, EventAi.TARGET_SELF)));
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.attackSwing(world, c.guid);
        client.clear();
        world.tick(501);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    @Test
    void timerOocWhenWorldTickShouldSendSpellGo() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ooc");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(EventAi.Script.timerOoc(0, 1000, 7164, EventAi.TARGET_SELF)));
        world.map(p.mapId, p.instanceId).add(c);
        client.clear();
        world.tick(501);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    @Test
    void spellHitWhenFireballShouldSendEventAiSpellGo() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Hit");
        Player p = client.session().player();
        p.spells.add(SpellEngine.FIREBALL);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        p.setPower(100);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_SPELLHIT, 0, 100, EventAi.EFLAG_REPEATABLE,
                SpellEngine.FIREBALL, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.castSpell(world, SpellEngine.FIREBALL, 1, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    @Test
    void meleeWhenEventAiInRangeAfterAttackTimeShouldSendCreatureAttackerState() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Melee");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.attackSwing(world, c.guid);
        client.clear();
        world.tick(2000);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        byte[] pkt = client.payload(Opcodes.SMSG_ATTACKERSTATEUPDATE);
        assertEquals(c.guid, packedGuid(pkt, 4));
    }

    @Test
    void meleeWhenNullAiShouldNotSendCreatureAttackerState() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "NullMelee");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.aiName = "NotARealAI";
        c.eventAi = null;
        FactorySelector.selectAI(c, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.attackSwing(world, c.guid);
        client.clear();
        world.tick(2000);
        assertFalse(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
    }

    @Test
    void meleeWhenEventAiOutOfRangeShouldChaseThenSendCreatureAttackerState() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Chase");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x + 20, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        client.attackSwing(world, c.guid);
        client.clear();
        world.tick(1000);
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
        WowBuffer move = new WowBuffer(client.payload(Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(c.guid, move.getPackedGuid());
        assertTrue(c.distance2d(p) > WorldSession.MELEE_RANGE);
        world.tick(2000);
        assertTrue(c.distance2d(p) <= WorldSession.MELEE_RANGE + 0.01f);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertEquals(c.guid, packedGuid(client.payload(Opcodes.SMSG_ATTACKERSTATEUPDATE), 4));
    }

    @Test
    void oocLosWhenPlayerEntersRangeShouldSendSpellGo() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Los");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x + 15, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_OOC_LOS, 0, 100, 0, 0, 10, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        world.map(p.mapId, p.instanceId).add(c);
        client.clear();
        world.tick(50);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO));
        p.relocate(c.x + 5, c.y, c.z, c.o);
        world.tick(50);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static int spellId(byte[] p) {
        int off = WowClientDouble.skipPackedGuid(p, 0);
        off = WowClientDouble.skipPackedGuid(p, off);
        return WowClientDouble.u32le(p, off);
    }

    private static long packedGuid(byte[] p, int off) {
        int mask = p[off++] & 0xFF;
        long g = 0;
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                g |= (long) (p[off++] & 0xFF) << (8 * i);
            }
        }
        return g;
    }
}
