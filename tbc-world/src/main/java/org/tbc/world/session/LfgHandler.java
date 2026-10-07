package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;

/** LFG list query. Layout: spec/03-protocol/packets/lfg.md */
public final class LfgHandler {
    private LfgHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_SET_LOOKING_FOR_GROUP, (s, w, in) -> setLooking(s))
                .register(Opcodes.MSG_LOOKING_FOR_GROUP, (s, w, in) -> list(s, in))
                .register(Opcodes.CMSG_LFG_SET_AUTOJOIN, (s, w, in) -> setAutoJoin(s))
                .register(Opcodes.CMSG_LFG_CLEAR_AUTOJOIN, (s, w, in) -> clearAutoJoin(s))
                .register(Opcodes.CMSG_SET_LFG_COMMENT, (s, w, in) -> setComment(s, in))
                .register(Opcodes.CMSG_CLEAR_LOOKING_FOR_GROUP, (s, w, in) -> clearLookingForGroup(s))
                .register(Opcodes.CMSG_CLEAR_LOOKING_FOR_MORE, (s, w, in) -> clearLookingForMore(s))
                .register(Opcodes.CMSG_SET_LOOKING_FOR_MORE, (s, w, in) -> setLookingForMore(s, in))
                .register(Opcodes.CMSG_ACCEPT_LFG_MATCH, (s, w, in) -> acceptMatch(s));
    }

    public static void setLooking(WorldSession s) {
        s.player().looking = true;
    }

    public static void list(WorldSession s, WowBuffer in) {
        Player p = s.player();
        int type = in.remaining() >= 4 ? in.getU32() : 0;
        int entry = in.remaining() >= 4 ? in.getU32() : 0;
        WowBuffer list = new WowBuffer(64);
        list.putU32(type);
        list.putU32(entry);
        list.putU32(1);
        list.putU32(1);
        list.putPackedGuid(p.guid);
        list.putU32(p.level);
        list.putU32(p.zoneId);
        list.putU8(0);
        list.putU32(0);
        list.putU32(0);
        list.putU32(0);
        list.putCString(p.lfgComment != null ? p.lfgComment : "");
        list.putU32(0);
        s.send(Opcodes.MSG_LOOKING_FOR_GROUP, list.array());
    }

    /** LFGHandler::HandleLfgSetAutoJoinOpcode — SMSG_MEETINGSTONE_JOINFAILED FAIL_NONE 0. */
    public static void setAutoJoin(WorldSession s) {
        s.player().lfgAutoJoin = true;
        WowBuffer fail = new WowBuffer(1);
        fail.putU8(0);
        s.send(Opcodes.SMSG_MEETINGSTONE_JOINFAILED, fail.array());
    }

    /** LFGHandler::HandleLfgClearAutoJoinOpcode — no SMSG. */
    public static void clearAutoJoin(WorldSession s) {
        s.player().lfgAutoJoin = false;
    }

    /** LFGHandler::HandleSetLfgCommentOpcode — string stored for list rows. */
    public static void setComment(WorldSession s, WowBuffer in) {
        s.player().lfgComment = in.remaining() > 0 ? in.getCString() : "";
    }

    /** CMSG_CLEAR_LOOKING_FOR_GROUP — StopLookingForGroup → SMSG_LFG_UPDATE queued=0. */
    public static void clearLookingForGroup(WorldSession s) {
        s.player().looking = false;
        sendUpdate(s, false, false, false, 0);
    }

    /** CMSG_CLEAR_LOOKING_FOR_MORE — StopLookingForMore → SMSG_LFG_UPDATE. */
    public static void clearLookingForMore(WorldSession s) {
        s.player().looking = false;
        sendUpdate(s, false, false, false, 0);
    }

    /**
     * CMSG_SET_LOOKING_FOR_MORE — StartLookingForMore.
     * Solo: looking + SMSG_LFG_UPDATE queued/lfg/lfm with more packed word.
     */
    public static void setLookingForMore(WorldSession s, WowBuffer in) {
        int data = in.remaining() >= 4 ? in.getU32() : 0;
        s.player().looking = true;
        sendUpdate(s, true, false, true, data);
    }

    private static void sendUpdate(WorldSession s, boolean queued, boolean lfg, boolean lfm, int more) {
        WowBuffer u = new WowBuffer(8);
        u.putU8(queued ? 1 : 0);
        u.putU8(lfg ? 1 : 0);
        u.putU8(lfm ? 1 : 0);
        if (lfm) {
            u.putU32(more);
        }
        s.send(Opcodes.SMSG_LFG_UPDATE, u.array());
    }

    public static void acceptMatch(WorldSession s) {
        WowBuffer u = new WowBuffer(8);
        u.putU8(1);
        u.putU8(1);
        u.putU8(0);
        s.send(Opcodes.SMSG_LFG_UPDATE, u.array());
    }
}
