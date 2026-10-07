package org.tbc.world.world;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Session-aware fan-out carved from World (refactoring plan cycle 3.1). */
class BroadcasterTest {
    private static final class RecordingSink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
        }

        @Override
        public void close() {
        }
    }

    private static Player online(GameMap m, long guid, float x, RecordingSink sink) {
        Player p = new Player();
        p.guid = guid;
        p.x = x;
        p.session = new WorldSession(sink, 0);
        m.add(p);
        return p;
    }

    private static Player offline(GameMap m, long guid, float x) {
        Player p = new Player();
        p.guid = guid;
        p.x = x;
        m.add(p);
        return p;
    }

    @Test
    void toPlayerWhenOnlineShouldSendAndWhenOfflineShouldDrop() {
        GameMap m = World.inMemory().map(0, 0);
        RecordingSink sink = new RecordingSink();
        Player on = online(m, 1, 0, sink);
        Player off = offline(m, 2, 0);
        Broadcaster.toPlayer(on, 0x1234, new byte[0]);
        Broadcaster.toPlayer(off, 0x1234, new byte[0]);
        assertEquals(List.of(0x1234), sink.ops);
    }

    @Test
    void sinkWhenOnlineShouldSendAndWhenOfflineShouldSwallow() {
        GameMap m = World.inMemory().map(0, 0);
        RecordingSink sink = new RecordingSink();
        Broadcaster.sink(online(m, 1, 0, sink)).accept(0x77, new byte[0]);
        Broadcaster.sink(offline(m, 2, 0)).accept(0x77, new byte[0]);
        assertEquals(List.of(0x77), sink.ops);
    }

    @Test
    void sinkOrNullWhenOfflineShouldBeNull() {
        GameMap m = World.inMemory().map(0, 0);
        RecordingSink sink = new RecordingSink();
        assertNotNull(Broadcaster.sinkOrNull(online(m, 1, 0, sink)));
        assertNull(Broadcaster.sinkOrNull(offline(m, 2, 0)));
    }

    @Test
    void nearbyShouldReachOnlinePlayersInRangeButNotSelfOrFarOrOffline() {
        GameMap m = World.inMemory().map(0, 0);
        RecordingSink selfSink = new RecordingSink();
        RecordingSink nearSink = new RecordingSink();
        RecordingSink farSink = new RecordingSink();
        Player self = online(m, 1, 0, selfSink);
        online(m, 2, 10, nearSink);
        online(m, 3, 500, farSink);
        offline(m, 4, 5);
        Broadcaster.nearby(m, self, GameMap.VISIBILITY, 0x42, new byte[0]);
        assertEquals(List.of(), selfSink.ops);
        assertEquals(List.of(0x42), nearSink.ops);
        assertEquals(List.of(), farSink.ops);
    }

    @Test
    void nearbyAndSelfShouldAlsoReachTheUnitItselfWhenOnline() {
        GameMap m = World.inMemory().map(0, 0);
        RecordingSink selfSink = new RecordingSink();
        RecordingSink nearSink = new RecordingSink();
        Player self = online(m, 1, 0, selfSink);
        online(m, 2, 10, nearSink);
        Broadcaster.nearbyAndSelf(m, self, GameMap.VISIBILITY, 0x42, new byte[0]);
        assertEquals(List.of(0x42), selfSink.ops);
        assertEquals(List.of(0x42), nearSink.ops);
    }

    @Test
    void nearbyAndSelfWhenUnitOfflineShouldStillReachNeighbours() {
        GameMap m = World.inMemory().map(0, 0);
        RecordingSink nearSink = new RecordingSink();
        Player self = offline(m, 1, 0);
        online(m, 2, 10, nearSink);
        Broadcaster.nearbyAndSelf(m, self, GameMap.VISIBILITY, 0x42, new byte[0]);
        assertEquals(List.of(0x42), nearSink.ops);
    }
}
