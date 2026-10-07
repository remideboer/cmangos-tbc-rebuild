package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Instance reset + dungeon difficulty (MiscHandler.cpp, instance.md). */
public final class InstanceHandler {
    private InstanceHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_RESET_INSTANCES, (s, w, in) -> resetInstances(s))
                .register(Opcodes.MSG_SET_DUNGEON_DIFFICULTY, (s, w, in) -> setDungeonDifficulty(s, in));
    }

    /** MiscHandler::HandleResetInstancesOpcode — fail while a member is inside the bound instance. */
    public static void resetInstances(WorldSession s) {
        Player p = s.player();
        Group g = p.group;
        int map = g != null ? g.bindMap : p.bindMap;
        boolean inside = false;
        if (g != null) {
            for (Player m : g.members) {
                if (m.mapId == g.bindMap && m.instanceId == g.instanceId && g.instanceId != 0) {
                    inside = true;
                }
            }
        }
        if (inside) {
            WowBuffer fail = new WowBuffer(8);
            fail.putU32(0);
            fail.putU32(map);
            s.send(Opcodes.SMSG_INSTANCE_RESET_FAILED, fail.array());
        } else {
            WowBuffer ok = new WowBuffer(4);
            ok.putU32(map == 0 ? 389 : map);
            s.send(Opcodes.SMSG_INSTANCE_RESET, ok.array());
            if (g != null) {
                g.instanceId = 0;
                g.bindMap = 0;
            }
        }
    }

    /** MiscHandler.cpp HandleSetDungeonDifficultyOpcode + SendDungeonDifficulty (instance.md). */
    public static void setDungeonDifficulty(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        int mode = in.getU32();
        if (mode >= 2) {
            return;
        }
        if (mode == p.difficulty) {
            return;
        }
        if (p.instanceId != 0) {
            return;
        }
        if (p.level < 70 && mode > 0) {
            return;
        }
        Group g = p.group;
        if (g != null) {
            if (g.leaderGuid != p.guid) {
                return;
            }
            g.instanceId = 0;
            g.bindMap = 0;
            g.difficulty = mode;
            for (Player m : g.members) {
                if (m.level < 70) {
                    continue;
                }
                m.difficulty = mode;
                if (m.session != null) {
                    m.session.send(Opcodes.MSG_SET_DUNGEON_DIFFICULTY, difficultyPacket(mode, true));
                }
            }
            return;
        }
        p.difficulty = mode;
        s.send(Opcodes.MSG_SET_DUNGEON_DIFFICULTY, difficultyPacket(mode, false));
    }

    private static byte[] difficultyPacket(int mode, boolean inGroup) {
        WowBuffer b = new WowBuffer(12);
        b.putU32(mode);
        b.putU32(1);
        b.putU32(inGroup ? 1 : 0);
        return b.array();
    }
}
