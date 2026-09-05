package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Mail;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Slice 9: group, trade, who, friends, mail. Layouts from spec packet files. */
public final class SocialHandler {
    public static final float TRADE_DISTANCE = 11.11f;
    public static final int TRADE_SLOTS = 7;
    public static final int TRADE_TRADED = 6;
    public static final int WHO_DISPLAY_CAP = 49;
    public static final int FRIEND_LIMIT = 50;
    public static final int FRIEND_LIST = 0x1;
    public static final int SOCIAL_FLAG_FRIEND = 0x01;
    public static final int MAIL_POSTAGE = 30;
    public static final int MAIL_ITEM_POSTAGE = 30;
    public static final int MAX_MAIL_ITEMS = 12;
    public static final int MAIL_SEND = 0;
    public static final int MAIL_ITEM_TAKEN = 2;
    public static final int MAIL_OK = 0;
    public static final int MAIL_ERR_EQUIP = 1;
    public static final int MAIL_ERR_SELF = 2;
    public static final int MAIL_ERR_MONEY = 3;
    public static final int MAIL_ERR_NOT_FOUND = 4;
    public static final int MAIL_ERR_INTERNAL = 6;
    public static final int MAIL_ERR_TOO_MANY = 18;
    public static final int MAIL_ERR_ATTACH = 19;
    public static final int PARTY_OP_INVITE = 0;
    public static final int ERR_PARTY_OK = 0;
    public static final int ERR_BAD_PLAYER_NAME = 1;
    public static final int ERR_GROUP_FULL = 4;
    public static final int ERR_ALREADY_IN_GROUP = 5;
    public static final int ERR_NOT_LEADER = 7;
    public static final int ERR_WRONG_FACTION = 8;
    public static final int FRIEND_NOT_FOUND = 0x04;
    public static final int FRIEND_ADDED_ONLINE = 0x06;
    public static final int FRIEND_ADDED_OFFLINE = 0x07;
    public static final int FRIEND_ALREADY = 0x08;
    public static final int FRIEND_SELF = 0x09;
    public static final int FRIEND_REMOVED = 0x05;
    public static final int FRIEND_LIST_FULL = 0x01;
    public static final int TRADE_BUSY = 0;
    public static final int TRADE_BEGIN = 1;
    public static final int TRADE_OPEN = 2;
    public static final int TRADE_CANCELED = 3;
    public static final int TRADE_ACCEPT = 4;
    public static final int TRADE_NO_TARGET = 6;
    public static final int TRADE_COMPLETE = 8;
    public static final int TRADE_TOO_FAR = 10;
    public static final int TRADE_WRONG_FACTION = 11;

    private SocialHandler() {}

    public static void contactList(WorldSession s, World world) {
        Player p = s.player();
        if (p == null) {
            s.send(Opcodes.SMSG_CONTACT_LIST, u32(0, 0));
            return;
        }
        WowBuffer b = new WowBuffer(64 + p.friends.size() * 32);
        b.putU32(p.friends.isEmpty() ? 0 : FRIEND_LIST);
        b.putU32(p.friends.size());
        for (Player.Friend f : p.friends) {
            b.putU64(f.guid);
            b.putU32(f.flags);
            b.putCString(f.note == null ? "" : f.note);
            if ((f.flags & SOCIAL_FLAG_FRIEND) != 0) {
                Player o = world.playerByGuid(f.guid);
                int status = o != null && o.session != null ? 1 : 0;
                b.putU8(status);
                if (status != 0) {
                    b.putU32(o.zoneId);
                    b.putU32(o.level);
                    b.putU32(o.clazz);
                }
            }
        }
        s.send(Opcodes.SMSG_CONTACT_LIST, b.array());
    }

