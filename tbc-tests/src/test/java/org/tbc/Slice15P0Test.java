package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.loot.GroupLoot;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** TP-SL15-* from packet files, one criterion per method. */
class Slice15P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl15LootNeedVsGreed() {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        WowClientDouble b = new WowClientDouble();
        a.connect(ACC_A);
        b.connect(ACC_B);
        Player pa = world.characters.create(ACC_A.id(), "Need", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player pb = world.characters.create(ACC_B.id(), "Greed", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, pa.guid);
        b.login(world, pb.guid);
        a.groupInvite(world, "Greed");
        b.groupAccept(world);

        Creature corpse = find(world, 6);
        pa.relocate(corpse.x, corpse.y, corpse.z, corpse.o);
        pb.relocate(corpse.x, corpse.y, corpse.z, corpse.o);
        corpse.lootable = true;
        corpse.taggedBy = pa.guid;

        a.clear();
        b.clear();
        WowBuffer method = new WowBuffer(16);
        method.putU32(GroupLoot.METHOD_NEED_BEFORE_GREED);
        method.putU64(pa.guid);
        method.putU32(2);
        a.handle(world, Opcodes.CMSG_LOOT_METHOD, method.array());
        byte[] list = lastPayload(a, Opcodes.SMSG_GROUP_LIST);
        assertEquals(0, list[0] & 0xFF, "party (not raid) type");
        assertEquals(GroupLoot.METHOD_NEED_BEFORE_GREED, lootMethodFromList(list));

        a.clear();
        b.clear();
        a.loot(world, corpse.guid);

        byte[] startA = lastPayload(a, Opcodes.SMSG_LOOT_START_ROLL);
        byte[] startB = lastPayload(b, Opcodes.SMSG_LOOT_START_ROLL);
        assertStartRoll(startA, corpse.guid);
        assertStartRoll(startB, corpse.guid);

        WowBuffer need = roll(corpse.guid, 0, GroupLoot.ROLL_NEED);
        WowBuffer greed = roll(corpse.guid, 0, GroupLoot.ROLL_GREED);
        a.handle(world, Opcodes.CMSG_LOOT_ROLL, need.array());
        b.handle(world, Opcodes.CMSG_LOOT_ROLL, greed.array());

        byte[] won = lastPayload(a, Opcodes.SMSG_LOOT_ROLL_WON);
        WowBuffer w = new WowBuffer(won);
        assertEquals(corpse.guid, w.getU64());
        assertEquals(0, w.getU32());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, w.getU32());
        w.getU32();
        w.getU32();
        assertEquals(pa.guid, w.getU64());
        w.getU8();
        assertEquals(GroupLoot.ROLL_NEED, w.getU8());
        assertTrue(b.saw(Opcodes.SMSG_LOOT_ROLL_WON));
    }

    @Test
    void tpSl15RaidConvertReadyCheck() {
        Pair g = loginTwo("Raider", "Mate");
        g.a.clear();
        g.b.clear();
        g.a.handle(g.world, Opcodes.CMSG_GROUP_RAID_CONVERT, new byte[0]);
        assertEquals(1, lastPayload(g.a, Opcodes.SMSG_GROUP_LIST)[0] & 0xFF);
        assertEquals(1, lastPayload(g.b, Opcodes.SMSG_GROUP_LIST)[0] & 0xFF);
        g.a.clear();
        g.b.clear();
        g.a.handle(g.world, Opcodes.MSG_RAID_READY_CHECK, new byte[0]);
        long requester = g.a.session().player().guid;
        assertEquals(requester, WowClientDouble.u64le(lastPayload(g.a, Opcodes.MSG_RAID_READY_CHECK), 0));
        assertEquals(requester, WowClientDouble.u64le(lastPayload(g.b, Opcodes.MSG_RAID_READY_CHECK), 0));
    }

    @Test
    void tpSl15BuyGuildCharter() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_A);
        Player created = world.characters.create(ACC_A.id(), "Buyer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        client.clear();
        client.petitionBuy(world, npc.guid, "CharterGuild", 1);
        byte[] push = lastPayload(client, Opcodes.SMSG_ITEM_PUSH_RESULT);
        assertEquals(1, WowClientDouble.u32le(push, 8), "received from NPC");
        assertEquals(Content.ITEM_GUILD_CHARTER, WowClientDouble.u32le(push, 25));
        assertEquals(0, p.money);
    }

    @Test
    void tpSl15SignGuildCharter() {
        Pair g = loginTwo("Owner", "Signer");
        Player owner = g.a.session().player();
        owner.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(g.world, Content.NPC_REBECCA_LAUGHLIN);
        owner.relocate(npc.x, npc.y, npc.z, npc.o);
        g.b.session().player().relocate(npc.x, npc.y, npc.z, npc.o);
        g.a.clear();
        g.a.petitionBuy(g.world, npc.guid, "SignGuild", 1);
        long petition = charterGuid(owner);
        g.a.clear();
        g.b.clear();
        g.b.petitionSign(g.world, petition);
        byte[] signed = lastPayload(g.b, Opcodes.SMSG_PETITION_SIGN_RESULTS);
        WowBuffer r = new WowBuffer(signed);
        assertEquals(petition, r.getU64());
        assertEquals(g.b.session().player().guid, r.getU64());
        assertEquals(0, r.getU32());
        assertTrue(g.a.saw(Opcodes.SMSG_PETITION_SIGN_RESULTS));
    }

    @Test
    void tpSl15TurnInGuildCharter() {
        Pair g = loginTwo("Founder", "Signer");
        g.world.minPetitionSigns = 1;
        Player owner = g.a.session().player();
        owner.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(g.world, Content.NPC_REBECCA_LAUGHLIN);
        owner.relocate(npc.x, npc.y, npc.z, npc.o);
        g.b.session().player().relocate(npc.x, npc.y, npc.z, npc.o);
        g.a.petitionBuy(g.world, npc.guid, "TurnGuild", 1);
        long petition = charterGuid(owner);
        g.b.petitionSign(g.world, petition);
        g.a.clear();
        g.a.petitionTurnIn(g.world, petition);
        assertEquals(0, WowClientDouble.u32le(lastPayload(g.a, Opcodes.SMSG_TURN_IN_PETITION_RESULTS), 0));
        assertFalse(g.a.saw(Opcodes.SMSG_ARENA_TEAM_ROSTER));
    }

    @Test
    void tpSl15GuildRosterGender() {
        Pair g = loginTwo("Raider", "Mate");
        Player p = g.a.session().player();
        g.a.clear();
        WowBuffer create = new WowBuffer(16);
        create.putCString("TestGuild");
        g.a.handle(g.world, Opcodes.CMSG_GUILD_CREATE, create.array());
        WowBuffer r = new WowBuffer(lastPayload(g.a, Opcodes.SMSG_GUILD_ROSTER));
        assertEquals(1, r.getU32());
        r.getCString();
        r.getCString();
        int ranks = r.getU32();
        assertTrue(ranks >= 1);
        for (int i = 0; i < ranks; i++) {
            r.getU32();
            r.getU32();
            for (int t = 0; t < 6; t++) {
                r.getU32();
                r.getU32();
            }
        }
        assertEquals(p.guid, r.getU64());
        assertEquals(1, r.getU8());
        assertEquals("Raider", r.getCString());
        r.getU32();
        assertEquals(p.level, r.getU8());
        assertEquals(p.clazz, r.getU8());
        assertEquals(p.gender, r.getU8());
    }

    @Test
    void tpSl15GuildBankSwap() {
        Pair g = loginTwo("Banker", "Mate");
        Player p = g.a.session().player();
        WowBuffer create = new WowBuffer(16);
        create.putCString("BankGuild");
        g.a.handle(g.world, Opcodes.CMSG_GUILD_CREATE, create.array());
        Item it = new Item(g.world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        it.slot = p.firstFreeBagSlot();
        p.items.put((int) it.guid, it);
        int bagSlot = it.slot;
        g.a.clear();
        WowBuffer activate = new WowBuffer(9);
        activate.putU64(1);
        activate.putU8(0);
        g.a.handle(g.world, Opcodes.CMSG_GUILD_BANKER_ACTIVATE, activate.array());
        assertTrue(g.a.saw(Opcodes.SMSG_GUILD_BANK_LIST));
        g.a.clear();
        WowBuffer deposit = new WowBuffer(24);
        deposit.putU64(1);
        deposit.putU8(0);
        deposit.putU8(0);
        deposit.putU8(0);
        deposit.putU32(Content.ITEM_WORN_SHORTSWORD);
        deposit.putU8(0);
        deposit.putU8(0);
        deposit.putU8(bagSlot);
        deposit.putU8(0);
        deposit.putU32(0);
        g.a.handle(g.world, Opcodes.CMSG_GUILD_BANK_SWAP_ITEMS, deposit.array());
        WowBuffer list = new WowBuffer(lastPayload(g.a, Opcodes.SMSG_GUILD_BANK_LIST));
        list.getU64();
        assertEquals(0, list.getU8());
        list.getU32();
        assertEquals(0, list.getU8());
        assertEquals(1, list.getU8());
        assertEquals(0, list.getU8());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, list.getU32());
        assertTrue(p.items.values().stream().noneMatch(i -> i.guid == it.guid));
        g.a.clear();
        WowBuffer withdraw = new WowBuffer(24);
        withdraw.putU64(1);
        withdraw.putU8(0);
        withdraw.putU8(0);
        withdraw.putU8(0);
        withdraw.putU32(Content.ITEM_WORN_SHORTSWORD);
        withdraw.putU8(0);
        withdraw.putU8(0);
        withdraw.putU8(p.firstFreeBagSlot() < 0 ? 23 : p.firstFreeBagSlot());
        withdraw.putU8(1);
        withdraw.putU32(0);
        g.a.handle(g.world, Opcodes.CMSG_GUILD_BANK_SWAP_ITEMS, withdraw.array());
        assertTrue(p.items.containsKey((int) it.guid));
    }

    @Test
    void tpSl15AuctionListResult() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_A);
        Player created = world.characters.create(ACC_A.id(), "Bidder", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature ah = find(world, Content.NPC_AUCTIONEER_CHILTON);
        p.relocate(ah.x, ah.y, ah.z, ah.o);
        client.clear();
        WowBuffer search = new WowBuffer(48);
        search.putU64(ah.guid);
        search.putU32(0);
        search.putCString("Worn");
        search.putU8(0);
        search.putU8(0);
        search.putU32(0xFFFFFFFF);
        search.putU32(0xFFFFFFFF);
        search.putU32(0xFFFFFFFF);
        search.putU32(0xFFFFFFFF);
        search.putU8(0);
        search.putU8(0);
        search.putU8(0);
        client.handle(world, Opcodes.CMSG_AUCTION_LIST_ITEMS, search.array());
        WowBuffer out = new WowBuffer(lastPayload(client, Opcodes.SMSG_AUCTION_LIST_RESULT));
        int count = out.getU32();
        assertEquals(1, count);
        assertEquals(1, out.getU32());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, out.getU32());
        for (int i = 0; i < 6; i++) {
            out.getU32();
            out.getU32();
            out.getU32();
        }
        out.getU32();
        out.getU32();
        out.getU32();
        out.getU32();
        out.getU32();
        out.getU64();
        out.getU32();
        out.getU32();
        out.getU32();
        out.getU32();
        out.getU64();
        out.getU32();
        int total = out.getU32();
        int delay = out.getU32();
        assertTrue(total >= 1);
        assertEquals(Content.AUCTION_LIST_DELAY_MS, delay);
    }

    private static Pair loginTwo(String aName, String bName) {
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
        return new Pair(world, a, b);
    }

    private static void assertStartRoll(byte[] payload, long lootGuid) {
        WowBuffer s = new WowBuffer(payload);
        assertEquals(lootGuid, s.getU64());
        assertEquals(0, s.getU32());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, s.getU32());
        s.getU32();
        s.getU32();
        assertEquals(GroupLoot.TIMEOUT_MS, s.getU32());
        assertEquals(GroupLoot.VOTE_MASK, s.getU8());
    }

    private static WowBuffer roll(long lootGuid, int slot, int type) {
        WowBuffer b = new WowBuffer(13);
        b.putU64(lootGuid);
        b.putU32(slot);
        b.putU8(type);
        return b;
    }

    /** SMSG_GROUP_LIST loot method byte after leader guid when there is at least one other member. */
    private static int lootMethodFromList(byte[] payload) {
        WowBuffer b = new WowBuffer(payload);
        b.getU8();
        b.getU8();
        b.getU8();
        b.getU8();
        b.getU64();
        int others = b.getU32();
        for (int i = 0; i < others; i++) {
            b.getCString();
            b.getU64();
            b.getU8();
            b.getU8();
            b.getU8();
        }
        b.getU64();
        return b.getU8();
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        throw new AssertionError("no creature entry " + entry);
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }

    private static long charterGuid(Player p) {
        for (Item it : p.items.values()) {
            if (it.entry == Content.ITEM_GUILD_CHARTER) {
                return it.guid;
            }
        }
        throw new AssertionError("no guild charter");
    }

    private record Pair(World world, WowClientDouble a, WowClientDouble b) {}
}
