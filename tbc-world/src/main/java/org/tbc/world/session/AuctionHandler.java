package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Auction hello / sell / search list. Layout: spec/03-protocol/packets/auction.md */
public final class AuctionHandler {
    public static final int AUCTION_STARTED = 0;
    public static final int AUCTION_REMOVED = 1;
    public static final int AUCTION_BID_PLACED = 2;
    public static final int AUCTION_OK = 0;
    public static final int AUCTION_ERR_INVENTORY = 1;
    public static final int AUCTION_ERR_DATABASE = 2;
    public static final int AUCTION_ERR_NOT_ENOUGH_MONEY = 3;
    public static final int AUCTION_ERR_ITEM_NOT_FOUND = 4;
    public static final int AUCTION_ERR_HIGHER_BID = 5;
    public static final int AUCTION_ERR_BID_INCREMENT = 7;
    public static final int AUCTION_ERR_BID_OWN = 10;
    /** Player.h MAX_MONEY_AMOUNT. */
    public static final int MAX_MONEY_AMOUNT = 0x7FFFFFFF - 1;
    /** AuctionHouseMgr.h MIN_AUCTION_TIME = 12 hours, in seconds. */
    public static final int MIN_AUCTION_TIME_SEC = 12 * 3600;
    /** AuctionHouse.dbc house 1 (Stormwind) depositPercent. */
    public static final int DEPOSIT_PERCENT = 5;
    public static final int ITEM_FLAG_CONJURED = 0x00000002;
    public static final int EQUIP_ERR_ITEM_NOT_FOUND = 23;
    public static final int EQUIP_ERR_CANNOT_TRADE_THAT = 79;

    private AuctionHandler() {}

    public static void sendHello(Creature c, BiConsumer<Integer, byte[]> send) {
        WowBuffer out = new WowBuffer(12);
        out.putU64(c.guid);
        out.putU32(Content.AUCTION_HOUSE_HUMAN);
        send.accept(Opcodes.MSG_AUCTION_HELLO, out.array());
    }

    public static void sellItem(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 28) {
            return;
        }
        long auctioneer = in.getU64();
        long itemGuid = in.getU64();
        int bid = in.getU32();
        int buyout = in.getU32();
        int minutes = in.getU32();
        if (bid == 0 || minutes == 0) {
            return;
        }
        Player p = s.player();
        if (auctioneerOf(world, p, auctioneer) == null) {
            return;
        }
        int etimeSec = minutes * 60;
        if (etimeSec != MIN_AUCTION_TIME_SEC
                && etimeSec != 2 * MIN_AUCTION_TIME_SEC
                && etimeSec != 4 * MIN_AUCTION_TIME_SEC) {
            return;
        }
        if (itemGuid == 0) {
            return;
        }
        if (bid > MAX_MONEY_AMOUNT || buyout > MAX_MONEY_AMOUNT) {
            commandResult(s, 0, AUCTION_STARTED, AUCTION_ERR_DATABASE, 0);
            return;
        }
        for (ObjectMgr.Auction a : world.objectMgr.auctions) {
            if (a.itemGuid() == itemGuid) {
                commandResult(s, 0, AUCTION_STARTED, AUCTION_ERR_INVENTORY, EQUIP_ERR_ITEM_NOT_FOUND);
                return;
            }
        }
        Item it = p.items.get(Guid.low(itemGuid));
        if (it == null) {
            commandResult(s, 0, AUCTION_STARTED, AUCTION_ERR_INVENTORY, EQUIP_ERR_ITEM_NOT_FOUND);
            return;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(it.entry);
        if (it.soulbound || (it.flags & ITEM_FLAG_CONJURED) != 0
                || (t != null && ((t.flags & ITEM_FLAG_CONJURED) != 0 || t.duration != 0))) {
            commandResult(s, 0, AUCTION_STARTED, AUCTION_ERR_INVENTORY, EQUIP_ERR_CANNOT_TRADE_THAT);
            return;
        }
        int deposit = depositCopper(t, it.count, etimeSec);
        if (p.money < deposit) {
            commandResult(s, 0, AUCTION_STARTED, AUCTION_ERR_NOT_ENOUGH_MONEY, 0);
            return;
        }
        p.setMoney(p.money - deposit);
        p.items.remove(Guid.low(it.guid));
        int id = world.objectMgr.nextAuctionId.getAndIncrement();
        String name = t != null && t.name != null ? t.name : "";
        world.objectMgr.auctions.add(new ObjectMgr.Auction(
                id, it.entry, p.guid, bid, buyout, etimeSec * 1000, name,
                it.guid, 0, 0, p.accountId));
        commandResult(s, id, AUCTION_STARTED, AUCTION_OK, 0);
    }

