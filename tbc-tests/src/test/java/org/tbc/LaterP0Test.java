package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.Honor;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.script.BossScript;
import org.tbc.world.script.ClassScripts;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaterP0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl14Weather() {
        Ctx c = loginOne("Swapper");
        assertTrue(c.loginSawWeather);
    }

    @Test
    void tpSl15SocialLeftover() {
        Ctx c = loginTwo("Raider", "Mate");
        c.a.handle(c.world, Opcodes.CMSG_GROUP_RAID_CONVERT, new byte[0]);
        assertTrue(c.a.session().player().group.raid);
        c.a.clear();
        c.b.clear();
        c.a.handle(c.world, Opcodes.MSG_RAID_READY_CHECK, new byte[0]);
        assertTrue(c.a.saw(Opcodes.MSG_RAID_READY_CHECK));
        assertTrue(c.b.saw(Opcodes.MSG_RAID_READY_CHECK));
        c.a.clear();
        WowBuffer create = new WowBuffer(16);
        create.putCString("TestGuild");
        c.a.handle(c.world, Opcodes.CMSG_GUILD_CREATE, create.array());
        assertTrue(c.a.saw(Opcodes.SMSG_GUILD_ROSTER));
    }

    @Test
    void tpSl16ArenaHellfireLfgWsgFlag() {
        Ctx c = loginOne("Pvper");
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_BATTLEMASTER_JOIN_ARENA, new byte[16]);
        assertEquals(2, WowClientDouble.u32le(c.client.payload(Opcodes.SMSG_BATTLEFIELD_STATUS), 17));
        c.client.battlefieldPort(c.world, 1);
        c.client.worldportAck(c.world);
        assertEquals(562, c.client.session().player().mapId);
        c.world.teleport(c.client.session().player(), 530, 0, 0, 0, 0);
        c.client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        c.client.handle(c.world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(c.client.saw(Opcodes.SMSG_UPDATE_WORLD_STATE));
        assertEquals(1, c.client.session().worldStates2476);
        assertEquals(1, c.client.session().worldStates2478);
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_SET_LOOKING_FOR_GROUP, new byte[8]);
        c.client.handle(c.world, Opcodes.MSG_LOOKING_FOR_GROUP, new byte[8]);
        assertTrue(c.client.saw(Opcodes.MSG_LOOKING_FOR_GROUP));
        assertEquals(c.client.session().player().guid,
                WowClientDouble.u64le(c.client.payload(Opcodes.MSG_LOOKING_FOR_GROUP), 4));
        c.world.teleport(c.client.session().player(), 489, 0, 0, 0, 0);
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(c.client.session().player().auras.stream().anyMatch(a -> a.spellId() == 23333));
    }

    @Test
    void tpSl17DeathRepopReclaimSpirit() {
        Ctx c = loginOne("Ghost");
        Player p = c.client.session().player();
        c.client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        c.client.handle(c.world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertTrue(p.ghost);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA));
        assertNotNull(p.corpse);
        assertTrue(c.client.saw(Opcodes.SMSG_DEATH_RELEASE_LOC));
        WowBuffer reclaim = new WowBuffer(8);
        reclaim.putU64(p.guid);
        c.client.handle(c.world, Opcodes.CMSG_RECLAIM_CORPSE, reclaim.array());
        assertFalse(p.ghost);
        assertEquals(p.maxHealth() / 2, p.health());
        p.ghost = true;
        p.level = 11;
        Item gear = new Item(c.world.nextItemGuid(), 25);
        gear.durability = 100;
        p.items.put((int) gear.guid, gear);
        c.client.handle(c.world, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE, new byte[8]);
        assertEquals(p.maxHealth() / 2, p.health());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.SICKNESS));
        assertEquals(75, gear.durability);
    }

    @Test
    void tpSl18PetsStableDismissTotem() {
        Ctx c = loginOne("Hunter");
        Player p = c.client.session().player();
        p.clazz = 3;
        WowBuffer act = new WowBuffer(20);
        act.putU64(0);
        act.putU32(2 | (0x07 << 24));
        act.putU64(0);
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_PET_ACTION, act.array());
        assertTrue(c.client.saw(Opcodes.SMSG_PET_SPELLS));
        assertNotNull(p.pet);
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_BUY_STABLE_SLOT, new byte[0]);
        c.client.handle(c.world, Opcodes.CMSG_STABLE_PET, new byte[0]);
        c.client.handle(c.world, Opcodes.CMSG_UNSTABLE_PET, new byte[0]);
        assertEquals(0x08, c.client.payload(Opcodes.SMSG_STABLE_RESULT)[0] == 0x09
                ? 0x08 : c.client.payload(Opcodes.SMSG_STABLE_RESULT)[0]);
        List<Byte> stable = new ArrayList<>();
        for (int i = 0; i < c.client.opcodes.size(); i++) {
            if (c.client.opcodes.get(i) == Opcodes.SMSG_STABLE_RESULT) {
                stable.add(c.client.payloads.get(i)[0]);
            }
        }
        assertTrue(stable.contains((byte) 0x08));
        assertTrue(stable.contains((byte) 0x09));
        WowBuffer dismiss = new WowBuffer(20);
        dismiss.putU64(0);
        dismiss.putU32(3 | (0x07 << 24));
        c.client.handle(c.world, Opcodes.CMSG_PET_ACTION, dismiss.array());
        assertNotNull(p.pet);
        p.clazz = 9;
        c.client.handle(c.world, Opcodes.CMSG_PET_ACTION, dismiss.array());
        assertNull(p.pet);
        p.totems[0] = 99;
        WowBuffer tot = new WowBuffer(1);
        tot.putU8(0);
        c.client.handle(c.world, Opcodes.CMSG_TOTEM_DESTROYED, tot.array());
        assertEquals(0, p.totems[0]);
    }

    @Test
    void tpSl19ChannelEmoteVoice() {
        Ctx c = loginTwo("Talker", "Wavee");
        WowBuffer join = new WowBuffer(32);
        join.putU32(0);
        join.putU8(0);
        join.putU8(0);
        join.putCString("General");
        join.putCString("");
        c.a.clear();
        c.a.handle(c.world, Opcodes.CMSG_JOIN_CHANNEL, join.array());
        assertEquals(2, c.a.payload(Opcodes.SMSG_CHANNEL_NOTIFY)[0] & 0xFF);
        assertTrue(c.a.saw(Opcodes.SMSG_CHANNEL_LIST));
        c.a.clear();
        WowBuffer list = new WowBuffer(16);
        list.putCString("General");
        c.a.handle(c.world, Opcodes.CMSG_CHANNEL_LIST, list.array());
        assertTrue(c.a.saw(Opcodes.SMSG_CHANNEL_LIST));
        assertEquals(c.a.session().player().guid,
                WowClientDouble.u64le(c.a.payload(Opcodes.SMSG_CHANNEL_LIST),
                        1 + "General".length() + 1 + 1 + 4));
        c.a.clear();
        WowBuffer emote = new WowBuffer(16);
        emote.putU32(1);
        emote.putU32(0);
        emote.putU64(c.b.session().player().guid);
        c.a.handle(c.world, Opcodes.CMSG_TEXT_EMOTE, emote.array());
        assertTrue(c.a.saw(Opcodes.SMSG_TEXT_EMOTE));
        assertTrue(c.b.saw(Opcodes.SMSG_TEXT_EMOTE));
        c.a.clear();
        c.a.handle(c.world, Opcodes.CMSG_VOICE_SESSION_ENABLE, new byte[4]);
        assertFalse(c.a.saw(Opcodes.SMSG_VOICE_SESSION_ROSTER_UPDATE));
    }

    @Test
    void tpSl20SellBuybackSocketRepair() {
        Ctx c = loginOne("Vendor");
        Player p = c.client.session().player();
        p.money = 10;
        Item it = new Item(c.world.nextItemGuid(), 25);
        it.slot = 32;
        it.durability = 10;
        p.items.put((int) it.guid, it);
        WowBuffer sell = new WowBuffer(17);
        sell.putU64(0);
        sell.putU64(it.guid);
        sell.putU8(0);
        c.client.handle(c.world, Opcodes.CMSG_SELL_ITEM, sell.array());
        assertEquals(it, p.buyback.get(74));
        assertEquals(1, p.getInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1));
        WowBuffer back = new WowBuffer(12);
        back.putU64(0);
        back.putU32(74);
        c.client.handle(c.world, Opcodes.CMSG_BUYBACK_ITEM, back.array());
        assertTrue(p.items.containsKey((int) it.guid));
        WowBuffer sock = new WowBuffer(32);
        sock.putU64(it.guid);
        sock.putU64(0);
        sock.putU64(0);
        sock.putU64(0);
        c.client.handle(c.world, Opcodes.CMSG_SOCKET_GEMS, sock.array());
        assertEquals(1, it.enchant);
        WowBuffer repair = new WowBuffer(17);
        repair.putU64(0);
        repair.putU64(0);
        repair.putU8(0);
        c.client.handle(c.world, Opcodes.CMSG_REPAIR_ITEM, repair.array());
        assertEquals(100, it.durability);
        assertEquals(9, p.money);
    }

    @Test
    void tpSl21GuildQueryBankRollPing() {
        Ctx c = loginOne("Guilded");
        Player p = c.client.session().player();
        p.guildId = 1;
        p.guildName = "Plates";
        p.money = 200000;
        WowBuffer q = new WowBuffer(4);
        q.putU32(1);
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_GUILD_QUERY, q.array());
        assertTrue(c.client.saw(Opcodes.SMSG_GUILD_QUERY_RESPONSE));
        byte[] g = c.client.payload(Opcodes.SMSG_GUILD_QUERY_RESPONSE);
        assertEquals('P', g[4]);
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_GUILD_BANK_BUY_TAB, new byte[8]);
        assertTrue(c.client.saw(Opcodes.MSG_GUILD_PERMISSIONS));
        assertEquals(6, WowClientDouble.u32le(c.client.payload(Opcodes.MSG_GUILD_PERMISSIONS), 0));
        c.client.clear();
        WowBuffer roll = new WowBuffer(8);
        roll.putU32(1);
        roll.putU32(100);
        c.client.handle(c.world, Opcodes.MSG_RANDOM_ROLL, roll.array());
        assertTrue(c.client.saw(Opcodes.MSG_RANDOM_ROLL));
        c.client.handle(c.world, Opcodes.MSG_MINIMAP_PING, new byte[]{1, 2, 3, 4});
        assertTrue(c.client.saw(Opcodes.MSG_MINIMAP_PING));
    }

    @Test
    void tpSl22HonorInspectDuel() {
        Ctx c = loginTwo("Killer", "Victim");
        Player killer = c.a.session().player();
        Player victim = c.b.session().player();
        killer.honorPoints = 74990;
        Honor.reward(killer, c.a.session(), victim.guid, 20);
        assertTrue(c.a.saw(Opcodes.SMSG_PVP_CREDIT));
        assertEquals(Honor.MAX_HONOR_POINTS, killer.honorPoints);
        Honor.midnightRoll(killer);
        assertEquals(0, killer.honorToday);
        assertEquals(20, killer.honorYesterday);
        c.a.clear();
        WowBuffer insp = new WowBuffer(16);
        insp.putPackedGuid(victim.guid);
        c.a.handle(c.world, Opcodes.CMSG_INSPECT, insp.array());
        assertTrue(c.a.saw(Opcodes.SMSG_INSPECT_TALENT));
        WowBuffer hon = new WowBuffer(8);
        hon.putU64(victim.guid);
        c.a.handle(c.world, Opcodes.MSG_INSPECT_HONOR_STATS, hon.array());
        assertTrue(c.a.saw(Opcodes.MSG_INSPECT_HONOR_STATS));
        killer.selection = victim.guid;
        WowBuffer go = new WowBuffer(8);
        go.putU64(7);
        c.a.handle(c.world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(c.b.saw(Opcodes.SMSG_DUEL_REQUESTED));
        c.a.clear();
        c.b.clear();
        c.a.handle(c.world, Opcodes.CMSG_DUEL_ACCEPTED, new byte[0]);
        assertEquals(3000, WowClientDouble.u32le(c.a.payload(Opcodes.SMSG_DUEL_COUNTDOWN), 0));
        c.a.clear();
        c.a.heartbeat(c.world, 1000, 1000, 0, 0);
        assertTrue(c.a.saw(Opcodes.SMSG_DUEL_OUTOFBOUNDS));
    }

    @Test
    void tpSl23ArenaPvpLogAfk() {
        Ctx c = loginOne("Arena");
        c.client.clear();
        c.client.handle(c.world, Opcodes.CMSG_TURN_IN_PETITION, new byte[8]);
        assertEquals(1, c.client.session().player().arenaTeam);
        c.client.handle(c.world, Opcodes.MSG_PVP_LOG_DATA, new byte[0]);
        assertEquals(0, WowClientDouble.u32le(c.client.payload(Opcodes.MSG_PVP_LOG_DATA), 0));
        c.client.handle(c.world, Opcodes.CMSG_REPORT_PVP_AFK, new byte[8]);
        c.client.handle(c.world, Opcodes.CMSG_REPORT_PVP_AFK, new byte[8]);
        c.client.handle(c.world, Opcodes.CMSG_REPORT_PVP_AFK, new byte[8]);
        assertTrue(c.client.session().player().auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.IDLE_AFK));
    }

    @Test
    void tpSl26GoTalentCancelChannel() {
        Ctx c = loginOne("Caster");
        Player p = c.client.session().player();
        c.client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        c.client.handle(c.world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(c.client.saw(Opcodes.SMSG_LOOT_RESPONSE));
        p.channeling = true;
        c.client.handle(c.world, Opcodes.MSG_TALENT_WIPE_CONFIRM, new byte[0]);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.TALENT_WIPE));
        c.client.handle(c.world, Opcodes.CMSG_CANCEL_CHANNELLING, new byte[0]);
        assertFalse(p.channeling);
    }

    @Test
    void tpSl27SpeedAckDismount() {
        Ctx c = loginOne("Mounter");
        Player p = c.client.session().player();
        p.auras.add(new Unit.Aura(PvpObjectives.MOUNT_AURA, 0, 1));
        p.mounted = true;
        WowBuffer ack = new WowBuffer(64);
        ack.putPackedGuid(p.guid);
        ack.putU32(1);
        ack.putU32(0);
        ack.putU8(0);
        ack.putU32(0);
        ack.putFloat(p.x);
        ack.putFloat(p.y);
        ack.putFloat(p.z);
        ack.putFloat(p.o);
        ack.putU32(0);
        ack.putFloat(7.0f);
        c.client.handle(c.world, Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK, ack.array());
        assertEquals(7.0f, p.lastAckSpeed);
        c.client.handle(c.world, Opcodes.CMSG_CANCEL_MOUNT_AURA, new byte[0]);
        assertFalse(p.mounted);
        assertTrue(p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.MOUNT_AURA));
    }

    @Test
    void tpSl28QuestLootTicketLfg() {
        Ctx c = loginTwo("Leader", "Share");
        c.a.clear();
        WowBuffer push = new WowBuffer(4);
        push.putU32(783);
        c.a.handle(c.world, Opcodes.CMSG_PUSHQUESTTOPARTY, push.array());
        assertTrue(c.a.saw(Opcodes.MSG_QUEST_PUSH_RESULT));
        assertTrue(c.b.saw(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
        WowBuffer give = new WowBuffer(17);
        give.putU64(1);
        give.putU8(0);
        give.putU64(c.b.session().player().guid);
        int before = c.b.session().player().items.size();
        c.a.handle(c.world, Opcodes.CMSG_LOOT_MASTER_GIVE, give.array());
        assertTrue(c.b.session().player().items.size() > before);
        c.a.clear();
        WowBuffer ticket = new WowBuffer(16);
        ticket.putCString("stuck");
        c.a.handle(c.world, Opcodes.CMSG_GMTICKET_CREATE, ticket.array());
        c.a.handle(c.world, Opcodes.CMSG_GMTICKET_GETTICKET, new byte[0]);
        assertEquals(0x06, WowClientDouble.u32le(c.a.payload(Opcodes.SMSG_GMTICKET_GETTICKET), 0));
        c.a.handle(c.world, Opcodes.CMSG_ACCEPT_LFG_MATCH, new byte[0]);
        c.a.clear();
        c.a.ping(c.world, 1);
        assertTrue(c.a.saw(Opcodes.SMSG_PONG));
    }

    @Test
    void tpSl29GmCommands() {
        World w = World.inMemory();
        WowClientDouble d = new WowClientDouble();
        d.connect(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        Player p = w.characters.create(1, "Lowsec", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        p.gmLevel = 0;
        p.mounted = true;
        assertTrue(w.gm.allowed(p, "help"));
        assertTrue(w.gm.allowed(p, "dismount"));
        assertFalse(w.gm.allowed(p, "die"));
        assertEquals("Dismounted.", w.gm.handle(w, p, ".dismount"));
        assertFalse(p.mounted);
        WowClientDouble gm = new WowClientDouble();
        gm.connect(new World.Account(2, "GM", new byte[40], 1, 1, "Win", "x86"));
        Player g = w.characters.create(2, "Mod", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        d.login(w, p.guid);
        gm.login(w, g.guid);
        Player low = d.session().player();
        Player mod = gm.session().player();
        mod.gmLevel = 1;
        low.gmLevel = 1;
        low.relocate(10, 10, 10, 0);
        GmCommands lower = new GmCommands(true);
        assertTrue(lower.handle(w, mod, ".appear Lowsec").contains("cannot appear"));
        low.gmLevel = 0;
        mod.gmLevel = 3;
        assertEquals("Appearing.", lower.handle(w, mod, ".appear Lowsec"));
        w.gm.overlay("die", 0);
        low.gmLevel = 0;
        assertTrue(w.gm.allowed(low, "die"));
    }

    @Test
    void tpSl30GruulAndMissingScript() {
        World w = World.inMemory();
        ScriptRegistry reg = w.scripts;
        assertTrue(reg.knows("boss_gruul"));
        BossScript gruul = reg.create("boss_gruul");
        gruul.aggro();
        List<Integer> casts = new ArrayList<>();
        gruul.update(new org.tbc.world.entity.Creature(), new Player(), 30_000, (c, t, id) -> casts.add(id));
        assertTrue(casts.contains(36300));
        assertNull(reg.create("missing_script_name_not_in_spec"));
        assertEquals(ClassScripts.SPELL_EXECUTE_DAMAGE, ClassScripts.warriorExecute(1).damageSpell());
        assertEquals("spell_warrior_execute", ClassScripts.key(5308));
        assertEquals("spell_unstable_affliction", ClassScripts.key(30108));
    }

    private static Ctx loginOne(String name) {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_A);
        Player created = world.characters.create(ACC_A.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Ctx c = new Ctx();
        c.world = world;
        c.client = client;
        c.loginSawWeather = client.saw(Opcodes.SMSG_WEATHER);
        return c;
    }

    private static Ctx loginTwo(String aName, String bName) {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        WowClientDouble b = new WowClientDouble();
        a.connect(ACC_A);
        b.connect(ACC_B);
        Player pa = world.characters.create(ACC_A.id(), aName, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player pb = world.characters.create(ACC_B.id(), bName, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, pa.guid);
        b.login(world, pb.guid);
        a.groupInvite(world, bName);
        b.groupAccept(world);
        Ctx c = new Ctx();
        c.world = world;
        c.a = a;
        c.b = b;
        c.client = a;
        return c;
    }

    private static final class Ctx {
        World world;
        WowClientDouble client;
        WowClientDouble a;
        WowClientDouble b;
        boolean loginSawWeather;
    }
}
