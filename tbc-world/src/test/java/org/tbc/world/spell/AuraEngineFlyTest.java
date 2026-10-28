package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-208 — SPELL_AURA_FLY (201).
 * CMaNGOS HandleAuraAllowFlight → SetCanFly.
 */
class AuraEngineFlyTest {
    private static final SpellEngine.SpellInfo FLY = new SpellEngine.SpellInfo(
            999201, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_FLY, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenFlyOnPlayerShouldSendMoveSetCanFly() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        assertTrue(new SpellEngine().auras().knownAura(AuraEngine.SPELL_AURA_FLY));
        new SpellEngine().apply(new Player(), target, FLY);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_SET_CAN_FLY));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(0, pkt.remaining());
    }

    @Test
    void unapplyWhenFlyOnPlayerShouldSendMoveUnsetCanFly() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, FLY);
        victim.last.clear();
        eng.auras().unapply(target, FLY);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_UNSET_CAN_FLY));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(1, pkt.getU32());
    }

    @Test
    void applyAuraWhenFlyOnCreatureShouldSendSplineSetFlying() {
        Creature mob = new Creature();
        mob.guid = 0xF13000000000000AL;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), mob, FLY);
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_SET_FLYING));
        assertEquals(mob.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.remaining());
        last.clear();
        eng.auras().unapply(mob, FLY);
        WowBuffer off = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_UNSET_FLYING));
        assertEquals(mob.guid, off.getPackedGuid());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FLY);
        new AuraEngine().unapply(null, FLY);
    }

    private static Sink login(World world, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

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
