package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;

/** LFG list query. Layout: spec/03-protocol/packets/lfg.md */
public final class LfgHandler {
    private LfgHandler() {}

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
        list.putCString("");
        list.putU32(0);
        s.send(Opcodes.MSG_LOOKING_FOR_GROUP, list.array());
    }
}
