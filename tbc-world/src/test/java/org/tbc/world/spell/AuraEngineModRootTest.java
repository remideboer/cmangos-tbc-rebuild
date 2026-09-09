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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-121 — SPELL_AURA_MOD_ROOT (26). Frost Nova 122 (effect 2 aura 26, frost).
 * CMaNGOS HandleAuraModRoot → SetImmobilizedState → SendMoveRoot: the controlling player's
 * session gets SMSG_FORCE_MOVE_ROOT = packed guid + uint32 order counter (movement.md).
 */
class AuraEngineModRootTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final SpellEngine.SpellInfo FROST_NOVA = new SpellEngine.SpellInfo(
            122, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_ROOT, 16, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModRootOnPlayerShouldSendForceMoveRootWithCounter() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ROOT));

        eng.apply(new Player(), target, FROST_NOVA);

        assertTrue(target.hasAura(122));
        WowBuffer root = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_MOVE_ROOT));
        assertEquals(target.guid, root.getPackedGuid());
        assertEquals(0, root.getU32());
        assertEquals(0, root.remaining());
    }

    @Test
    void applyAuraWhenModRootTwiceShouldIncrementOrderCounter() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();

        eng.apply(new Player(), target, FROST_NOVA);
        eng.apply(new Player(), target, FROST_NOVA);

        WowBuffer root = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_MOVE_ROOT));
        root.getPackedGuid();
        assertEquals(1, root.getU32());
    }

    @Test
    void applyAuraWhenModRootOnOfflinePlayerOrCreatureShouldNotThrow() {
        SpellEngine eng = new SpellEngine();
        Player offline = new Player();
        eng.apply(new Player(), offline, FROST_NOVA);
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FROST_NOVA);
        assertTrue(offline.hasAura(122));
        assertTrue(mob.hasAura(122));
    }

    @Test
    void sendMoveRootWhenReleasedShouldSendForceMoveUnroot() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        target.sendMoveRoot(false);
        WowBuffer unroot = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_MOVE_UNROOT));
        assertEquals(target.guid, unroot.getPackedGuid());
        assertEquals(0, unroot.getU32());
    }

    @Test
    void applyAuraWhenNotRootShouldNotSendForceMoveRoot() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine.SpellInfo stun = new SpellEngine.SpellInfo(
                853, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STUN, 2, 0, 0, 0, 0f);
        new SpellEngine().apply(new Player(), target, stun);
        assertFalse(victim.ops.contains(Opcodes.SMSG_FORCE_MOVE_ROOT));
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