    public static void placeBid(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 16) {
            return;
        }
        long auctioneer = in.getU64();
        int auctionId = in.getU32();
        int price = in.getU32();
        if (auctionId == 0 || price == 0) {
            return;
        }
        Player p = s.player();
        if (auctioneerOf(world, p, auctioneer) == null) {
            return;
        }
        List<ObjectMgr.Auction> list = world.objectMgr.auctions;
        int idx = -1;
        ObjectMgr.Auction auction = null;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id() == auctionId) {
                idx = i;
                auction = list.get(i);
                break;
            }
        }
        if (auction == null || auction.owner() == p.guid
                || (auction.ownerAccount() != 0 && auction.ownerAccount() == p.accountId)) {
            commandResult(s, 0, AUCTION_BID_PLACED, AUCTION_ERR_BID_OWN, 0);
            return;
        }
        if (price <= auction.currentBid()) {
            WowBuffer data = new WowBuffer(32);
            data.putU32(auction.id());
            data.putU32(AUCTION_BID_PLACED);
            data.putU32(AUCTION_ERR_HIGHER_BID);
            data.putU64(auction.bidder());
            data.putU32(auction.currentBid());
            data.putU32(outBid(auction.currentBid()));
            s.send(Opcodes.SMSG_AUCTION_COMMAND_RESULT, data.array());
            return;
        }
        int minNext = auction.currentBid() + outBid(auction.currentBid());
        if ((price < auction.buyout() || auction.buyout() == 0) && price < minNext) {
            commandResult(s, auction.id(), AUCTION_BID_PLACED, AUCTION_ERR_BID_INCREMENT, 0);
            return;
        }
        if (price > p.money) {
            return;
        }
        if (price < auction.startBid()) {
            return;
        }
        commandResult(s, auction.id(), AUCTION_BID_PLACED, AUCTION_OK, outBid(auction.currentBid()));
        int paid = auction.buyout() != 0 && price > auction.buyout() ? auction.buyout() : price;
        p.setMoney(p.money - paid);
        list.set(idx, new ObjectMgr.Auction(
                auction.id(), auction.itemEntry(), auction.owner(), auction.startBid(), auction.buyout(),
                auction.timeLeftMs(), auction.name(), auction.itemGuid(), p.guid, paid, auction.ownerAccount()));
    }

    public static void listItems(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        if (in.remaining() >= 4) {
            in.getU32();
        }
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (auctioneerOf(world, p, guid) == null) {
            return;
        }
        String q = name == null ? "" : name.toLowerCase();
        List<ObjectMgr.Auction> hits = new ArrayList<>();
        for (ObjectMgr.Auction a : world.objectMgr.auctions) {
            if (!q.isEmpty() && (a.name() == null || !a.name().toLowerCase().contains(q))) {
                continue;
            }
            hits.add(a);
        }
        WowBuffer out = new WowBuffer(256);
        out.putU32(hits.size());
        for (ObjectMgr.Auction a : hits) {
            putRow(out, a);
        }
        out.putU32(hits.size());
        out.putU32(Content.AUCTION_LIST_DELAY_MS);
        s.send(Opcodes.SMSG_AUCTION_LIST_RESULT, out.array());
    }

    /** CMaNGOS AuctionHouseObject::Update. world-loop.md WUPDATE_AUCTIONS. */
    public static void expire(World world) {
        if (world == null || world.objectMgr == null) {
            return;
        }
        world.objectMgr.auctions.removeIf(a -> a.timeLeftMs() <= 0);
    }

    static Creature auctioneerOf(World world, Player p, long guid) {
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_AUCTIONEER) == 0) {
            return null;
        }
        return npc;
    }

    static int depositCopper(ObjectMgr.ItemTemplate t, int count, int etimeSec) {
        int sell = t == null ? 0 : t.sellPrice;
        float deposit = (float) sell * count * (etimeSec / (float) MIN_AUCTION_TIME_SEC);
        deposit = deposit * DEPOSIT_PERCENT * 3.0f / 100.0f;
        if (deposit < 0) {
            deposit = 0;
        }
        return (int) deposit;
    }

    static int outBid(int bid) {
        int outbid = (bid / 100) * 5;
        if (outbid == 0) {
            outbid = 1;
        }
        return outbid;
    }

    static void commandResult(WorldSession s, int auctionId, int action, int error, int extra) {
        WowBuffer data = new WowBuffer(24);
        data.putU32(auctionId);
        data.putU32(action);
        data.putU32(error);
        if (error == AUCTION_OK && action == AUCTION_BID_PLACED) {
            data.putU32(extra);
        } else if (error == AUCTION_ERR_INVENTORY) {
            data.putU32(extra);
        }
        s.send(Opcodes.SMSG_AUCTION_COMMAND_RESULT, data.array());
    }

    static void putRow(WowBuffer out, ObjectMgr.Auction a) {
        out.putU32(a.id());
        out.putU32(a.itemEntry());
        for (int i = 0; i < 6; i++) {
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
        }
        out.putU32(0);
        out.putU32(0);
        out.putU32(1);
        out.putU32(0);
        out.putU32(0);
        out.putU64(a.owner());
        out.putU32(a.startBid());
        out.putU32(a.currentBid() == 0 ? 0 : outBid(a.currentBid()));
        out.putU32(a.buyout());
        out.putU32(a.timeLeftMs());
        out.putU64(a.bidder());
        out.putU32(a.currentBid());
    }
}
