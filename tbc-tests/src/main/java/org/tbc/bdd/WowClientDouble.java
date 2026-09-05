package org.tbc.bdd;

import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;

/** In-process 8606 client double. No TCP, no AuthCrypt. */
public final class WowClientDouble implements PacketSink {
    public final List<Integer> opcodes = new ArrayList<>();
    public final List<byte[]> payloads = new ArrayList<>();
    private WorldSession session;

    public WorldSession attach(WorldSession s) {
        this.session = s;
        return s;
    }

    public WorldSession connect(World.Account account) {
        WorldSession s = new WorldSession(this, 0x11111111);
        s.injectAccount(account);
        return attach(s);
    }

    public WorldSession session() {
        return session;
    }

    public void handle(World world, int opcode, byte[] payload) {
        session.handle(world, opcode, payload);
    }

    public void login(World world, long guid) {
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
    }

    public void logout(World world) {
        handle(world, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
    }

    /** TCP close analogue — logout.md SetOffline, not CMSG_LOGOUT_REQUEST. */
    public void disconnect(World world) {
        session.markSocketClosed();
        session.tick(world, 0);
    }

    /** CMSG_PLAYER_LOGOUT — logout.md: intentional no-op (not a LOGOUT_REQUEST substitute). */
    public void playerLogout(World world) {
        handle(world, Opcodes.CMSG_PLAYER_LOGOUT, new byte[0]);
    }

    public void ping(World world, int latency) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(latency);
        handle(world, Opcodes.CMSG_PING, b.array());
    }

    public void gossipHello(World world, long guid) {
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        handle(world, Opcodes.CMSG_GOSSIP_HELLO, g.array());
    }