    public static void addFriend(WorldSession s, World world, WowBuffer in) {
        String name = in.getCString();
        String note = in.remaining() > 0 ? in.getCString() : "";
        if (note.length() > 200) {
            return;
        }
        Player p = s.player();
        if (p.name.equalsIgnoreCase(name)) {
            friendStatus(s, FRIEND_SELF, p.guid, "", 0, 0, 0, 0);
            return;
        }
        if (p.friends.size() >= FRIEND_LIMIT) {
            friendStatus(s, FRIEND_LIST_FULL, 0, "", 0, 0, 0, 0);
            return;
        }
        Player t = world.playerByName(name);
        if (t == null) {
            t = world.characters.storedByName(name);
        }
        if (t == null) {
            friendStatus(s, FRIEND_NOT_FOUND, 0, "", 0, 0, 0, 0);
            return;
        }
        for (Player.Friend f : p.friends) {
            if (f.guid == t.guid) {
                friendStatus(s, FRIEND_ALREADY, t.guid, note, 0, 0, 0, 0);
                return;
            }
        }
        Player.Friend row = new Player.Friend();
        row.guid = t.guid;
        row.note = note;
        world.characters.addFriend(Guid.low(p.guid), row);
        p.friends.add(row);
        boolean online = t.session != null;
        friendStatus(s, online ? FRIEND_ADDED_ONLINE : FRIEND_ADDED_OFFLINE, t.guid, note,
                online ? 1 : 0, t.zoneId, t.level, t.clazz);
    }

    public static void delFriend(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Player p = s.player();
        p.friends.removeIf(f -> f.guid == guid);
        world.characters.removeFriend(Guid.low(p.guid), guid);
        friendStatus(s, FRIEND_REMOVED, guid, "", 0, 0, 0, 0);
    }

