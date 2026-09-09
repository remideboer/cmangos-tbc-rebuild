package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.DeathHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice 6 combat P0 wire: AttackStart, aggro, idle throttle, corpse respawn, evade. */
class Slice06P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl06CreatureAttackStartWhenPlayerSwings() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Fighter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.attackSwing(world, c.guid);
        boolean sawCreatureStart = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            assertTrue(payload.length >= 16);
            long attacker = WowClientDouble.u64le(payload, 0);
            long victim = WowClientDouble.u64le(payload, 8);
            if (attacker == c.guid && victim == p.guid) {
                sawCreatureStart = true;
            }
        }
        assertTrue(sawCreatureStart);
    }

    @Test
    void tpSl06HostileWhenPlayerEntersDetectionShouldAttackStart() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Pull", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x + 10, c.y, c.z, c.o);
        client.clear();
        world.tick(50);
        boolean sawCreatureStart = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            long attacker = WowClientDouble.u64le(payload, 0);
            long victim = WowClientDouble.u64le(payload, 8);
            if (attacker == c.guid && victim == p.guid) {
                sawCreatureStart = true;
            }
        }
        assertTrue(sawCreatureStart);
        assertTrue(c.inCombat);
    }

    @Test
    void hostileWhenOocAfterFirstUpdateShouldNotPullOnFiftyMsTick() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Wait", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x + 50, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        world.tick(50);
        assertFalse(c.inCombat);
        ox = p.x;
        oy = p.y;
        p.relocate(c.x + 10, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        world.tick(50);
        assertFalse(c.inCombat);
        world.tick(500);
        assertTrue(c.inCombat);
        boolean sawCreatureStart = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            long attacker = WowClientDouble.u64le(payload, 0);
            long victim = WowClientDouble.u64le(payload, 8);
            if (attacker == c.guid && victim == p.guid) {
                sawCreatureStart = true;
            }
        }
        assertTrue(sawCreatureStart);
    }

    @Test
    void tpSl06RespawnWhenDelayElapsedShouldSendHealthValues() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Respawn", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        c.respawnDelayMs = 1;
        client.attackSwing(world, c.guid);
        int n = 0;
        while (c.alive() && n++ < 80) {
            world.meleeHit(p, c);
        }
        assertFalse(c.alive());
        client.clear();
        world.tick(50);
        assertTrue(c.alive());
        assertEquals(c.maxHealth(), c.health());
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_OBJECT) || client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void tpSl06EvadeWhenPastLeashShouldSendAttackStopAndHealth() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Evade", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        assertTrue(c.inCombat);
        ox = p.x;
        oy = p.y;
        p.relocate(c.spawnX + 35, c.spawnY, c.spawnZ, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        world.tick(50);
        assertFalse(c.inCombat);
        assertEquals(c.maxHealth(), c.health());
        boolean sawStop = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTOP) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            int off = 0;
            long attacker = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            long victim = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            int nowDead = WowClientDouble.u32le(payload, off);
            if (attacker == c.guid && victim == p.guid && nowDead == 0) {
                sawStop = true;
            }
        }
        assertTrue(sawStop);
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_OBJECT) || client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    /**
     * TP-SL06-010 — Unit::Kill → SetDeathState(JUST_DIED) → Player::KillPlayer: root, release-timer
     * byte on a continent, 6-minute death timer that auto-repops (death.md "Kill (server)").
     */
    @Test
    void tpSl06CreatureDamageKillsPlayer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Victim", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        client.clear();
        int n = 0;
        while (p.alive() && n++ < 400) {
            world.creatureMeleeHit(c, p);
        }
        assertFalse(p.alive());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        // Player::KillPlayer: SendMoveRoot(true) + PLAYER_FIELD_BYTE_RELEASE_TIMER (0x08, byte 0) on a non-instance map.
        assertTrue(client.saw(Opcodes.SMSG_FORCE_MOVE_ROOT));
        assertEquals(RELEASE_TIMER, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_BYTES) & RELEASE_TIMER);
        // Killer AttackStop: nowDead is the attacker's IsDead() → 0 (Unit::SendMeleeAttackStop).
        assertTrue(sawAttackStop(client, c.guid, p.guid, 0));
        assertFalse(p.ghost);
        // m_deathTimer 6 × MINUTE → BuildPlayerRepop + RepopAtGraveyard without CMSG_REPOP_REQUEST.
        client.clear();
        world.advanceMs(DeathHandler.DEATH_TIMER_MS);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.SMSG_DEATH_RELEASE_LOC));
        assertTrue(client.saw(Opcodes.SMSG_FORCE_MOVE_UNROOT));
    }

    /**
     * TP-SL06-011 — RewardSinglePlayerAtKill → GiveXP(MaNGOS::XP::Gain) → SendLogXPGain.
     * Level 1 vs level-1 Kobold Vermin on map 0: BaseGain = 1*5 + 45 = 50.
     */
    @Test
    void tpSl06KillGrantsXp() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Grinder", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = killKobold(world, client, p);
        byte[] log = client.payload(Opcodes.SMSG_LOG_XPGAIN);
        assertEquals(22, log.length);
        assertEquals(c.guid, WowClientDouble.u64le(log, 0));
        assertEquals(50, WowClientDouble.u32le(log, 8));
        assertEquals(0, log[12]);
        assertEquals(50, WowClientDouble.u32le(log, 13));
        assertEquals(1.0f, WowClientDouble.floatle(log, 17));
        assertEquals(0, log[21]);
        assertEquals(50, client.valuesField(p.guid, UpdateFields.PLAYER_XP));
    }

    /**
     * TP-SL06-012 — GiveXP past PLAYER_NEXT_LEVEL_XP → GiveLevel(2): SMSG_LEVELUP_INFO deltas from
     * player_classlevelstats / player_levelstats (human warrior 20→29 hp, str/agi/sta +1), fields re-derived.
     */
    @Test
    void tpSl06LevelUp() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Dinger", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        created.xp = 350;
        world.characters.save(created);
        client.login(world, created.guid);
        Player p = client.session().player();
        killKobold(world, client, p);
        byte[] info = client.payload(Opcodes.SMSG_LEVELUP_INFO);
        assertEquals(4 + 4 + 5 * 4 + 5 * 4, info.length);
        assertEquals(2, WowClientDouble.u32le(info, 0));
        assertEquals(9, WowClientDouble.u32le(info, 4));
        for (int i = 0; i < 5; i++) {
            assertEquals(0, WowClientDouble.u32le(info, 8 + i * 4));
        }
        assertEquals(1, WowClientDouble.u32le(info, 28));
        assertEquals(1, WowClientDouble.u32le(info, 32));
        assertEquals(1, WowClientDouble.u32le(info, 36));
        assertEquals(0, WowClientDouble.u32le(info, 40));
        assertEquals(0, WowClientDouble.u32le(info, 44));
        assertEquals(2, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_LEVEL));
        assertEquals(900, client.valuesField(p.guid, UpdateFields.PLAYER_NEXT_LEVEL_XP));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_XP));
        assertEquals(24, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT0));
        // basehp 29 + GetHealthBonusFromStamina(23) = 29 + 20 + 3 * 10 → 79, full after GiveLevel.
        assertEquals(79, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXHEALTH));
        assertEquals(79, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(0, p.getInt(UpdateFields.PLAYER_CHARACTER_POINTS1));
    }

    /** TP-SL06-013 — Creature::LoadFromDB m_respawnDelay = spawntimesecs (min=max 120), not a fixed 300 s. */
    @Test
    void tpSl06RespawnUsesSpawntimesecs() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Camper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        ObjectMgr.Spawn row = new ObjectMgr.Spawn(9001, 6, 0, p.x, p.y, p.z, p.o, 0f, 0, 120, 120);
        Creature c = world.objectMgr.spawnCreature(row, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        client.attackSwing(world, c.guid);
        int n = 0;
        while (c.alive() && n++ < 400) {
            world.meleeHit(p, c);
        }
        assertFalse(c.alive());
        world.advanceMs(119_000);
        world.tick(50);
        assertFalse(c.alive());
        client.clear();
        world.advanceMs(1_000);
        world.tick(50);
        assertTrue(c.alive());
        assertEquals(c.maxHealth(), client.valuesField(c.guid, UpdateFields.UNIT_FIELD_HEALTH));
    }

    private static Creature killKobold(World world, WowClientDouble client, Player p) {
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        client.clear();
        int n = 0;
        while (c.alive() && n++ < 400) {
            world.meleeHit(p, c);
        }
        assertFalse(c.alive());
        return c;
    }

    private static final int RELEASE_TIMER = 0x08;

    private static boolean sawAttackStop(WowClientDouble client, long attacker, long victim, int nowDead) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTOP) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            int off = 0;
            long a = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            long v = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            if (a == attacker && v == victim && WowClientDouble.u32le(payload, off) == nowDead) {
                return true;
            }
        }
        return false;
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
