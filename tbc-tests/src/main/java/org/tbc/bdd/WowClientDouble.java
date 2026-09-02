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
