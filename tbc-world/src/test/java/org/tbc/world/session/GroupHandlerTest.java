package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupHandlerTest {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void updateOfflineLeaderWhenLeaderStillOnlineShouldRefreshTimestamp() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        a.session.player().group = new Group();
        a.session.player().group.leaderGuid = a.session.player().guid;
        a.session.player().group.members.add(a.session.player());
        a.session.player().group.members.add(b.session.player());
        b.session.player().group = a.session.player().group;
        a.session.player().group.leaderLastOnlineMs = 1;
        long before = world.nowMs();
        GroupHandler.updateOfflineLeaders(world);
        assertTrue(a.session.player().group.leaderLastOnlineMs >= before);
        assertFalse(b.ops.contains(Opcodes.SMSG_GROUP_SET_LEADER));
    }

    @Test
    void updateOfflineLeaderWhenDelayNotElapsedShouldKeepLeader() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        Group g = new Group();
        g.leaderGuid = a.session.player().guid;
        g.members.add(a.session.player());
        g.members.add(b.session.player());
        a.session.player().group = g;
        b.session.player().group = g;
        a.session.player().session = null;
        g.leaderLastOnlineMs = world.nowMs() - 1;
        GroupHandler.updateOfflineLeader(g, world.nowMs());
        assertEquals(a.session.player().guid, g.leaderGuid);
        assertFalse(b.ops.contains(Opcodes.SMSG_GROUP_SET_LEADER));
    }

    @Test
    void updateOfflineLeaderWhenEveryoneOfflineShouldKeepLeader() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        Group g = new Group();
        g.leaderGuid = a.session.player().guid;
        g.members.add(a.session.player());
        g.members.add(b.session.player());
        a.session.player().group = g;
        b.session.player().group = g;
        a.session.player().session = null;
        b.session.player().session = null;
        g.leaderLastOnlineMs = 0;
        GroupHandler.updateOfflineLeader(g, world.nowMs());
        assertEquals(a.session.player().guid, g.leaderGuid);
    }

    private static Sink login(World world, World.Account acc, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
