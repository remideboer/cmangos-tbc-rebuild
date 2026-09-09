package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL07-004 — Spell::SendInterrupted goes to the set: a nearby player sees the cancelled cast. */
class SpellEngineInterruptBroadcastTest {
    @Test
    void cancelWhenCasterMovesShouldSendSpellFailureToNearbyPlayer() {
        World world = World.inMemory();
        Sink caster = login(world, "Pyro", 1);
        Sink watcher = login(world, "Watcher", 2);
        Player p = caster.session.player();
        p.spells.add(SpellEngine.FIREBALL);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        WowBuffer target = new WowBuffer(12);
        target.putU32(SpellCastTargets.UNIT);
        target.putPackedGuid(c.guid);
        world.spells.cast(p, world.map(p.mapId, p.instanceId), 10, SpellEngine.FIREBALL, 1,
                new WowBuffer(target.array()), caster.session::send);
        watcher.last.clear();

        p.relocate(p.x + 1f, p.y, p.z, p.o);
        world.spells.update(100, 10);

        byte[] fail = watcher.last.get(Opcodes.SMSG_SPELL_FAILURE);
        WowBuffer b = new WowBuffer(fail);
        assertEquals(p.guid, b.getPackedGuid());
        assertEquals(SpellEngine.FIREBALL, b.getU32());
        assertEquals(SpellEngine.SPELL_FAILED_INTERRUPTED, b.getU8());
        assertTrue(watcher.last.containsKey(Opcodes.SMSG_SPELL_FAILED_OTHER));
        assertFalse(watcher.last.containsKey(Opcodes.SMSG_CAST_RESULT), "cast result is caster-only");
    }

    private static Sink login(World world, String name, int accountId) {
        World.Account acc = new World.Account(accountId, name.toUpperCase(), new byte[40], 3, 1, "Win", "x86");
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, accountId);
        s.injectAccount(acc);
        Player created = world.characters.create(accountId, name, 1, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