    public static void who(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            emptyWho(s);
            return;
        }
        int levelMin = in.getU32();
        int levelMax = in.getU32();
        if (levelMax == 0 || levelMax == 100) {
            levelMax = 255;
        }
        String name = in.getCString().toLowerCase();
        String guild = in.getCString();
        int raceMask = in.remaining() >= 4 ? in.getU32() : 0;
        int classMask = in.remaining() >= 4 ? in.getU32() : 0;
        int zoneCount = in.remaining() >= 4 ? in.getU32() : 0;
        if (zoneCount > 10) {
            return;
        }
        int[] zones = new int[zoneCount];
        for (int i = 0; i < zoneCount; i++) {
            if (in.remaining() < 4) {
                return;
            }
            zones[i] = in.getU32();
        }
        int strCount = in.remaining() >= 4 ? in.getU32() : 0;
        if (strCount > 4) {
            return;
        }
        String[] extra = new String[strCount];
        for (int i = 0; i < strCount; i++) {
            extra[i] = in.getCString().toLowerCase();
        }
        Player self = s.player();
        java.util.ArrayList<Player> hits = new java.util.ArrayList<>();
        for (Player o : world.playersOnline()) {
            if (o.team != self.team) {
                continue;
            }
            if (o.level < levelMin || o.level > levelMax) {
                continue;
            }
            if (raceMask != 0 && ((1 << o.race) & raceMask) == 0) {
                continue;
            }
            if (classMask != 0 && ((1 << o.clazz) & classMask) == 0) {
                continue;
            }
            if (!name.isEmpty() && !o.name.toLowerCase().contains(name)) {
                continue;
            }
            if (!guild.isEmpty()) {
                continue;
            }
            if (zoneCount > 0) {
                boolean zok = false;
                for (int z : zones) {
                    if (z == o.zoneId) {
                        zok = true;
                        break;
                    }
                }
                if (!zok) {
                    continue;
                }
            }
            if (strCount > 0) {
                boolean sok = false;
                String ln = o.name.toLowerCase();
                for (String e : extra) {
                    if (!e.isEmpty() && ln.contains(e)) {
                        sok = true;
                        break;
                    }
                }
                if (!sok) {
                    continue;
                }
            }
            hits.add(o);
        }
        int match = hits.size();
        int display = Math.min(WHO_DISPLAY_CAP, match);
        WowBuffer out = new WowBuffer(16 + display * 32);
        out.putU32(display);
        out.putU32(match);
        for (int i = 0; i < display; i++) {
            Player o = hits.get(i);
            out.putCString(o.name);
            out.putCString("");
            out.putU32(o.level);
            out.putU32(o.clazz);
            out.putU32(o.race);
            out.putU8(o.gender);
            out.putU32(o.zoneId);
        }
        s.send(Opcodes.SMSG_WHO, out.array());
    }

    public static void groupInvite(WorldSession s, World world, WowBuffer in) {
        String name = in.getCString();
        Player t = world.playerByName(name);
        if (t == null || t.session == null) {
            partyResult(s, PARTY_OP_INVITE, name, ERR_BAD_PLAYER_NAME);
            return;
        }
        Player p = s.player();
        if (t.group != null) {
            partyResult(s, PARTY_OP_INVITE, name, ERR_ALREADY_IN_GROUP);
            return;
        }
        if (p.group != null && p.group.leaderGuid != p.guid) {
            partyResult(s, PARTY_OP_INVITE, name, ERR_NOT_LEADER);
            return;
        }
        if (p.group != null && p.group.members.size() >= Group.MAX_PARTY) {
            partyResult(s, PARTY_OP_INVITE, name, ERR_GROUP_FULL);
            return;
        }
        if (p.team != t.team) {
            partyResult(s, PARTY_OP_INVITE, name, ERR_WRONG_FACTION);
            return;
        }
        t.session.pendingInviteFrom = p;
        WowBuffer inv = new WowBuffer(16);
        inv.putCString(p.name);
        t.session.send(Opcodes.SMSG_GROUP_INVITE, inv.array());
        partyResult(s, PARTY_OP_INVITE, name, ERR_PARTY_OK);
    }

    public static void groupAccept(WorldSession s, World world) {
        Player from = s.pendingInviteFrom;
        s.pendingInviteFrom = null;
        if (from == null || from.session == null) {
            return;
        }
        Player p = s.player();
        if (from.group == null) {
            from.group = new Group();
            from.group.leaderGuid = from.guid;
            from.group.members.add(from);
        }
        if (from.group.members.size() >= Group.MAX_PARTY) {
            partyResult(from.session, PARTY_OP_INVITE, p.name, ERR_GROUP_FULL);
            return;
        }
        from.group.members.add(p);
        p.group = from.group;
        sendGroupList(from.group);
    }

    public static void groupDisband(WorldSession s) {
        Player p = s.player();
        if (p.group == null) {
            return;
        }
        Group g = p.group;
        for (Player m : new java.util.ArrayList<>(g.members)) {
            m.group = null;
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_GROUP_DESTROYED, new byte[0]);
                m.session.send(Opcodes.SMSG_GROUP_LIST, Group.emptyList());
            }
        }
        g.members.clear();
    }

    public static void initiateTrade(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            tradeStatus(s, TRADE_NO_TARGET, 0, 0);
            return;
        }
        long guid = in.getU64();
        Player p = s.player();
        if (p.trade != null) {
            tradeStatus(s, TRADE_BUSY, 0, 0);
            return;
        }
        Player t = world.playerByGuid(guid);
        if (t == null || t.session == null) {
            tradeStatus(s, TRADE_NO_TARGET, 0, 0);
            return;
        }
        if (p.team != t.team) {
            tradeStatus(s, TRADE_WRONG_FACTION, 0, 0);
            return;
        }
        if (p.distance2d(t) > TRADE_DISTANCE) {
            tradeStatus(s, TRADE_TOO_FAR, 0, 0);
            return;
        }
        if (t.trade != null) {
            tradeStatus(s, TRADE_BUSY, 0, 0);
            return;
        }
        p.trade = new Player.TradeData();
        p.trade.partner = t;
        t.trade = new Player.TradeData();
        t.trade.partner = p;
        tradeStatus(t.session, TRADE_BEGIN, p.guid, 0);
    }

    public static void beginTrade(WorldSession s) {
        Player p = s.player();
        if (p.trade == null || p.trade.partner == null || p.trade.partner.session == null) {
            return;
        }
        tradeStatus(s, TRADE_OPEN, 0, 0);
        tradeStatus(p.trade.partner.session, TRADE_OPEN, 0, 0);
    }

    public static void setTradeItem(WorldSession s, WowBuffer in) {
        if (in.remaining() < 3) {
            return;
        }
        int tradeSlot = in.getU8();
        int bag = in.getU8();
        int slot = in.getU8();
        Player p = s.player();
        if (p.trade == null || tradeSlot >= TRADE_SLOTS) {
            return;
        }
        Item it = p.itemAt(bag, slot);
        if (it == null) {
            cancelTrade(s);
            return;
        }
        p.trade.slots[tradeSlot] = it;
        p.trade.accepted = false;
        if (p.trade.partner != null) {
            p.trade.partner.trade.accepted = false;
        }
    }

    public static void acceptTrade(WorldSession s, WowBuffer in) {
        if (in.remaining() >= 4) {
            in.getU32();
        }
        Player p = s.player();
        if (p.trade == null || p.trade.partner == null) {
            return;
        }
        Player o = p.trade.partner;
        p.trade.accepted = true;
        if (!o.trade.accepted) {
            tradeStatus(o.session, TRADE_ACCEPT, 0, 0);
            return;
        }
        if (p.distance2d(o) > TRADE_DISTANCE) {
            tradeStatus(s, TRADE_TOO_FAR, 0, 0);
            tradeStatus(o.session, TRADE_TOO_FAR, 0, 0);
            clearTrade(p);
            clearTrade(o);
            return;
        }
        if (!swapTraded(p, o)) {
            return;
        }
        tradeStatus(s, TRADE_COMPLETE, 0, 0);
        tradeStatus(o.session, TRADE_COMPLETE, 0, 0);
        clearTrade(p);
        clearTrade(o);
    }

    public static void cancelTrade(WorldSession s) {
        Player p = s.player();
        if (p.trade == null) {
            return;
        }
        Player o = p.trade.partner;
        tradeStatus(s, TRADE_CANCELED, 0, 0);
        if (o != null && o.session != null) {
            tradeStatus(o.session, TRADE_CANCELED, 0, 0);
            clearTrade(o);
        }
        clearTrade(p);
    }

    public static void sendMail(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            mailResult(s, 0, MAIL_SEND, MAIL_ERR_INTERNAL, 0, 0);
            return;
        }
        in.getU64();
        String receiver = in.getCString();
        String subject = in.getCString();
        String body = in.getCString();
        if (in.remaining() >= 8) {
            in.getU32();
            in.getU32();
        }
        int itemCount = in.remaining() > 0 ? in.getU8() : 0;
        if (itemCount > MAX_MAIL_ITEMS) {
            mailResult(s, 0, MAIL_SEND, MAIL_ERR_TOO_MANY, 0, 0);
            return;
        }
        java.util.ArrayList<Item> attached = new java.util.ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            if (in.remaining() < 9) {
                mailResult(s, 0, MAIL_SEND, MAIL_ERR_ATTACH, 0, 0);
                return;
            }
            in.getU8();
            long itemGuid = in.getU64();
            Item it = s.player().items.get(Guid.low(itemGuid));
            if (it == null) {
                mailResult(s, 0, MAIL_SEND, MAIL_ERR_ATTACH, 0, 0);
                return;
            }
            attached.add(it);
        }
        int money = in.remaining() >= 4 ? in.getU32() : 0;
        int cod = in.remaining() >= 4 ? in.getU32() : 0;
        Player p = s.player();
        if (p.name.equalsIgnoreCase(receiver)) {
            mailResult(s, 0, MAIL_SEND, MAIL_ERR_SELF, 0, 0);
            return;
        }
        Player dest = world.playerByName(receiver);
        if (dest == null) {
            dest = world.characters.storedByName(receiver);
        }
        if (dest == null) {
            mailResult(s, 0, MAIL_SEND, MAIL_ERR_NOT_FOUND, 0, 0);
            return;
        }
        int postage = MAIL_POSTAGE + MAIL_ITEM_POSTAGE * attached.size();
        if (p.money < postage + money) {
            mailResult(s, 0, MAIL_SEND, MAIL_ERR_MONEY, 0, 0);
            return;
        }
        p.setMoney(p.money - postage - money);
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.sender = Guid.low(p.guid);
        m.receiver = Guid.low(dest.guid);
        m.subject = subject;
        m.body = body;
        m.money = money;
        m.cod = cod;
        m.checked = body.isEmpty() ? 0 : 0x10;
        long now = world.nowMs() / 1000;
        m.deliverTime = now;
        m.expireTime = now + 30L * 24 * 3600;
        for (Item it : attached) {
            p.items.remove(Guid.low(it.guid));
            m.items.add(it);
        }
        world.characters.storeMail(m);
        mailResult(s, m.id, MAIL_SEND, MAIL_OK, 0, 0);
        Player live = world.playerByGuid(dest.guid);
        if (live != null && live.session != null) {
            live.session.send(Opcodes.SMSG_RECEIVED_MAIL, u32(0));
        }
    }

    public static void getMailList(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() >= 8) {
            in.getU64();
        }
        long now = world.nowMs() / 1000;
        java.util.List<Mail> inbox = world.characters.inbox(Guid.low(s.player().guid), now);
        int n = Math.min(50, inbox.size());
        WowBuffer out = new WowBuffer(8);
        out.putU8(n);
        for (int i = 0; i < n; i++) {
            Mail m = inbox.get(i);
            WowBuffer row = new WowBuffer(128);
            row.putU32(m.id);
            row.putU8(0);
            row.putU64(m.sender);
            row.putU32(m.cod);
            row.putU32(0);
            row.putU32(0);
            row.putU32(m.stationery);
            row.putU32(m.money);
            row.putU32(m.checked);
            row.putFloat(Math.max(0f, (m.expireTime - now) / 86400f));
            row.putU32(0);
            row.putCString(m.subject);
            row.putU8(m.items.size());
            for (int k = 0; k < m.items.size(); k++) {
                Item it = m.items.get(k);
                row.putU8(k);
                row.putU32(Guid.low(it.guid));
                row.putU32(it.entry);
                for (int e = 0; e < 18; e++) {
                    row.putU32(0);
                }
                row.putU32(0);
                row.putU32(0);
                row.putU8(it.count);
                row.putU32(0);
                row.putU32(it.durability);
                row.putU32(it.durability);
            }
            byte[] body = row.array();
            out.putU16(body.length);
            out.putBytes(body);
        }
        s.send(Opcodes.SMSG_MAIL_LIST_RESULT, out.array());
    }

    public static void takeMailItem(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 16) {
            mailResult(s, 0, MAIL_ITEM_TAKEN, MAIL_ERR_INTERNAL, 0, 0);
            return;
        }
        in.getU64();
        int mailId = in.getU32();
        int itemLow = in.getU32();
        Mail m = world.characters.mail(mailId);
        if (m == null || m.receiver != Guid.low(s.player().guid)) {
            mailResult(s, mailId, MAIL_ITEM_TAKEN, MAIL_ERR_INTERNAL, 0, 0);
            return;
        }
        Player p = s.player();
        int bagSlot = p.firstFreeBagSlot();
        if (bagSlot < 0) {
            mailResult(s, mailId, MAIL_ITEM_TAKEN, MAIL_ERR_EQUIP, 0, 0);
            return;
        }
        Item taken = null;
        for (int i = 0; i < m.items.size(); i++) {
            if (Guid.low(m.items.get(i).guid) == itemLow) {
                taken = m.items.remove(i);
                break;
            }
        }
        if (taken == null) {
            mailResult(s, mailId, MAIL_ITEM_TAKEN, MAIL_ERR_INTERNAL, 0, 0);
            return;
        }
        taken.ownerGuid = Guid.low(p.guid);
        taken.bag = 0;
        taken.slot = bagSlot;
        p.items.put(Guid.low(taken.guid), taken);
        world.characters.storeMail(m);
        mailResult(s, mailId, MAIL_ITEM_TAKEN, MAIL_OK, itemLow, taken.count);
    }

    private static boolean swapTraded(Player a, Player b) {
        Item[] fromA = new Item[TRADE_TRADED];
        Item[] fromB = new Item[TRADE_TRADED];
        int nA = 0;
        int nB = 0;
        for (int i = 0; i < TRADE_TRADED; i++) {
            if (a.trade.slots[i] != null) {
                fromA[nA++] = a.trade.slots[i];
            }
            if (b.trade.slots[i] != null) {
                fromB[nB++] = b.trade.slots[i];
            }
        }
        int freeA = countFree(a) + nA;
        int freeB = countFree(b) + nB;
        if (freeA < nB || freeB < nA) {
            return false;
        }
        for (int i = 0; i < nA; i++) {
            a.items.remove(Guid.low(fromA[i].guid));
        }
        for (int i = 0; i < nB; i++) {
            b.items.remove(Guid.low(fromB[i].guid));
        }
        for (int i = 0; i < nA; i++) {
            give(b, fromA[i]);
        }
        for (int i = 0; i < nB; i++) {
            give(a, fromB[i]);
        }
        int goldA = a.trade.gold;
        int goldB = b.trade.gold;
        a.setMoney(a.money - goldA + goldB);
        b.setMoney(b.money - goldB + goldA);
        return true;
    }

    private static int countFree(Player p) {
        int n = 0;
        for (int s = Player.INVENTORY_SLOT_ITEM_START; s < Player.INVENTORY_SLOT_ITEM_END; s++) {
            if (p.itemAt(0, s) == null) {
                n++;
            }
        }
        return n;
    }

    private static void give(Player p, Item it) {
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = p.firstFreeBagSlot();
        p.items.put(Guid.low(it.guid), it);
    }

    private static void clearTrade(Player p) {
        p.trade = null;
    }

    private static void sendGroupList(Group g) {
        for (Player m : g.members) {
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_GROUP_LIST, g.listFor(m));
            }
        }
    }

    private static void partyResult(WorldSession s, int op, String name, int result) {
        WowBuffer b = new WowBuffer(16);
        b.putU32(op);
        b.putCString(name == null ? "" : name);
        b.putU32(result);
        s.send(Opcodes.SMSG_PARTY_COMMAND_RESULT, b.array());
    }

    private static void tradeStatus(WorldSession s, int status, long guid, int extra) {
        WowBuffer b = new WowBuffer(16);
        b.putU32(status);
        if (status == TRADE_BEGIN) {
            b.putU64(guid);
        } else if (status == TRADE_OPEN) {
            b.putU32(extra);
        }
        s.send(Opcodes.SMSG_TRADE_STATUS, b.array());
    }

    private static void friendStatus(WorldSession s, int result, long guid, String note,
                                     int status, int area, int level, int clazz) {
        WowBuffer b = new WowBuffer(32);
        b.putU8(result);
        b.putU64(guid);
        if (result == FRIEND_ADDED_OFFLINE || result == FRIEND_ADDED_ONLINE) {
            b.putCString(note == null ? "" : note);
        }
        if (result == FRIEND_ADDED_ONLINE) {
            b.putU8(status);
            b.putU32(area);
            b.putU32(level);
            b.putU32(clazz);
        }
        s.send(Opcodes.SMSG_FRIEND_STATUS, b.array());
    }

    private static void mailResult(WorldSession s, int mailId, int action, int error, int itemLow, int count) {
        WowBuffer b = new WowBuffer(20);
        b.putU32(mailId);
        b.putU32(action);
        b.putU32(error);
        if (action == MAIL_ITEM_TAKEN && error == MAIL_OK) {
            b.putU32(itemLow);
            b.putU32(count);
        }
        s.send(Opcodes.SMSG_SEND_MAIL_RESULT, b.array());
    }

    private static void emptyWho(WorldSession s) {
        WowBuffer out = new WowBuffer(8);
        out.putU32(0);
        out.putU32(0);
        s.send(Opcodes.SMSG_WHO, out.array());
    }

    private static byte[] u32(int... v) {
        WowBuffer b = new WowBuffer(v.length * 4);
        for (int x : v) {
            b.putU32(x);
        }
        return b.array();
    }
}
