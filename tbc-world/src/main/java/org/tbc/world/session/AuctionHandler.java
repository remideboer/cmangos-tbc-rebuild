package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Mail;
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
    /** AuctionHouse.dbc house 1 (Stormwind) depositPercent / consignmentRate. */
    public static final int DEPOSIT_PERCENT = 5;
    public static final int CUT_PERCENT = 5;
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
        sendList(s, Opcodes.SMSG_AUCTION_LIST_RESULT, hits);
    }

    /** CMSG_AUCTION_LIST_OWNER_ITEMS — owner's rows only. auction.md SMSG_AUCTION_OWNER_LIST_RESULT. */
    public static void listOwnerItems(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        in.getU32();
        Player p = s.player();
        if (auctioneerOf(world, p, guid) == null) {
            return;
        }
        List<ObjectMgr.Auction> hits = new ArrayList<>();
        for (ObjectMgr.Auction a : world.objectMgr.auctions) {
            if (a.owner() == p.guid) {
                hits.add(a);
            }
        }
        sendList(s, Opcodes.SMSG_AUCTION_OWNER_LIST_RESULT, hits);
    }

    /** CMSG_AUCTION_LIST_BIDDER_ITEMS — current bids. auction.md SMSG_AUCTION_BIDDER_LIST_RESULT. */
    public static void listBidderItems(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 16) {
            return;
        }
        long guid = in.getU64();
        in.getU32();
        int outbidCount = in.getU32();
        if (in.remaining() != outbidCount * 4) {
            outbidCount = 0;
        }
        Player p = s.player();
        if (auctioneerOf(world, p, guid) == null) {
            return;
        }
        List<ObjectMgr.Auction> hits = new ArrayList<>();
        for (int i = 0; i < outbidCount && in.remaining() >= 4; i++) {
            int id = in.getU32();
            for (ObjectMgr.Auction a : world.objectMgr.auctions) {
                if (a.id() == id) {
                    hits.add(a);
                    break;
                }
            }
        }
        for (ObjectMgr.Auction a : world.objectMgr.auctions) {
            if (a.bidder() == p.guid) {
                boolean already = false;
                for (ObjectMgr.Auction h : hits) {
                    if (h.id() == a.id()) {
                        already = true;
                        break;
                    }
                }
                if (!already) {
                    hits.add(a);
                }
            }
        }
        sendList(s, Opcodes.SMSG_AUCTION_BIDDER_LIST_RESULT, hits);
    }

    /** CMSG_AUCTION_REMOVE_ITEM — owner cancel. auction.md AUCTION_REMOVED; item + bidder refund by mail. */
    public static void removeItem(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int auctionId = in.getU32();
        Player p = s.player();
        if (auctioneerOf(world, p, guid) == null) {
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
        if (auction == null || auction.owner() != p.guid) {
            commandResult(s, 0, AUCTION_REMOVED, AUCTION_ERR_DATABASE, 0);
            return;
        }
        if (auction.itemGuid() == 0 || auction.itemEntry() == 0) {
            commandResult(s, 0, AUCTION_REMOVED, AUCTION_ERR_INVENTORY, EQUIP_ERR_ITEM_NOT_FOUND);
            return;
        }
        if (auction.currentBid() != 0) {
            int cut = auctionCut(auction.currentBid());
            if (p.money < cut) {
                return;
            }
            if (auction.bidder() != 0) {
                sendCancelledToBidder(world, auction);
            }
            p.setMoney(p.money - cut);
        }
        Item returned = new Item(auction.itemGuid(), auction.itemEntry());
        sendAuctionMail(world, Guid.low(p.guid),
                auction.itemEntry() + ":0:" + Mail.AUCTION_CANCELED, 0, returned);
        commandResult(s, auction.id(), AUCTION_REMOVED, AUCTION_OK, 0);
        list.remove(idx);
    }

    static int auctionCut(int bid) {
        return CUT_PERCENT * bid / 100;
    }

    static void sendCancelledToBidder(World world, ObjectMgr.Auction auction) {
        Player bidder = world.playerByGuid(auction.bidder());
        if (bidder != null && bidder.session != null) {
            WowBuffer data = new WowBuffer(12);
            data.putU32(auction.id());
            data.putU32(auction.itemEntry());
            data.putU32(0);
            bidder.session.send(Opcodes.SMSG_AUCTION_REMOVED_NOTIFICATION, data.array());
        }
        sendAuctionMail(world, Guid.low(auction.bidder()),
                auction.itemEntry() + ":0:" + Mail.AUCTION_CANCELLED_TO_BIDDER, auction.currentBid(), null);
    }

    static void sendAuctionMail(World world, int receiverLow, String subject, int money, Item item) {
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.messageType = Mail.MAIL_AUCTION;
        m.sender = Content.AUCTION_HOUSE_HUMAN;
        m.receiver = receiverLow;
        m.subject = subject;
        m.money = money;
        m.checked = Mail.MAIL_CHECK_MASK_COPIED;
        m.stationery = Mail.MAIL_STATIONERY_AUCTION;
        long now = world.nowMs() / 1000;
        m.deliverTime = now;
        m.expireTime = now + 30L * 24 * 3600;
        if (item != null) {
            m.items.add(item);
        }
        world.characters.storeMail(m);
        Player live = world.playerByGuid(Guid.player(receiverLow));
        if (live != null && live.session != null) {
            WowBuffer z = new WowBuffer(4);
            z.putU32(0);
            live.session.send(Opcodes.SMSG_RECEIVED_MAIL, z.array());
        }
    }

    static void sendList(WorldSession s, int opcode, List<ObjectMgr.Auction> hits) {
        WowBuffer out = new WowBuffer(256);
        out.putU32(hits.size());
        for (ObjectMgr.Auction a : hits) {
            putRow(out, a);
        }
        out.putU32(hits.size());
        out.putU32(Content.AUCTION_LIST_DELAY_MS);
        s.send(opcode, out.array());
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