    public void gossipSelect(World world, long guid, int menuId, int gossipListId) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(guid);
        b.putU32(menuId);
        b.putU32(gossipListId);
        handle(world, Opcodes.CMSG_GOSSIP_SELECT_OPTION, b.array());
    }

    public void binderActivate(World world, long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        handle(world, Opcodes.CMSG_BINDER_ACTIVATE, b.array());
    }

    public void npcTextQuery(World world, int textId, long guid) {
        WowBuffer b = new WowBuffer(12);
        b.putU32(textId);
        b.putU64(guid);
        handle(world, Opcodes.CMSG_NPC_TEXT_QUERY, b.array());
    }

    public void listInventory(World world, long guid) {
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        handle(world, Opcodes.CMSG_LIST_INVENTORY, g.array());
    }

    public void buyItem(World world, long vendor, int itemId, int count) {
        WowBuffer b = new WowBuffer(14);
        b.putU64(vendor);
        b.putU32(itemId);
        b.putU8(count);
        b.putU8(1);
        handle(world, Opcodes.CMSG_BUY_ITEM, b.array());
    }

    public void buyItemInSlot(World world, long vendor, int itemId, long bagGuid, int bagSlot, int count) {
        WowBuffer b = new WowBuffer(22);
        b.putU64(vendor);
        b.putU32(itemId);
        b.putU64(bagGuid);
        b.putU8(bagSlot);
        b.putU8(count);
        handle(world, Opcodes.CMSG_BUY_ITEM_IN_SLOT, b.array());
    }

    public void queryQuest(World world, long guid, int questId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(questId);
        handle(world, Opcodes.CMSG_QUESTGIVER_QUERY_QUEST, b.array());
    }

    public void acceptQuest(World world, long guid, int questId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(questId);
        handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, b.array());
    }

    public void completeQuest(World world, long guid, int questId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(questId);
        handle(world, Opcodes.CMSG_QUESTGIVER_COMPLETE_QUEST, b.array());
    }

    public void setActionButton(World world, int button, int packed) {
        WowBuffer b = new WowBuffer(5);
        b.putU8(button);
        b.putU32(packed);
        handle(world, Opcodes.CMSG_SET_ACTION_BUTTON, b.array());
    }

    public boolean saw(int opcode) {
        return opcodes.contains(opcode);
    }

    public byte[] payload(int opcode) {
        for (int i = opcodes.size() - 1; i >= 0; i--) {
            if (opcodes.get(i) == opcode) {
                return payloads.get(i);
            }
        }
        return new byte[0];
    }

    public void clear() {
        opcodes.clear();
        payloads.clear();
    }

    public static int u32le(byte[] p, int off) {
        return (p[off] & 0xFF)
                | ((p[off + 1] & 0xFF) << 8)
                | ((p[off + 2] & 0xFF) << 16)
                | ((p[off + 3] & 0xFF) << 24);
    }

    public static float floatle(byte[] p, int off) {
        return Float.intBitsToFloat(u32le(p, off));
    }

    public void attackSwing(World world, long guid) {
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        handle(world, Opcodes.CMSG_ATTACKSWING, g.array());
    }

    public void loot(World world, long guid) {
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        handle(world, Opcodes.CMSG_LOOT, g.array());
    }

    public void lootRelease(World world, long guid) {
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        handle(world, Opcodes.CMSG_LOOT_RELEASE, g.array());
    }

    public void autostoreLootItem(World world, int lootSlot) {
        WowBuffer g = new WowBuffer(1);
        g.putU8(lootSlot);
        handle(world, Opcodes.CMSG_AUTOSTORE_LOOT_ITEM, g.array());
    }

    public void lootMoney(World world) {
        handle(world, Opcodes.CMSG_LOOT_MONEY, new byte[0]);
    }

    public void castSpell(World world, int spellId, int castCount, long targetGuid) {
        WowBuffer b = new WowBuffer(32);
        b.putU32(spellId);
        b.putU8(castCount);
        if (targetGuid != 0) {
            b.putU32(org.tbc.world.spell.SpellCastTargets.UNIT);
            b.putPackedGuid(targetGuid);
        } else {
            b.putU32(0);
        }
        handle(world, Opcodes.CMSG_CAST_SPELL, b.array());
    }

    public void groupInvite(World world, String name) {
        WowBuffer b = new WowBuffer(16);
        b.putCString(name);
        handle(world, Opcodes.CMSG_GROUP_INVITE, b.array());
    }

    public void groupAccept(World world) {
        handle(world, Opcodes.CMSG_GROUP_ACCEPT, new byte[0]);
    }

    public void partyChat(World world, String msg) {
        WowBuffer b = new WowBuffer(8 + msg.length());
        b.putU32(0x02);
        b.putU32(0);
        b.putCString(msg);
        handle(world, Opcodes.CMSG_MESSAGECHAT, b.array());
    }

    public void say(World world, String msg) {
        WowBuffer b = new WowBuffer(8 + msg.length());
        b.putU32(1);
        b.putU32(7);
        b.putCString(msg);
        handle(world, Opcodes.CMSG_MESSAGECHAT, b.array());
    }

    public void whisper(World world, String name, String msg) {
        WowBuffer b = new WowBuffer(16 + name.length() + msg.length());
        b.putU32(0x07);
        b.putU32(7);
        b.putCString(name);
        b.putCString(msg);
        handle(world, Opcodes.CMSG_MESSAGECHAT, b.array());
    }

    public void nameQuery(World world, long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        handle(world, Opcodes.CMSG_NAME_QUERY, b.array());
    }

    /** C2S MSG_MOVE_HEARTBEAT: MovementInfo only, no packed GUID. */
    public void heartbeat(World world, float x, float y, float z, float o) {
        WowBuffer b = new WowBuffer(32);
        b.putU32(0);
        b.putU8(0);
        b.putU32(0);
        b.putFloat(x);
        b.putFloat(y);
        b.putFloat(z);
        b.putFloat(o);
        b.putU32(0);
        handle(world, Opcodes.MSG_MOVE_HEARTBEAT, b.array());
    }

    public void initiateTrade(World world, long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        handle(world, Opcodes.CMSG_INITIATE_TRADE, b.array());
    }

    public void beginTrade(World world) {
        handle(world, Opcodes.CMSG_BEGIN_TRADE, new byte[0]);
    }

    public void setTradeItem(World world, int tradeSlot, int bag, int slot) {
        WowBuffer b = new WowBuffer(3);
        b.putU8(tradeSlot);
        b.putU8(bag);
        b.putU8(slot);
        handle(world, Opcodes.CMSG_SET_TRADE_ITEM, b.array());
    }

    public void acceptTrade(World world) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(0);
        handle(world, Opcodes.CMSG_ACCEPT_TRADE, b.array());
    }

    public void who(World world) {
        WowBuffer b = new WowBuffer(32);
        b.putU32(1);
        b.putU32(100);
        b.putCString("");
        b.putCString("");
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        handle(world, Opcodes.CMSG_WHO, b.array());
    }

    public void addFriend(World world, String name) {
        WowBuffer b = new WowBuffer(32);
        b.putCString(name);
        b.putCString("");
        handle(world, Opcodes.CMSG_ADD_FRIEND, b.array());
    }

    public void delFriend(World world, long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        handle(world, Opcodes.CMSG_DEL_FRIEND, b.array());
    }

    public void guildCreate(World world, String name) {
        WowBuffer b = new WowBuffer(16);
        b.putCString(name);
        handle(world, Opcodes.CMSG_GUILD_CREATE, b.array());
    }

    public void guildInvite(World world, String name) {
        WowBuffer b = new WowBuffer(16);
        b.putCString(name);
        handle(world, Opcodes.CMSG_GUILD_INVITE, b.array());
    }

    public void guildAccept(World world) {
        handle(world, Opcodes.CMSG_GUILD_ACCEPT, new byte[0]);
    }

    public void auctionSell(World world, long auctioneer, long itemGuid, int bid, int buyout, int minutes) {
        WowBuffer b = new WowBuffer(32);
        b.putU64(auctioneer);
        b.putU64(itemGuid);
        b.putU32(bid);
        b.putU32(buyout);
        b.putU32(minutes);
        handle(world, Opcodes.CMSG_AUCTION_SELL_ITEM, b.array());
    }

    public void auctionBid(World world, long auctioneer, int auctionId, int price) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(auctioneer);
        b.putU32(auctionId);
        b.putU32(price);
        handle(world, Opcodes.CMSG_AUCTION_PLACE_BID, b.array());
    }

    public void sendMail(World world, long mailbox, String receiver, String subject, String body, long itemGuid) {
        WowBuffer b = new WowBuffer(64);
        b.putU64(mailbox);
        b.putCString(receiver);
        b.putCString(subject);
        b.putCString(body);
        b.putU32(41);
        b.putU32(0);
        if (itemGuid != 0) {
            b.putU8(1);
            b.putU8(0);
            b.putU64(itemGuid);
        } else {
            b.putU8(0);
        }
        b.putU32(0);
        b.putU32(0);
        b.putU64(0);
        b.putU8(0);
        handle(world, Opcodes.CMSG_SEND_MAIL, b.array());
    }

    public void getMailList(World world, long mailbox) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(mailbox);
        handle(world, Opcodes.CMSG_GET_MAIL_LIST, b.array());
    }

    public void takeMailItem(World world, long mailbox, int mailId, int itemLow) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(mailbox);
        b.putU32(mailId);
        b.putU32(itemLow);
        handle(world, Opcodes.CMSG_MAIL_TAKE_ITEM, b.array());
    }

    public void areaTrigger(World world, int trigger) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(trigger);
        handle(world, Opcodes.CMSG_AREATRIGGER, b.array());
    }

    public void worldportAck(World world) {
        handle(world, Opcodes.MSG_MOVE_WORLDPORT_ACK, new byte[0]);
    }

    public void resetInstances(World world) {
        handle(world, Opcodes.CMSG_RESET_INSTANCES, new byte[0]);
    }

    public void battlemasterJoin(World world) {
        WowBuffer b = new WowBuffer(17);
        b.putU64(0);
        b.putU32(2);
        b.putU32(0);
        b.putU8(0);
        handle(world, Opcodes.CMSG_BATTLEMASTER_JOIN, b.array());
    }

    public void learnTalent(World world, int talentId, int rank) {
        WowBuffer b = new WowBuffer(8);
        b.putU32(talentId);
        b.putU32(rank);
        handle(world, Opcodes.CMSG_LEARN_TALENT, b.array());
    }

    public void battlefieldPort(World world, int action) {
        WowBuffer b = new WowBuffer(9);
        b.putU8(0);
        b.putU8(0x0D);
        b.putU32(2);
        b.putU16(0x1F90);
        b.putU8(action);
        handle(world, Opcodes.CMSG_BATTLEFIELD_PORT, b.array());
    }

    public static long u64le(byte[] p, int off) {
        return (u32le(p, off) & 0xFFFFFFFFL) | ((long) u32le(p, off + 4) << 32);
    }

    public static int skipPackedGuid(byte[] p, int off) {
        int mask = p[off] & 0xFF;
        off++;
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                off++;
            }
        }
        return off;
    }

    @Override
    public void send(int opcode, byte[] payload) {
        opcodes.add(opcode);
        payloads.add(payload == null ? new byte[0] : payload);
    }

    @Override
    public void close() {
    }
}
