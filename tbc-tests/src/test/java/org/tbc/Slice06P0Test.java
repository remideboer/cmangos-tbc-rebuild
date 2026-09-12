package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
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

    /**
     * TP-SL06-018 — CMSG_ATTACKSWING out of melee and out of GetAttackDistance must not
     * AttackStart the creature (Unit::Attack only starts the player; DetectOrAttack is range-gated).
     */
    @Test
    void tpSl06OutOfRangeSwingShouldNotStartCreatureAttack() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "FarSwing", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x + 40f, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        client.attackSwing(world, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSWING_NOTINRANGE));
        assertTrue(sawAttackStart(client, p.guid, c.guid));
        assertFalse(sawAttackStart(client, c.guid, p.guid));
        assertFalse(c.inCombat);
        assertEquals(0, c.victim);
    }

    /**
     * TP-SL06-018 — yellow/neutral (CanAttack, not CanAttackOnSight) must not aggro from a
     * click outside melee; MoveInLineOfSight ignores neutrals.
     */
    @Test
    void tpSl06NeutralOutOfMeleeSwingShouldNotAggro() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "WolfClick", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        setFaction(p, 115);
        setFaction(c, 32);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x + 10f, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        client.attackSwing(world, c.guid);
        assertFalse(sawAttackStart(client, c.guid, p.guid));
        assertFalse(c.inCombat);
        world.tick(500);
        assertFalse(c.inCombat);
        assertFalse(sawAttackStart(client, c.guid, p.guid));
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
        c.lastHitMs = world.nowMs() - org.tbc.world.combat.Combat.PURSUIT_MS - 1;
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
     * TP-SL06-020 — CombatManager player path: empty hostile refs → HandleExitCombat → CombatStop.
     * After creature evade the player drops UNIT_FLAG_IN_COMBAT (not stuck until CMSG_ATTACKSTOP).
     */
    @Test
    void tpSl06PlayerLeavesCombatWhenCreatureEvades() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DropFlag", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        // Northshire in-memory cluster includes Garrick (103); kite +35 from create-pos pulls him.
        float ox = p.x;
        float oy = p.y;
        p.relocate(20_000f, 20_000f, 80f, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        client.attackSwing(world, c.guid);
        assertTrue(p.inCombat);
        assertEquals(Unit.UNIT_FLAG_IN_COMBAT, p.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_IN_COMBAT);
        ox = p.x;
        oy = p.y;
        p.relocate(c.spawnX + 35, c.spawnY, c.spawnZ, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        c.lastHitMs = world.nowMs() - org.tbc.world.combat.Combat.PURSUIT_MS - 1;
        client.clear();
        world.tick(50);
        assertFalse(c.inCombat);
        assertFalse(p.inCombat);
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_IN_COMBAT);
        assertEquals(0, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_IN_COMBAT);
        assertTrue(sawAttackStop(client, p.guid, c.guid, 0));
    }

    /**
     * TP-SL06-021 — CMaNGOS ChaseMovementGenerator + UpdateMeleeAttackingState:
     * out of melee the creature chases (SMSG_MONSTER_MOVE) and swingError retries in 100 ms,
     * then hits when the victim re-enters melee (not a full BASEATTACKTIME delay).
     */
    @Test
    void tpSl06ChaseWhenVictimLeavesMeleeShouldRetrySwingIn100Ms() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Kiter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(20_000f, 20_000f, 80f, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        client.attackSwing(world, c.guid);
        assertTrue(c.inCombat);
        c.meleeCooldownMs = 0;
        ox = p.x;
        oy = p.y;
        p.relocate(c.x + 15f, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        world.tick(50);
        assertTrue(c.inCombat);
        assertEquals(org.tbc.world.combat.Combat.SWING_ERROR_RETRY_MS, c.meleeCooldownMs);
        assertFalse(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertTrue(sawMonsterMoveType(client, c.guid, org.tbc.world.session.TaxiHandler.MONSTER_MOVE_FACING_TARGET));
        ox = p.x;
        oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        world.tick(org.tbc.world.combat.Combat.SWING_ERROR_RETRY_MS);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
    }

    /**
     * TP-SL06-022 — outdoor combat-start leash (CMaNGOS 90 yd drop): a chase that keeps
     * landing hits still EnterEvadeMode when the creature is past combat start.
     */
    @Test
    void tpSl06EvadeWhenCreaturePastCombatStartLeashShouldSendAttackStop() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Leash", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(20_000f, 20_000f, 80f, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        client.attackSwing(world, c.guid);
        assertTrue(c.inCombat);
        float destX = c.combatStartX + org.tbc.world.combat.Combat.COMBAT_START_LEASH + 1f;
        float destY = c.combatStartY;
        ox = p.x;
        oy = p.y;
        p.relocate(destX, destY, p.z, p.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        ox = c.x;
        oy = c.y;
        c.relocate(destX, destY, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(c, ox, oy);
        client.clear();
        world.tick(50);
        assertFalse(c.inCombat);
        assertTrue(c.evading);
        assertTrue(sawAttackStop(client, c.guid, p.guid, 0));
        world.tick(50);
        assertFalse(c.inCombat);
        assertTrue(c.evading);
    }

    /**
     * TP-SL06-023 — CMaNGOS UpdateMeleeAttackingState HasInArc(2π/3): back to the target
     * is SWING_ERROR_BAD_FACING (SMSG_ATTACKSWING_BADFACING), no SMSG_ATTACKERSTATEUPDATE.
     */
    @Test
    void tpSl06SwingWhenNotFacingShouldSendBadFacing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Turner", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(20_000f, 20_000f, 80f, (float) Math.PI);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x + 2f, p.y, p.z, 0, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        client.clear();
        client.attackSwing(world, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSWING_BADFACING));
        assertFalse(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        ox = p.x;
        oy = p.y;
        p.relocate(p.x, p.y, p.z, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        client.attackSwing(world, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        ox = p.x;
        oy = p.y;
        p.relocate(p.x, p.y, p.z, (float) Math.PI);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        p.lastMeleeMs = 0;
        client.clear();
        client.session().tick(world, 50);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSWING_BADFACING));
        assertFalse(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
    }

    /**
     * TP-SL06-019 — SelectHostileTarget unreachable (Z above CREATURE_Z_ATTACK_RANGE_MELEE) starts
     * CombatManager's 10 s evade timer (hits EVADES); expiry EnterEvadeMode / SendMeleeAttackStop.
     */
    @Test
    void tpSl06UnreachableShouldEvadeHitsThenResetAfterTenSeconds() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Cliff", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
        p.relocate(c.x, c.y, c.z + 20f, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        world.tick(50);
        assertTrue(c.inCombat);
        client.clear();
        world.meleeHit(p, c);
        boolean sawEvade = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKERSTATEUPDATE) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            int hitInfo = WowClientDouble.u32le(payload, 0);
            int off = WowClientDouble.skipPackedGuid(payload, 4);
            off = WowClientDouble.skipPackedGuid(payload, off);
            off += 4 + 1 + 4 + 4 + 4 + 4 + 4;
            int victimState = WowClientDouble.u32le(payload, off);
            if ((hitInfo & org.tbc.world.combat.Combat.HITINFO_MISS) != 0
                    && (hitInfo & org.tbc.world.combat.Combat.HITINFO_SWINGNOHITSOUND) != 0
                    && victimState == org.tbc.world.combat.Combat.VICTIM_EVADES) {
                sawEvade = true;
            }
        }
        assertTrue(sawEvade);
        assertTrue(c.inCombat);
        client.clear();
        world.tick(org.tbc.world.combat.Combat.EVADE_TIMER_MS);
        assertFalse(c.inCombat);
        assertEquals(c.maxHealth(), c.health());
        assertTrue(sawAttackStop(client, c.guid, p.guid, 0));
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

    /** TP-SL06-014 — UNIT_FIELD_HEALTH is a public field: Map::SendObjectUpdates delivers the victim's VALUES to nearby players. */
    @Test
    void tpSl06VictimHealthBroadcastToNearby() {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        a.connect(ACC);
        Player createdA = world.characters.create(ACC.id(), "Tank", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, createdA.guid);
        Player victim = a.session().player();
        WowClientDouble b = new WowClientDouble();
        b.connect(new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86"));
        Player createdB = world.characters.create(2, "Healer", 1, 5, 0, 1, 1, 1, 1, 0, world.objectMgr);
        b.login(world, createdB.guid);
        Creature c = world.objectMgr.spawnCreature(6, 0, victim.x, victim.y, victim.z, victim.o, world.scripts);
        world.map(victim.mapId, victim.instanceId).add(c);
        int hpBefore = victim.health();
        b.clear();
        int n = 0;
        while (victim.health() == hpBefore && n++ < 200) {
            world.creatureMeleeHit(c, victim);
        }
        assertTrue(victim.health() < hpBefore);
        assertEquals(victim.health(), b.valuesField(victim.guid, UpdateFields.UNIT_FIELD_HEALTH));
    }

    /**
     * TP-SL06-015 — Unit::Kill: the corpse's HEALTH 0 goes to everyone in range (SendMessageToSet); the
     * UNIT_DYNAMIC_FLAGS block is built per viewer (Object::BuildValuesUpdate), LOOTABLE only for the tapper.
     */
    @Test
    void tpSl06CreatureDeathBroadcastAndLootableFlag() {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        a.connect(ACC);
        Player createdA = world.characters.create(ACC.id(), "Killer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, createdA.guid);
        Player killer = a.session().player();
        WowClientDouble b = new WowClientDouble();
        b.connect(new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86"));
        Player createdB = world.characters.create(2, "Watcher", 1, 5, 0, 1, 1, 1, 1, 0, world.objectMgr);
        b.login(world, createdB.guid);
        b.clear();

        Creature c = killKobold(world, a, killer);

        assertEquals(0, a.valuesField(c.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(0, b.valuesField(c.guid, UpdateFields.UNIT_FIELD_HEALTH), "observer sees the corpse");
        assertEquals(UNIT_DYNFLAG_LOOTABLE,
                a.valuesField(c.guid, UpdateFields.UNIT_DYNAMIC_FLAGS) & UNIT_DYNFLAG_LOOTABLE, "tapper may loot");
        assertEquals(0, b.valuesField(c.guid, UpdateFields.UNIT_DYNAMIC_FLAGS) & UNIT_DYNFLAG_LOOTABLE,
                "observer gets no loot sparkle");
    }

    /**
     * Unit::SetDeathState(JUST_DIED) StopMoving: SMSG_MONSTER_MOVE MonsterMoveStop so the client
     * drops FACING_TARGET (otherwise the corpse keeps turning toward the looter).
     */
    @Test
    void tpSl06DeadCreatureShouldStopFacingOnWire() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Corpse", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        world.tick(50);
        int n = 0;
        while (c.alive() && n++ < 80) {
            world.meleeHit(p, c);
        }
        assertFalse(c.alive());
        assertTrue(sawMonsterMoveType(client, c.guid, org.tbc.world.session.TaxiHandler.MONSTER_MOVE_STOP));
        assertEquals(0, client.valuesField(c.guid, UpdateFields.UNIT_FIELD_TARGET));
        assertEquals(0, client.valuesField(c.guid, UpdateFields.UNIT_FIELD_TARGET + 1));
        client.clear();
        ox = p.x;
        oy = p.y;
        p.relocate(c.x + 4, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        world.tick(200);
        assertFalse(sawMonsterMoveType(client, c.guid, org.tbc.world.session.TaxiHandler.MONSTER_MOVE_FACING_TARGET));
        assertEquals(org.tbc.world.ai.MotionMaster.IDLE, c.motion.type());
    }

    private static final int UNIT_DYNFLAG_LOOTABLE = 0x0001;

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

    private static boolean sawMonsterMoveType(WowClientDouble client, long guid, int moveType) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_MONSTER_MOVE) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            if (packedGuid(payload, 0) != guid) {
                continue;
            }
            int off = WowClientDouble.skipPackedGuid(payload, 0) + 16;
            if (off < payload.length && (payload[off] & 0xFF) == moveType) {
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

    private static boolean sawAttackStart(WowClientDouble client, long attacker, long victim) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            if (payload.length >= 16
                    && WowClientDouble.u64le(payload, 0) == attacker
                    && WowClientDouble.u64le(payload, 8) == victim) {
                return true;
            }
        }
        return false;
    }

    private static void setFaction(Unit u, int templateId) {
        u.faction = templateId;
        u.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, templateId);
    }
}
