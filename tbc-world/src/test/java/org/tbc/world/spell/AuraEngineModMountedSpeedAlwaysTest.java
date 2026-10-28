package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-201 — SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS (130).
 * CMaNGOS HandleAuraModIncreaseMountedSpeed → UpdateSpeed mounted stack term.
 */
class AuraEngineModMountedSpeedAlwaysTest {
    private static final SpellEngine.SpellInfo MOUNTED_ALWAYS = new SpellEngine.SpellInfo(
            999130, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS, 0, 0, 60, 60, 0f);

    @Test
    void applyAuraWhenMountedShouldSendForceRunSpeedChange() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        target.mounted = true;
        assertTrue(new SpellEngine().auras().knownAura(AuraEngine.SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS));
        new SpellEngine().apply(new Player(), target, MOUNTED_ALWAYS);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_RUN_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(0, pkt.getU8());
        assertEquals(Unit.BASE_RUN_SPEED * 1.6f, pkt.getFloat(), 0.001f);
    }

    @Test
    void unapplyWhenMountedShouldRestoreForceRunSpeed() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        target.mounted = true;
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, MOUNTED_ALWAYS);
        victim.last.clear();
        eng.auras().unapply(target, MOUNTED_ALWAYS);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_RUN_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(1, pkt.getU32());
        assertEquals(0, pkt.getU8());
        assertEquals(Unit.BASE_RUN_SPEED, pkt.getFloat(), 0.001f);
    }

    @Test
    void applyAuraWhenOnFootShouldNotChangeRunSpeed() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        new SpellEngine().apply(new Player(), target, MOUNTED_ALWAYS);
        assertNull(victim.last.get(Opcodes.SMSG_FORCE_RUN_SPEED_CHANGE));
        assertEquals(Unit.BASE_RUN_SPEED, target.runSpeed(), 0.001f);
    }

    @Test
    void applyWhenTargetMissingOrZeroAmountShouldNoOp() {
        new AuraEngine().apply(null, MOUNTED_ALWAYS);
        Player p = new Player();
        p.mounted = true;
        new AuraEngine().apply(p, new SpellEngine.SpellInfo(
                999131, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS,
                0, 0, 0, 0, 0f));
        assertEquals(Unit.BASE_RUN_SPEED, p.runSpeed(), 0.001f);
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
