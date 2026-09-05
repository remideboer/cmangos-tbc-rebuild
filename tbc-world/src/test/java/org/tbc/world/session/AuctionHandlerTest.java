package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionHandlerTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void sellItemWhenCheaterFieldsShouldIgnore() {
        World world = World.inMemory();
        Sink sink = login(world);
        atChilton(world, sink.session.player());
        AuctionHandler.sellItem(sink.session, world, new WowBuffer(4));
        AuctionHandler.sellItem(sink.session, world, sell(0, 1, 100, 0, 720));
        AuctionHandler.sellItem(sink.session, world, sell(chilton(world).guid, 1, 0, 0, 720));
        AuctionHandler.sellItem(sink.session, world, sell(chilton(world).guid, 1, 100, 0, 1));
        AuctionHandler.sellItem(sink.session, world, sell(chilton(world).guid, 0, 100, 0, 720));
        assertFalse(sink.ops.contains(Opcodes.SMSG_AUCTION_COMMAND_RESULT));
        assertFalse(sink.ops.contains(Opcodes.SMSG_AUCTION_COMMAND_RESULT));
    }

    @Test
    void sellItemWhenNotAuctioneerShouldIgnore() {
        World world = World.inMemory();
        Sink sink = login(world);
        AuctionHandler.sellItem(sink.session, world, sell(1, 1, 100, 0, 720));
        assertFalse(sink.ops.contains(Opcodes.SMSG_AUCTION_COMMAND_RESULT));
    }

    @Test
    void sellItemWhenMoneyOrItemIllegalShouldSendError() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        atChilton(world, p);
        long ah = chilton(world).guid;
        AuctionHandler.sellItem(sink.session, world, sell(ah, 1, AuctionHandler.MAX_MONEY_AMOUNT + 1, 0, 720));
        assertEquals(AuctionHandler.AUCTION_ERR_DATABASE, errorOf(sink));
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.sellItem(sink.session, world, sell(ah, 1, 100, AuctionHandler.MAX_MONEY_AMOUNT + 1, 720));
        assertEquals(AuctionHandler.AUCTION_ERR_DATABASE, errorOf(sink));

        Item missing = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        p.items.remove((int) missing.guid);
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.sellItem(sink.session, world, sell(ah, missing.guid, 100, 0, 720));
        assertEquals(AuctionHandler.AUCTION_ERR_INVENTORY, errorOf(sink));
        assertEquals(AuctionHandler.EQUIP_ERR_ITEM_NOT_FOUND, extraOf(sink));

        Item bound = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        bound.soulbound = true;
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.sellItem(sink.session, world, sell(ah, bound.guid, 100, 0, 720));
        assertEquals(AuctionHandler.EQUIP_ERR_CANNOT_TRADE_THAT, extraOf(sink));

        Item conjured = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        conjured.flags = AuctionHandler.ITEM_FLAG_CONJURED;
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.sellItem(sink.session, world, sell(ah, conjured.guid, 100, 0, 720));
        assertEquals(AuctionHandler.EQUIP_ERR_CANNOT_TRADE_THAT, extraOf(sink));

        Item broke = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        p.setMoney(0);
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.sellItem(sink.session, world, sell(ah, broke.guid, 100, 0, 720));
        assertEquals(AuctionHandler.AUCTION_ERR_NOT_ENOUGH_MONEY, errorOf(sink));
    }

    @Test
    void sellItemWhenAlreadyListedShouldSendNotFound() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        atChilton(world, p);
        Item it = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        world.objectMgr.auctions.add(new org.tbc.world.content.ObjectMgr.Auction(
                50, it.entry, p.guid, 100, 0, 1000, "x", it.guid, 0, 0, p.accountId));
        AuctionHandler.sellItem(sink.session, world, sell(chilton(world).guid, it.guid, 100, 0, 720));
        assertEquals(AuctionHandler.AUCTION_ERR_INVENTORY, errorOf(sink));
        assertTrue(p.items.containsKey((int) it.guid));
    }

    @Test
    void sellItemWhenDurationItemShouldRefuseTrade() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        atChilton(world, p);
        world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD).duration = 60;
        Item it = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        AuctionHandler.sellItem(sink.session, world, sell(chilton(world).guid, it.guid, 100, 0, 720));
        assertEquals(AuctionHandler.EQUIP_ERR_CANNOT_TRADE_THAT, extraOf(sink));
        world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD).duration = 0;
    }

    @Test
    void sellItemWhenTemplateConjuredShouldRefuseTrade() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        atChilton(world, p);
        world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD).flags = AuctionHandler.ITEM_FLAG_CONJURED;
        Item it = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        AuctionHandler.sellItem(sink.session, world, sell(chilton(world).guid, it.guid, 100, 0, 1440));
        assertEquals(AuctionHandler.EQUIP_ERR_CANNOT_TRADE_THAT, extraOf(sink));
        world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD).flags = 0;
    }

    @Test
    void depositCopperWhenNoTemplateShouldBeZero() {
        assertEquals(0, AuctionHandler.depositCopper(null, 1, AuctionHandler.MIN_AUCTION_TIME_SEC));
        assertEquals(1, AuctionHandler.outBid(0));
        assertEquals(5, AuctionHandler.outBid(100));
    }

    @Test
    void placeBidWhenOwnOrTooLowShouldRefuse() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        atChilton(world, p);
        long ah = chilton(world).guid;
        AuctionHandler.placeBid(sink.session, world, new WowBuffer(4));
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 0, 100));
        AuctionHandler.placeBid(sink.session, world, bidBuf(1, 1, 100));
        assertFalse(sink.ops.contains(Opcodes.SMSG_AUCTION_COMMAND_RESULT));

        world.objectMgr.auctions.set(0, new org.tbc.world.content.ObjectMgr.Auction(
                1, Content.ITEM_WORN_SHORTSWORD, p.guid, 100, 0, 1000, "Worn Shortsword", 0, 0, 0, p.accountId));
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 1, 100));
        assertEquals(AuctionHandler.AUCTION_ERR_BID_OWN, errorOf(sink));

        world.objectMgr.auctions.set(0, new org.tbc.world.content.ObjectMgr.Auction(
                1, Content.ITEM_WORN_SHORTSWORD, 88, 100, 0, 1000, "Worn Shortsword", 0, 0, 0, p.accountId));
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 1, 100));
        assertEquals(AuctionHandler.AUCTION_ERR_BID_OWN, errorOf(sink));

        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 77, 100));
        assertEquals(AuctionHandler.AUCTION_ERR_BID_OWN, errorOf(sink));

        world.objectMgr.auctions.set(0, new org.tbc.world.content.ObjectMgr.Auction(
                1, Content.ITEM_WORN_SHORTSWORD, 99, 100, 0, 1000, "Worn Shortsword", 0, 5, 100, 0));
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 1, 100));
        assertEquals(AuctionHandler.AUCTION_ERR_HIGHER_BID, errorOf(sink));

        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 1, 101));
        assertEquals(AuctionHandler.AUCTION_ERR_BID_INCREMENT, errorOf(sink));

        p.setMoney(10);
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 1, 200));
        assertFalse(sink.ops.contains(Opcodes.SMSG_AUCTION_COMMAND_RESULT));

        p.setMoney(1000);
        world.objectMgr.auctions.set(0, new org.tbc.world.content.ObjectMgr.Auction(
                1, Content.ITEM_WORN_SHORTSWORD, 99, 100, 0, 1000, "Worn Shortsword", 0, 0, 0, 0));
        sink.ops.clear();
        sink.last.clear();
        AuctionHandler.placeBid(sink.session, world, bidBuf(ah, 1, 80));
        assertFalse(sink.ops.contains(Opcodes.SMSG_AUCTION_COMMAND_RESULT));
    }

    private static WowBuffer bidBuf(long ah, int id, int price) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(ah);
        b.putU32(id);
        b.putU32(price);
        return b;
    }

    private static int errorOf(Sink sink) {
        return u32(sink.last.get(Opcodes.SMSG_AUCTION_COMMAND_RESULT), 8);
    }

    private static int extraOf(Sink sink) {
        return u32(sink.last.get(Opcodes.SMSG_AUCTION_COMMAND_RESULT), 12);
    }

    private static int u32(byte[] p, int off) {
        return (p[off] & 0xFF)
                | ((p[off + 1] & 0xFF) << 8)
                | ((p[off + 2] & 0xFF) << 16)
                | ((p[off + 3] & 0xFF) << 24);
    }

    private static Item give(World world, Player p, int entry) {
        Item it = new Item(world.nextItemGuid(), entry);
        it.slot = p.firstFreeBagSlot();
        p.items.put((int) it.guid, it);
        p.setMoney(Math.max(p.money, 100));
        return it;
    }

    private static WowBuffer sell(long ah, long item, int bid, int buyout, int minutes) {
        WowBuffer b = new WowBuffer(32);
        b.putU64(ah);
        b.putU64(item);
        b.putU32(bid);
        b.putU32(buyout);
        b.putU32(minutes);
        return b;
    }

    private static void atChilton(World world, Player p) {
        Creature ah = chilton(world);
        p.relocate(ah.x, ah.y, ah.z, ah.o);
    }

    private static Creature chilton(World world) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == Content.NPC_AUCTIONEER_CHILTON) {
                return c;
            }
        }
        throw new AssertionError("no Chilton");
    }

    private static Sink login(World world) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), "Seller", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
