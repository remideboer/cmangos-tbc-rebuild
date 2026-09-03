package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Channels and text emote. Layout: spec/03-protocol/packets/chat.md */
public final class ChannelHandler {
    public static final int YOU_JOINED = 0x02;
    public static final int CHANNEL_ID_GENERAL = 1;

    private ChannelHandler() {}

    public static void join(WorldSession s, WowBuffer in) {
        if (in.remaining() >= 4) {
            in.getU32();
        }
        if (in.remaining() > 0) {
            in.getU8();
        }
        if (in.remaining() > 0) {
            in.getU8();
        }
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (in.remaining() > 0) {
            in.getCString();
        }
        s.channels.add(name);
        WowBuffer n = new WowBuffer(32);
        n.putU8(YOU_JOINED);
        n.putCString(name);
        n.putU8(0);
        n.putU32(CHANNEL_ID_GENERAL);
        n.putU32(0);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
        sendList(s, name);
    }

    public static void list(WorldSession s, WowBuffer in) {
        String name = in.remaining() > 0 ? in.getCString() : "";
        sendList(s, name);
    }

    public static void textEmote(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        int emote = in.remaining() >= 4 ? in.getU32() : 0;
        int num = in.remaining() >= 4 ? in.getU32() : 0;
        long target = in.remaining() >= 8 ? in.getU64() : 0;
        WowBuffer out = new WowBuffer(32);
        out.putU64(p.guid);
        out.putU32(emote);
        out.putU32(num);
        out.putU32(1);
        out.putU8(0);
        byte[] payload = out.array();
        s.send(Opcodes.SMSG_TEXT_EMOTE, payload);
        Player other = world.playerByGuid(target);
        if (other != null && other.session != null) {
            other.session.send(Opcodes.SMSG_TEXT_EMOTE, payload);
        }
    }

    private static void sendList(WorldSession s, String name) {
        Player p = s.player();
        WowBuffer list = new WowBuffer(32);
        list.putU8(0);
        list.putCString(name);
        list.putU8(0);
        list.putU32(1);
        list.putU64(p.guid);
        list.putU8(0);
        s.send(Opcodes.SMSG_CHANNEL_LIST, list.array());
    }
}
