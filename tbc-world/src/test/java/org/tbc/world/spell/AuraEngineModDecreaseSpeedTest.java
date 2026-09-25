package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-127 — SPELL_AURA_MOD_DECREASE_SPEED (33). Frostbolt 116 EffectBasePoints+1 = −40%.
 * CMaNGOS HandleAuraModDecreaseSpeed → UpdateSpeed → SetSpeedRate → SMSG_FORCE_RUN_SPEED_CHANGE.
 */
class AuraEngineModDecreaseSpeedTest {
    /** Spell.dbc 116 effect0: APPLY_AURA aura 33, amount −40. */
    private static final SpellEngine.SpellInfo FROSTBOLT_SLOW = new SpellEngine.SpellInfo(
            SpellEngine.FROSTBOLT, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DECREASE_SPEED,
            4, 0, -40, -40, 0f);

    @Test
    void applyAuraWhenModDecreaseSpeedOnPlayerShouldSendForceRunSpeedChange() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        assertTrue(new SpellEngine().auras().knownAura(AuraEngine.SPELL_AURA_MOD_DECREASE_SPEED));
        new SpellEngine().apply(new Player(), target, FROSTBOLT_SLOW);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_RUN_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(0, pkt.getU8());
        assertEquals(Unit.BASE_RUN_SPEED * 0.6f, pkt.getFloat(), 0.001f);
        assertEquals(Unit.BASE_RUN_SPEED * 0.6f, target.runSpeed(), 0.001f);
    }

    @Test
    void unapplyWhenModDecreaseSpeedOnPlayerShouldRestoreForceRunSpeed() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, FROSTBOLT_SLOW);
        victim.last.clear();
        eng.unapplyAura(target, SpellEngine.FROSTBOLT);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_RUN_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(1, pkt.getU32());
        assertEquals(0, pkt.getU8());
        assertEquals(Unit.BASE_RUN_SPEED, pkt.getFloat(), 0.001f);
        assertEquals(Unit.BASE_RUN_SPEED, target.runSpeed(), 0.001f);
    }

    @Test
    void applyAuraWhenModDecreaseSpeedOnCreatureShouldSendSplineSetRunSpeed() {
        Creature mob = new Creature();
        mob.guid = 0xF130000000000009L;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        new SpellEngine().apply(new Player(), mob, FROSTBOLT_SLOW);
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_SET_RUN_SPEED));
        assertEquals(mob.guid, pkt.getPackedGuid());
        assertEquals(Unit.BASE_RUN_SPEED * 0.6f, pkt.getFloat(), 0.001f);
    }

    @Test
    void applyWhenTargetMissingOrZeroAmountShouldNoOp() {
        new AuraEngine().apply(null, FROSTBOLT_SLOW);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                999010, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DECREASE_SPEED,
                0, 0, 0, 0, 0f);
        new AuraEngine().apply(p, zero);
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
