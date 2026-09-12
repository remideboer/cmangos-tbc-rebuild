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
