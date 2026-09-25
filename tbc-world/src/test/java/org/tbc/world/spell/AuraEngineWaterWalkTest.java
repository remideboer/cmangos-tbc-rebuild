package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-138 — SPELL_AURA_WATER_WALK (104). Spell.dbc 546 Water Walking.
 * CMaNGOS HandleAuraWaterWalk → SetWaterWalk → SMSG_MOVE_WATER_WALK / LAND_WALK (player)
 * or SMSG_SPLINE_MOVE_WATER_WALK / LAND_WALK (creature).
 */
class AuraEngineWaterWalkTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final SpellEngine.SpellInfo WATER_WALK = new SpellEngine.SpellInfo(
            SpellEngine.WATER_WALKING, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_WATER_WALK,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenWaterWalkOnPlayerShouldSendMoveWaterWalkWithCounter() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_WATER_WALK));

        eng.apply(new Player(), target, WATER_WALK);

        assertTrue(target.hasAura(SpellEngine.WATER_WALKING));
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_WATER_WALK));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(0, pkt.remaining());
    }

    @Test
    void unapplyWhenWaterWalkOnPlayerShouldSendMoveLandWalk() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, WATER_WALK);
        victim.last.clear();
        eng.unapplyAura(target, SpellEngine.WATER_WALKING);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_LAND_WALK));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(1, pkt.getU32());
    }

    @Test
    void applyAuraWhenWaterWalkOnCreatureShouldSendSplineWaterWalk() {
        Creature mob = new Creature();
        mob.guid = 0xF130000000000008L;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        new SpellEngine().apply(new Player(), mob, WATER_WALK);
        assertTrue(mob.hasAura(SpellEngine.WATER_WALKING));
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_WATER_WALK));
        assertEquals(mob.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.remaining());
    }

    @Test
    void unapplyWhenWaterWalkOnCreatureShouldSendSplineLandWalk() {
        Creature mob = new Creature();
        mob.guid = 0xF130000000000009L;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), mob, WATER_WALK);
        last.clear();
        eng.unapplyAura(mob, SpellEngine.WATER_WALKING);
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_LAND_WALK));
        assertEquals(mob.guid, pkt.getPackedGuid());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, WATER_WALK);
        new AuraEngine().unapply(null, WATER_WALK);
    }

    private static Sink login(World world, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
