package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;

/** Raid convert and ready check. Layout: spec/03-protocol/packets/group.md */
public final class GroupHandler {
    private GroupHandler() {}

    public static void raidConvert(WorldSession s) {
        Player p = s.player();
        if (p.group == null || p.group.leaderGuid != p.guid || p.group.members.size() < 2) {
            return;
        }
        p.group.raid = true;
        for (Player m : p.group.members) {
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_GROUP_LIST, p.group.listFor(m));
            }
        }
    }

    public static void readyCheck(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() != 0 || p.group == null) {
            return;
        }
        WowBuffer req = new WowBuffer(8);
        req.putU64(p.guid);
        byte[] payload = req.array();
        for (Player m : p.group.members) {
            if (m.session != null) {
                m.session.send(Opcodes.MSG_RAID_READY_CHECK, payload);
            }
        }
    }

    public static void randomRoll(WorldSession s, WowBuffer in) {
        Player p = s.player();
        int min = in.remaining() >= 4 ? in.getU32() : 1;
        int max = in.remaining() >= 4 ? in.getU32() : 100;
        if (max > 10000) {
            max = 10000;
        }
        int roll = min;
        WowBuffer out = new WowBuffer(24);
        out.putU32(min);
        out.putU32(max);
        out.putU32(roll);
        out.putU64(p.guid);
        byte[] payload = out.array();
        if (p.group != null) {
            for (Player m : p.group.members) {
                if (m.session != null) {
                    m.session.send(Opcodes.MSG_RANDOM_ROLL, payload);
                }
            }
        } else {
            s.send(Opcodes.MSG_RANDOM_ROLL, payload);
        }
    }

    public static void minimapPing(WorldSession s, WowBuffer in) {
        Player p = s.player();
        float x = in.remaining() >= 4 ? in.getFloat() : 0;
        float y = in.remaining() >= 4 ? in.getFloat() : 0;
        WowBuffer out = new WowBuffer(16);
        out.putU64(p.guid);
        out.putFloat(x);
        out.putFloat(y);
        byte[] payload = out.array();
        if (p.group != null) {
            for (Player m : p.group.members) {
                if (m.session != null) {
                    m.session.send(Opcodes.MSG_MINIMAP_PING, payload);
                }
            }
        } else {
            s.send(Opcodes.MSG_MINIMAP_PING, payload);
        }
    }
}
