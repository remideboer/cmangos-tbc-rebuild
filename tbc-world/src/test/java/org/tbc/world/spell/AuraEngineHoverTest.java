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
 * TP-SL26-140 — SPELL_AURA_HOVER (106). Spell.dbc 11010 Hover.
 * CMaNGOS HandleAuraHover → SetHover → SMSG_MOVE_SET_HOVER / UNSET_HOVER (player)
 * or SMSG_SPLINE_MOVE_SET_HOVER / UNSET_HOVER (creature).
 */
class AuraEngineHoverTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final SpellEngine.SpellInfo HOVER = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_HOVER, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_HOVER,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenHoverOnPlayerShouldSendMoveSetHoverWithCounter() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_HOVER));

        eng.apply(new Player(), target, HOVER);

        assertTrue(target.hasAura(SpellEngine.SPELL_HOVER));
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_SET_HOVER));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(0, pkt.remaining());
    }

    @Test
    void unapplyWhenHoverOnPlayerShouldSendMoveUnsetHover() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, HOVER);
        victim.last.clear();
        eng.unapplyAura(target, SpellEngine.SPELL_HOVER);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_UNSET_HOVER));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(1, pkt.getU32());
    }

    @Test
    void applyAuraWhenHoverOnCreatureShouldSendSplineSetHover() {
        Creature mob = new Creature();
        mob.guid = 0xF13000000000000CL;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        new SpellEngine().apply(new Player(), mob, HOVER);
        assertTrue(mob.hasAura(SpellEngine.SPELL_HOVER));
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_SET_HOVER));
        assertEquals(mob.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.remaining());
    }

    @Test
    void unapplyWhenHoverOnCreatureShouldSendSplineUnsetHover() {
        Creature mob = new Creature();
        mob.guid = 0xF13000000000000DL;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), mob, HOVER);
        last.clear();
        eng.unapplyAura(mob, SpellEngine.SPELL_HOVER);
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_UNSET_HOVER));
        assertEquals(mob.guid, pkt.getPackedGuid());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HOVER);
        new AuraEngine().unapply(null, HOVER);
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
