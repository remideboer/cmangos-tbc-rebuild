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
    private static final World.Account ACC_FAR =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

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
        p.relocate(c.x + 40, c.y, c.z, c.o);
        client.clear();
        world.tick(501);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    @Test
    void timerOocWhenFarPlayerOnSameMapShouldNotReceiveSpellGo() {
        World world = World.inMemory();
        WowClientDouble near = login(world, ACC, "Near");
        WowClientDouble far = login(world, ACC_FAR, "Far");
        Player pn = near.session().player();
        Player pf = far.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, pn.x, pn.y, pn.z, pn.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(EventAi.Script.timerOoc(0, 1000, 7164, EventAi.TARGET_SELF)));
        world.map(pn.mapId, pn.instanceId).add(c);
        pn.relocate(c.x + 40, c.y, c.z, c.o);
        pf.relocate(c.x + 500, c.y, c.z, c.o);
        near.clear();
        far.clear();
        world.tick(501);
        assertTrue(near.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(near.payload(Opcodes.SMSG_SPELL_GO)));
        assertFalse(far.saw(Opcodes.SMSG_SPELL_GO));
    }

    @Test
    void spellHitWhenFireballShouldSendEventAiSpellGo() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Hit");
        Player p = client.session().player();
        p.spells.add(SpellEngine.FIREBALL);
        // Fireball costs mana (POWER1); login helper creates a warrior whose setPower writes POWER2.
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER1, 100);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_SPELLHIT, 0, 100, EventAi.EFLAG_REPEATABLE,
                SpellEngine.FIREBALL, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.castSpell(world, SpellEngine.FIREBALL, 1, c.guid);
        // Fireball rank 1 lands after its 1500 ms cast bar (Spell::update), then EVENT_SPELLHIT fires.
        world.tick(1500);
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

    /**
     * TP-SL11-003 — Mana Wyrm EventAI Faerie Fire 25602 writes UNIT_FIELD_AURA on the victim.
     */
    @Test
    void tpSl11WyrmFaerieFireWhenCombatTimerShouldPutAuraOnVictim() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Wyrmff");
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(15274, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(EventAi.Script.timerInCombat(0, 1000, 25602, EventAi.TARGET_HOSTILE)));
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.attackSwing(world, c.guid);
        client.clear();
        world.tick(501);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(25602, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
        int slot = org.tbc.world.spell.AuraSlots.slotOf(p, 25602);
        assertTrue(slot >= 0, "visible Faerie Fire slot");
        assertEquals(25602, client.valuesField(p.guid, org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_AURA + slot));
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
        // Away from abbey faction NPCs — they DetectOrAttack hostiles in range.
        relocateFar(world, p);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x + 20, p.y, p.z, p.o, world.scripts);
        c.detectionRange = 25f;
        world.map(p.mapId, p.instanceId).add(c);
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
        relocateFar(world, p);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x + 25, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_OOC_LOS, 0, 100, 0, 0, 10, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        c.extraFlags = org.tbc.world.entity.Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT;
        world.map(p.mapId, p.instanceId).add(c);
        client.clear();
        world.tick(50);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO));
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x + 5, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        world.tick(Creature.IDLE_UPDATE_MS);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    private static void relocateFar(World world, Player p) {
        float ox = p.x;
        float oy = p.y;
        p.relocate(20_000f, 20_000f, 80f, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
    }

    @Test
    void aggroWhenEventAiFireballShouldApplySpellEngineDamage() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "FbCast");
        Player p = client.session().player();
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                EventAi.Action.cast(SpellEngine.FIREBALL, EventAi.TARGET_HOSTILE),
                EventAi.Action.none(), EventAi.Action.none())));
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.attackSwing(world, c.guid);
        assertTrue(p.health() < 100);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(SpellEngine.FIREBALL, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
        assertTrue(client.saw(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
    }

    private static WowClientDouble login(World world, String name) {
        return login(world, ACC, name);
    }

    private static WowClientDouble login(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
