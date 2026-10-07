package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.session.DeathHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL17-* from spec/03-protocol/packets/death.md */
class Slice17P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl17RepopGhostAtGraveyard() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ghost");
        Player p = client.session().player();
        float deathX = p.x;
        float deathY = p.y;
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertTrue(p.ghost);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA));
        assertNotNull(p.corpse);
        assertEquals(deathX, p.corpse.x, 0.01);
        assertEquals(deathY, p.corpse.y, 0.01);
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(DeathHandler.GY_ELWYNN_MAP, WowClientDouble.u32le(loc, 0));
        assertEquals(-8935.33f, WowClientDouble.floatle(loc, 4), 0.05);
        assertEquals(-188.646f, WowClientDouble.floatle(loc, 8), 0.05);
        assertEquals(DeathHandler.CORPSE_RECLAIM_DELAY_FIRST_MS,
                WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_CORPSE_RECLAIM_DELAY), 0));
        assertTrue(sawSpellGo(client, PvpObjectives.GHOST_AURA));
        assertEquals(Player.PLAYER_FLAGS_GHOST, p.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST);
        assertTrue(client.saw(Opcodes.SMSG_MOVE_WATER_WALK));
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    /**
     * TP-SL17-018 — BuildPlayerRepop: ghost at the graveyard stands and can walk.
     * KillPlayer roots + HEALTH 0; release must UNROOT, HEALTH 1, and
     * UNIT_BYTE1_FLAG_ALWAYS_STAND (Player.cpp BuildPlayerRepop / ResurrectPlayer).
     */
    @Test
    void tpSl17RepopWhenKilledShouldStandUnrootAndSendHealthOne() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Walker");
        Player p = client.session().player();
        DeathHandler.killPlayer(client.session(), world);
        assertTrue(client.saw(Opcodes.SMSG_FORCE_MOVE_ROOT));
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertTrue(p.ghost);
        assertEquals(1, p.health());
        assertEquals(1, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertTrue(client.saw(Opcodes.SMSG_FORCE_MOVE_UNROOT));
        int bytes1 = client.valuesField(p.guid, UpdateFields.UNIT_FIELD_BYTES_1);
        assertEquals(org.tbc.world.entity.Unit.UNIT_STAND_STATE_STAND, bytes1 & 0xFF);
        assertEquals(org.tbc.world.entity.Unit.UNIT_BYTE1_FLAG_ALWAYS_STAND,
                (bytes1 >>> 24) & org.tbc.world.entity.Unit.UNIT_BYTE1_FLAG_ALWAYS_STAND);
    }

    /**
     * TP-SL17-010 — RepopAtGraveyard uses the closest world_safe_locs linked to the
     * zone (CMaNGOS GetClosestGraveYard area then zone). Goldshire → loc 106, not
     * the map default / Northshire.
     */
    @Test
    void tpSl17RepopWhenGoldshireShouldUseClosestSpiritHealerInZone() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "GoldshireGhost");
        Player p = client.session().player();
        p.relocate(Content.GOLDSHIRE_X, Content.GOLDSHIRE_Y, Content.GOLDSHIRE_Z, 0);
        p.zoneId = 1;
        p.zoneClient = 87;
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(0, WowClientDouble.u32le(loc, 0));
        assertEquals(-9339.46f, WowClientDouble.floatle(loc, 4), 0.05);
        assertEquals(171.408f, WowClientDouble.floatle(loc, 8), 0.05);
        assertTrue(Math.abs(WowClientDouble.floatle(loc, 4) - DeathHandler.GY_ELWYNN_X) > 100);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    /**
     * TP-SL17-019 — Die on Sunstrider Isle (createinfo zone 3431): resolve parent Eversong 3430
     * → world_safe_locs 912, not Horde continent default / void.
     */
    @Test
    void tpSl17RepopWhenSunstriderIsleShouldUseNearestEversongSpiritHealer() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "SinThoren");
        Player p = client.session().player();
        world.map(p.mapId, p.instanceId).remove(p);
        p.mapId = 530;
        p.zoneId = 3431;
        p.zoneClient = 0;
        p.team = org.tbc.world.map.GraveyardManager.HORDE;
        p.relocate(10349.6f, -6357.29f, 33.4026f, 5.31605f);
        world.map(530, 0).add(p);
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(530, WowClientDouble.u32le(loc, 0));
        assertEquals(10458.5f, WowClientDouble.floatle(loc, 4), 0.5f);
        assertEquals(-6364.61f, WowClientDouble.floatle(loc, 8), 0.5f);
        assertEquals(530, p.mapId);
        assertEquals(10458.5f, p.x, 0.5f);
        assertEquals(39.7907f, p.z, 0.5f);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    /**
     * TP-SL17-021 — Blood Elf mage create (class 8) must repop at Sunstrider GY 912 from
     * createinfo zone, without tests poking map/zone/team (regression vs Hero-only poke).
     */
    @Test
    void tpSl17RepopWhenBloodElfMageShouldUseSunstriderSpiritHealer() {
        assertBloodElfRepopAtSunstriderGy(8, "Bemage");
    }

    /**
     * TP-SL17-021 — Hero (classless) Blood Elf at Sarrandor: closest spirit healer 912,
     * not void / Horde default. Create+login only — no zone poke.
     */
    @Test
    void tpSl17RepopWhenHeroBloodElfShouldUseSunstriderSpiritHealer() {
        assertBloodElfRepopAtSunstriderGy(org.tbc.world.classless.ClasslessConfig.CLASS_CLASSLESS,
                "Behero");
    }

    /**
     * TP-SL17-025 — KillPlayer roots; the 8606 client answers CMSG_FORCE_MOVE_ROOT_ACK with a
     * raw u64 guid (CMaNGOS HandleForceSpeedChangeAck {@code >> ObjectGuid}). Release must still
     * use the death spot → Sunstrider GY 912 on map 530, not a misparsed void / Horde default.
     */
    @Test
    void tpSl17RepopAfterRootAckRawGuidShouldUseSunstriderSpiritHealer() {
        World world = World.inMemory();
        WowClientDouble client = loginBloodElf(world, "Berootack",
                org.tbc.world.classless.ClasslessConfig.CLASS_CLASSLESS);
        Player p = client.session().player();
        p.relocate(10381.6f, -6399.23f, 38.5306f, 3.74096f);
        DeathHandler.killPlayer(client.session(), world);
        assertTrue(client.saw(Opcodes.SMSG_FORCE_MOVE_ROOT));
        client.handle(world, Opcodes.CMSG_FORCE_MOVE_ROOT_ACK,
                rawGuidAck(p.guid, 10381.6f, -6399.23f, 38.5306f, 3.74096f));
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(530, WowClientDouble.u32le(loc, 0));
        assertEquals(10458.5f, WowClientDouble.floatle(loc, 4), 0.5f);
        assertEquals(-6364.61f, WowClientDouble.floatle(loc, 8), 0.5f);
        assertEquals(39.7907f, WowClientDouble.floatle(loc, 12), 0.5f);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    /**
     * TP-SL17-026 — HandleMoveTeleportAckOpcode: the client answers the repop MSG_MOVE_TELEPORT_ACK
     * with raw guid + counter + time only. The ghost stays at the spirit healer (teleport dest),
     * not relocated to (0,0,0) from a MovementInfo the packet does not carry.
     */
    @Test
    void tpSl17TeleportAckAfterRepopShouldKeepGhostAtSpiritHealer() {
        World world = World.inMemory();
        WowClientDouble client = loginBloodElf(world, "Betpack",
                org.tbc.world.classless.ClasslessConfig.CLASS_CLASSLESS);
        Player p = client.session().player();
        p.relocate(10381.6f, -6399.23f, 38.5306f, 3.74096f);
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        WowBuffer ack = new WowBuffer(16);
        ack.putU64(p.guid);
        ack.putU32(0);
        ack.putU32(1000);
        client.handle(world, Opcodes.MSG_MOVE_TELEPORT_ACK, ack.array());
        assertEquals(10458.5f, p.x, 0.5f);
        assertEquals(-6364.61f, p.y, 0.5f);
        assertEquals(39.7907f, p.z, 0.5f);
    }

    /**
     * CMaNGOS TeleportTo clears MOVEFLAG_FALLING* so a ghost who died in the void still
     * lands at the GY instead of keeping fall flags on MSG_MOVE_TELEPORT_ACK.
     */
    @Test
    void tpSl17RepopWhenBloodElfFallingShouldClearFallFlagsAtSpiritHealer() {
        World world = World.inMemory();
        WowClientDouble client = loginBloodElf(world, "Befall",
                org.tbc.world.classless.ClasslessConfig.CLASS_CLASSLESS);
        Player p = client.session().player();
        p.relocate(10381.6f, -6399.23f, -200f, 3.74f);
        p.movement.moveFlags = org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_FALLING
                | org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_FALLINGFAR;
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertEquals(10458.5f, p.x, 0.5f);
        assertEquals(39.7907f, p.z, 0.5f);
        assertEquals(0, p.movement.moveFlags);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(530, WowClientDouble.u32le(loc, 0));
        assertEquals(10458.5f, WowClientDouble.floatle(loc, 4), 0.5f);
        assertEquals(39.7907f, WowClientDouble.floatle(loc, 12), 0.5f);
    }

    /**
     * TP-SL17-023 — Undead create (Tirisfal 85): Deathknell GY 94, not Barrens / void.
     */
    @Test
    void tpSl17RepopWhenUndeadShouldUseDeathknellSpiritHealer() {
        assertStarterRepop(5, 1, "UndeadGy", 0, 85,
                1882.94f, 1629.11f, 94.4175f);
    }

    /**
     * TP-SL17-023 — Orc create (Durotar 14): Valley of Trials GY 709, not Crossroads far jump.
     */
    @Test
    void tpSl17RepopWhenOrcShouldUseValleyOfTrialsSpiritHealer() {
        assertStarterRepop(2, 1, "OrcGy", 1, 14,
                -634.635f, -4296.03f, 40.5254f);
    }

    /**
     * Starter-race create→repop matrix (void guard): same-map GY, MSG_MOVE_TELEPORT_ACK, Z match.
     * race,clazz,name,expectMap,expectZone,gyX,gyY,gyZ
     */
    @ParameterizedTest(name = "tpSl17StarterRepopMatrix {2}")
    @CsvSource({
            "1, 1, HumanGy, 0, 12, -8935.33, -188.646, 80.4165",
            "2, 1, OrcMatrix, 1, 14, -634.635, -4296.03, 40.5254",
            "4, 1, NightElfGy, 1, 141, 10384.8, 811.531, 1317.54",
            "5, 1, UndeadMatrix, 0, 85, 1882.94, 1629.11, 94.4175",
            "6, 1, TaurenGy, 1, 215, -2944.56, -153.215, 65.786",
            "10, 8, BeMageMatrix, 530, 3431, 10458.5, -6364.61, 39.7907",
            "11, 1, DraeneiMatrix, 530, 3526, -4123.14, -13660.1, 74.6",
            "1, 6, HeroHumanGy, 0, 12, -8935.33, -188.646, 80.4165",
            "10, 6, HeroBeGy, 530, 3431, 10458.5, -6364.61, 39.7907",
    })
    void tpSl17StarterRepopMatrixShouldLandAtSameMapSpiritHealer(int race, int clazz, String name,
            int expectMap, int expectZone, float gyX, float gyY, float gyZ) {
        assertStarterRepop(race, clazz, name, expectMap, expectZone, gyX, gyY, gyZ);
    }

    /**
     * TP-SL17-022 — Draenei warrior create (Ammen Vale 3526): repop at world_safe_locs 918,
     * not Alliance EK default / void. Create+login only — no zone poke.
     */
    @Test
    void tpSl17RepopWhenDraeneiShouldUseAmmenValeSpiritHealer() {
        World world = World.inMemory();
        WowClientDouble client = loginRace(world, "DraeneiGy", 11, 1);
        Player p = client.session().player();
        assertEquals(530, p.mapId);
        assertEquals(org.tbc.world.map.AreaTable.AMMEN_VALE, p.zoneId);
        assertEquals(org.tbc.world.map.GraveyardManager.ALLIANCE, p.team);
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(530, WowClientDouble.u32le(loc, 0));
        assertEquals(-4123.14f, WowClientDouble.floatle(loc, 4), 0.5f);
        assertEquals(-13660.1f, WowClientDouble.floatle(loc, 8), 0.5f);
        assertEquals(74.6f, WowClientDouble.floatle(loc, 12), 0.5f);
        assertEquals(530, p.mapId);
        assertEquals(-4123.14f, p.x, 0.5f);
        assertEquals(74.6f, p.z, 0.5f);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    /**
     * TP-SL17-020 — Die in Shadowglen (createinfo zone Teldrassil 141): world_safe_locs 93 Aldrassil,
     * not Alliance default Elwynn / mid-air cloud void.
     */
    @Test
    void tpSl17RepopWhenShadowglenShouldUseNearestAldrassilSpiritHealer() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Shadowglen");
        Player p = client.session().player();
        world.map(p.mapId, p.instanceId).remove(p);
        p.mapId = 1;
        p.zoneId = org.tbc.world.map.AreaTable.TELDRASSIL;
        p.zoneClient = 0;
        p.team = org.tbc.world.map.GraveyardManager.ALLIANCE;
        p.relocate(10311.3f, 831.463f, 1326.41f, 0f);
        world.map(1, 0).add(p);
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(1, WowClientDouble.u32le(loc, 0));
        assertEquals(10384.8f, WowClientDouble.floatle(loc, 4), 0.5f);
        assertEquals(811.531f, WowClientDouble.floatle(loc, 8), 0.5f);
        assertEquals(1317.54f, WowClientDouble.floatle(loc, 12), 0.5f);
        assertEquals(1, p.mapId);
        assertEquals(10384.8f, p.x, 0.5f);
        assertEquals(1317.54f, p.z, 0.5f);
        assertTrue(p.ghost);
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    @Test
    void tpSl17RepopWhenDunMoroghShouldUseClosestGraveyard() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Piep");
        Player p = client.session().player();
        p.relocate(-6240f, 331f, 383f, 0);
        p.zoneId = 1;
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(0, WowClientDouble.u32le(loc, 0));
        float gx = WowClientDouble.floatle(loc, 4);
        float gy = WowClientDouble.floatle(loc, 8);
        assertEquals(-6220f, gx, 0.01);
        assertEquals(330f, gy, 0.01);
        assertTrue(Math.abs(gx - DeathHandler.GY_ELWYNN_X) > 100);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    @Test
    void tpSl17ReclaimHalfHpNoSickness() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ghost");
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        p.ghostTimeMs = world.nowMs() - DeathHandler.CORPSE_RECLAIM_DELAY_FIRST_MS;
        p.relocate(p.corpse.x, p.corpse.y, p.corpse.z, 0);
        WowBuffer reclaim = new WowBuffer(8);
        reclaim.putU64(p.guid);
        client.handle(world, Opcodes.CMSG_RECLAIM_CORPSE, reclaim.array());
        assertFalse(p.ghost);
        assertEquals(p.maxHealth() / 2, p.health());
        assertTrue(p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.SICKNESS));
    }

    /**
     * TP-SL17-011 — Ghost gossip on a spirit healer casts dummy 17251 →
     * SMSG_SPIRIT_HEALER_CONFIRM (raw NPC guid). CMSG_SPIRIT_HEALER_ACTIVATE then
     * ResurrectPlayer(0.5) on the wire: not ghost, 50% HP/mana/energy VALUES,
     * PLAYER_FLAGS without GHOST, land walk.
     */
    @Test
    void tpSl17SpiritHealerReviveRestoresLivingStatsOnWire() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Healed");
        Player p = client.session().player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 0);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        p.setInt(UpdateFields.UNIT_FIELD_POWER4, 0);
        p.setInt(UpdateFields.UNIT_FIELD_POWER2, 500);
        Creature healer = spawnSpiritHealer(world, p);
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        healer.relocate(p.x, p.y, p.z, p.o);
        client.clear();
        client.gossipHello(world, healer.guid);
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_MESSAGE));
        client.gossipSelect(world, healer.guid, 0, 0);
        byte[] confirm = lastPayload(client, Opcodes.SMSG_SPIRIT_HEALER_CONFIRM);
        assertEquals(healer.guid, WowClientDouble.u64le(confirm, 0));
        client.clear();
        WowBuffer activate = new WowBuffer(8);
        activate.putU64(healer.guid);
        client.handle(world, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE, activate.array());
        assertFalse(p.ghost);
        assertEquals(p.maxHealth() / 2, p.health());
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(50, p.getInt(UpdateFields.UNIT_FIELD_POWER4));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_POWER2));
        assertEquals(p.maxHealth() / 2, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(100, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(50, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER4));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST);
        assertTrue(client.saw(Opcodes.SMSG_MOVE_LAND_WALK));
    }

    /**
     * TP-SL17-016 — Spirit service (healer/guide) CREATE only for ghosts
     * ({@code isInvisibleForAlive}). Living {@code revealNearby} skips them; resurrect
     * sends {@code SMSG_DESTROY_OBJECT} raw guid and drops them from seen.
     */
    @Test
    void tpSl17SpiritHealersVisibleOnlyToGhosts() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "SeeHealer");
        Player p = client.session().player();
        Creature healer = spawnSpiritHealer(world, p);
        client.clear();
        client.session().forgetSeen();
        client.session().revealNearby(world);
        assertFalse(client.sawCreateObject(healer.guid));

        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        healer.relocate(p.x, p.y, p.z, p.o);
        client.clear();
        client.session().forgetSeen();
        client.session().revealNearby(world);
        assertTrue(client.sawCreateObject(healer.guid));

        client.clear();
        WowBuffer activate = new WowBuffer(8);
        activate.putU64(healer.guid);
        client.handle(world, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE, activate.array());
        assertFalse(p.ghost);
        assertEquals(healer.guid, WowClientDouble.u64le(lastPayload(client, Opcodes.SMSG_DESTROY_OBJECT), 0));
        client.clear();
        client.session().revealNearby(world);
        assertFalse(client.sawCreateObject(healer.guid));
    }

    /**
     * TP-SL17-017 — Ghosts cannot attack or use living-world interact (vendor/GO);
     * spirit healer gossip still works. Creatures skip ghost aggro.
     */
    @Test
    void tpSl17GhostCannotAttackOrGossipVendor() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "GhostLock");
        Player p = client.session().player();
        Creature kobold = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(kobold);
        Creature vendor = world.objectMgr.spawnCreature(Content.NPC_CORINA_STEELE, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(vendor);
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        kobold.relocate(p.x, p.y, p.z, p.o);
        vendor.relocate(p.x, p.y, p.z, p.o);
        client.clear();
        client.attackSwing(world, kobold.guid);
        assertFalse(client.saw(Opcodes.SMSG_ATTACKSTART));
        client.clear();
        client.gossipHello(world, vendor.guid);
        assertFalse(client.saw(Opcodes.SMSG_GOSSIP_MESSAGE));
        client.clear();
        WowBuffer goUse = new WowBuffer(8);
        goUse.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, goUse.array());
        assertFalse(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
        Creature healer = spawnSpiritHealer(world, p);
        healer.relocate(p.x, p.y, p.z, p.o);
        client.clear();
        client.gossipHello(world, healer.guid);
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_MESSAGE));
    }

    /**
     * TP-SL17-012 — Unit::Kill of a player: attackers EnterEvadeMode / MoveTargetedHome
     * so they walk back to spawn instead of standing on the corpse.
     */
    @Test
    void tpSl17CreaturesWhenPlayerDiesShouldEvadeHome() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "EvadeHome");
        Player p = client.session().player();
        Creature killer = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(killer);
        float homeX = killer.spawnX;
        float homeY = killer.spawnY;
        float fightX = homeX + 20f;
        float fightY = homeY;
        killer.relocate(fightX, fightY, killer.z, killer.o);
        world.map(p.mapId, p.instanceId).reindex(killer, homeX, homeY);
        Creature add = world.objectMgr.spawnCreature(6, 0, homeX, homeY, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(add);
        add.relocate(fightX, fightY, add.z, add.o);
        world.map(p.mapId, p.instanceId).reindex(add, homeX, homeY);
        add.inCombat = true;
        add.victim = p.guid;
        add.threatManager.add(p, 10f);
        float ox = p.x;
        float oy = p.y;
        p.relocate(fightX, fightY, p.z, p.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, killer.guid);
        p.setHealth(1);
        client.clear();
        int n = 0;
        while (p.alive() && n++ < 400) {
            world.creatureMeleeHit(killer, p);
        }
        assertFalse(p.alive());
        assertTrue(killer.evading || killer.motion.type() == org.tbc.world.ai.MotionMaster.HOME);
        assertTrue(add.evading || add.motion.type() == org.tbc.world.ai.MotionMaster.HOME);
        world.tick(500);
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
    }

    /**
     * TP-SL17-013 — Player::Update skips RegenerateAll unless IsAlive(); ghosts keep HP 1
     * and do not use out-of-combat spirit regen.
     */
    @Test
    void tpSl17GhostShouldNotRegenerateHealthOrMana() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "NoRegen");
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertTrue(p.ghost);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 40);
        client.clear();
        world.tick(Player.REGEN_TIME_FULL);
        assertEquals(1, p.health());
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertFalse(client.saw(Opcodes.SMSG_UPDATE_OBJECT));
        assertFalse(client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    /**
     * TP-SL17-015 — Logout while ghost persists PLAYER_FLAGS_GHOST; the next login
     * create-self is still dead (HP 1, aura 8326, water walk, corpse delay).
     */
    @Test
    void tpSl17LogoutWhileGhostShouldLoginStillGhost() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "StayDead");
        Player p = client.session().player();
        long guid = p.guid;
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertTrue(p.ghost);
        client.session().logout(world, true);

        WowClientDouble relog = new WowClientDouble();
        relog.connect(ACC);
        relog.login(world, guid);
        Player loaded = relog.session().player();
        assertTrue(loaded.ghost);
        assertEquals(1, loaded.health());
        Map<Integer, Integer> self = relog.selfCreateValues();
        assertEquals(Player.PLAYER_FLAGS_GHOST,
                self.getOrDefault(UpdateFields.PLAYER_FLAGS, 0) & Player.PLAYER_FLAGS_GHOST);
        assertEquals(1, self.getOrDefault(UpdateFields.UNIT_FIELD_HEALTH, 0).intValue());
        boolean ghostAura = false;
        for (int slot = 0; slot < 56; slot++) {
            if (Integer.valueOf(PvpObjectives.GHOST_AURA).equals(self.get(UpdateFields.UNIT_FIELD_AURA + slot))) {
                ghostAura = true;
                break;
            }
        }
        assertTrue(ghostAura);
        assertTrue(loaded.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA));
        assertTrue(relog.saw(Opcodes.SMSG_MOVE_WATER_WALK));
        assertEquals(DeathHandler.CORPSE_RECLAIM_DELAY_FIRST_MS,
                WowClientDouble.u32le(lastPayload(relog, Opcodes.SMSG_CORPSE_RECLAIM_DELAY), 0));
    }

    @Test
    void tpSl17SpiritHealerSickness() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ghost");
        Player p = client.session().player();
        p.setGhost(true);
        p.level = 11;
        Item gear = new Item(world.nextItemGuid(), 25);
        gear.durability = 100;
        p.items.put((int) gear.guid, gear);
        client.clear();
        client.handle(world, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE, new byte[8]);
        assertEquals(p.maxHealth() / 2, p.health());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.SICKNESS));
        assertEquals(75, gear.durability);
        assertTrue(sawSpellGo(client, PvpObjectives.SICKNESS));
    }

    @Test
    void tpSl17SelfResWhenSpellSetShouldCastAndClear() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Soul");
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        // Reincarnation 20625 — death.md CMSG_SELF_RES casts PLAYER_SELF_RES_SPELL.
        int selfRes = 20625;
        p.setInt(UpdateFields.PLAYER_SELF_RES_SPELL, selfRes);
        client.clear();
        client.handle(world, Opcodes.CMSG_SELF_RES, new byte[0]);
        assertTrue(sawSpellGo(client, selfRes));
        assertEquals(0, p.getInt(UpdateFields.PLAYER_SELF_RES_SPELL));
    }

    @Test
    void tpSl17ResurrectResponseAcceptShouldApplyRequestHp() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Rez");
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        long caster = 0x0000000000000064L;
        int requestHp = 42;
        client.clear();
        DeathHandler.offerResurrect(client.session(), caster, "Healer", false, requestHp, 0);
        byte[] req = lastPayload(client, Opcodes.SMSG_RESURRECT_REQUEST);
        assertEquals(caster, WowClientDouble.u64le(req, 0));
        client.clear();
        WowBuffer resp = new WowBuffer(9);
        resp.putU64(caster);
        resp.putU8(1);
        client.handle(world, Opcodes.CMSG_RESURRECT_RESPONSE, resp.array());
        assertFalse(p.ghost);
        assertEquals(requestHp, p.health());
    }

    @Test
    void tpSl17KillPlayerTimerWhenExpiredShouldAutoRepop() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Timer");
        Player p = client.session().player();
        DeathHandler.killPlayer(client.session(), world);
        assertFalse(p.ghost);
        client.clear();
        world.advanceMs(DeathHandler.DEATH_TIMER_MS);
        DeathHandler.tickDeathTimers(world);
        assertTrue(p.ghost);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA));
        assertNotNull(lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC));
    }

    /**
     * TP-SL17-008 — Player::DurabilityLossAll(0.10f, false) on KillPlayer. Equipped
     * Worn Shortsword 25 at 20/20 loses 10% (ITEM_FIELD_DURABILITY VALUES 18); backpack
     * and bank stay 20 (death.md; Unit.cpp DealDamage durability packet).
     */
    @Test
    void tpSl17DeathDurabilityLoss() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "DeathDurability");
        Player p = client.session().player();
        Item equipped = durableSword(world, Player.EQUIPMENT_SLOT_MAINHAND);
        Item bag = durableSword(world, p.firstFreeBagSlot());
        Item bank = durableSword(world, Player.BANK_SLOT_ITEM_START);
        p.items.put((int) equipped.guid, equipped);
        p.items.put((int) bag.guid, bag);
        p.items.put((int) bank.guid, bank);
        client.clear();
        DeathHandler.killPlayer(client.session(), world);
        assertEquals(18, client.valuesField(UpdateBuilder.itemGuid(equipped), UpdateFields.ITEM_FIELD_DURABILITY));
        assertEquals(20, bag.durability);
        assertEquals(20, bank.durability);
        byte[] deathDur = lastPayload(client, Opcodes.SMSG_DURABILITY_DAMAGE_DEATH);
        assertEquals(0, deathDur.length);
    }

    /**
     * TP-SL17-009 — CMSG_REPAIR_ITEM cost is lost × DurabilityCosts[ilvl] × DurabilityQuality
     * (inventory.md); broke player is left unrepaired.
     */
    @Test
    void tpSl17RepairCost() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Repair");
        Player p = client.session().player();
        Creature smith = world.objectMgr.spawnCreature(Content.NPC_CORINA_STEELE,
                0, p.x, p.y, p.z, p.o, world.scripts);
        smith.npcFlags |= Content.UNIT_NPC_FLAG_REPAIR;
        world.map(p.mapId, p.instanceId).add(smith);
        Item first = durableSword(world, p.firstFreeBagSlot());
        first.durability = 10;
        p.items.put((int) first.guid, first);
        Item second = durableSword(world, p.firstFreeBagSlot());
        second.durability = 15;
        p.items.put((int) second.guid, second);
        p.setMoney(100);
        client.clear();
        WowBuffer one = new WowBuffer(17);
        one.putU64(smith.guid);
        one.putU64(first.guid);
        one.putU8(0);
        client.handle(world, Opcodes.CMSG_REPAIR_ITEM, one.array());
        assertEquals(20, client.valuesField(UpdateBuilder.itemGuid(first), UpdateFields.ITEM_FIELD_DURABILITY));
        assertEquals(92, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COINAGE));
        assertEquals(15, second.durability);
        client.clear();
        WowBuffer all = new WowBuffer(17);
        all.putU64(smith.guid);
        all.putU64(0);
        all.putU8(0);
        client.handle(world, Opcodes.CMSG_REPAIR_ITEM, all.array());
        assertEquals(20, client.valuesField(UpdateBuilder.itemGuid(second), UpdateFields.ITEM_FIELD_DURABILITY));
        assertEquals(88, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COINAGE));
        p.setMoney(0);
        first.durability = 10;
        client.clear();
        client.handle(world, Opcodes.CMSG_REPAIR_ITEM, all.array());
        assertEquals(10, first.durability);
        assertEquals(0, p.money);
        assertFalse(client.saw(Opcodes.SMSG_UPDATE_OBJECT));
        assertFalse(client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    private static Creature spawnSpiritHealer(World world, Player p) {
        int entry = 6491;
        world.objectMgr.creatures.put(entry, new org.tbc.world.content.ObjectMgr.CreatureTemplate(
                entry, "Spirit Healer", 0, 35, 100, 60,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_SPIRITHEALER, "", "", 0));
        Creature healer = world.objectMgr.spawnCreature(entry, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(healer);
        return healer;
    }

    private static Item durableSword(World world, int slot) {
        Item it = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        it.slot = slot;
        it.durability = 20;
        it.maxDurability = 20;
        return it;
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static WowClientDouble loginBloodElf(World world, String name, int clazz) {
        return loginRace(world, name, 10, clazz);
    }

    private static WowClientDouble loginRace(World world, String name, int race, int clazz) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, race, clazz, 0, 1, 1, 1, 1, 0, world.objectMgr);
        assertNotNull(created);
        client.login(world, created.guid);
        return client;
    }

    private static void assertBloodElfRepopAtSunstriderGy(int clazz, String name) {
        World world = World.inMemory();
        WowClientDouble client = loginBloodElf(world, name, clazz);
        Player p = client.session().player();
        assertEquals(530, p.mapId);
        assertEquals(3431, p.zoneId);
        assertEquals(org.tbc.world.map.GraveyardManager.HORDE, p.team);
        p.relocate(10381.6f, -6399.23f, 38.5306f, 3.74096f);
        assertRepopAtGy(world, client, p, 530, 10458.5f, -6364.61f, 39.7907f);
    }

    private static void assertStarterRepop(int race, int clazz, String name, int expectMap, int expectZone,
                                           float gyX, float gyY, float gyZ) {
        World world = World.inMemory();
        WowClientDouble client = loginRace(world, name, race, clazz);
        Player p = client.session().player();
        assertEquals(expectMap, p.mapId);
        assertEquals(expectZone, p.zoneId);
        assertRepopAtGy(world, client, p, expectMap, gyX, gyY, gyZ);
    }

    private static void assertRepopAtGy(World world, WowClientDouble client, Player p,
                                        int expectMap, float gyX, float gyY, float gyZ) {
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(expectMap, WowClientDouble.u32le(loc, 0));
        assertEquals(gyX, WowClientDouble.floatle(loc, 4), 0.5f);
        assertEquals(gyY, WowClientDouble.floatle(loc, 8), 0.5f);
        assertEquals(gyZ, WowClientDouble.floatle(loc, 12), 0.5f);
        assertEquals(expectMap, p.mapId);
        assertEquals(gyX, p.x, 0.5f);
        assertEquals(gyZ, p.z, 0.5f);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    /** movement.md force/flag ACK: raw u64 guid + u32 counter + MovementInfo. */
    private static byte[] rawGuidAck(long guid, float x, float y, float z, float o) {
        org.tbc.world.net.wow8606.MovementInfo m = new org.tbc.world.net.wow8606.MovementInfo();
        m.x = x;
        m.y = y;
        m.z = z;
        m.o = o;
        WowBuffer ack = new WowBuffer(64);
        ack.putU64(guid);
        ack.putU32(0);
        m.write(ack, false, guid, 1000);
        return ack.array();
    }

    private static boolean sawSpellGo(WowClientDouble client, int spellId) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_SPELL_GO) {
                continue;
            }
            byte[] p = client.payloads.get(i);
            for (int off = 0; off + 4 <= p.length; off++) {
                if (WowClientDouble.u32le(p, off) == spellId) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }
}
