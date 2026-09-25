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
 * TP-SL26-141 — SPELL_AURA_MOD_INCREASE_SWIM_SPEED (58). Spell.dbc 7840 Swim Speed +100%.
 * CMaNGOS HandleAuraModIncreaseSwimSpeed → UpdateSpeed(MOVE_SWIM) → FORCE/SPLINE swim speed.
 */
class AuraEngineModIncreaseSwimSpeedTest {
    private static final SpellEngine.SpellInfo SWIM = new SpellEngine.SpellInfo(
            SpellEngine.SWIM_SPEED, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_INCREASE_SWIM_SPEED, 0, 0, 100, 100, 0f);

    @Test
    void applyAuraWhenModIncreaseSwimSpeedOnPlayerShouldSendForceSwimSpeedChange() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        assertTrue(new SpellEngine().auras().knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_SWIM_SPEED));
        new SpellEngine().apply(new Player(), target, SWIM);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_SWIM_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(Unit.BASE_SWIM_SPEED * 2.0f, pkt.getFloat(), 0.001f);
        assertEquals(Unit.BASE_SWIM_SPEED * 2.0f, target.swimSpeed(), 0.001f);
    }

    @Test
    void unapplyWhenModIncreaseSwimSpeedOnPlayerShouldRestoreForceSwimSpeed() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, SWIM);
        victim.last.clear();
        eng.unapplyAura(target, SpellEngine.SWIM_SPEED);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_SWIM_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(1, pkt.getU32());
        assertEquals(Unit.BASE_SWIM_SPEED, pkt.getFloat(), 0.001f);
    }

    @Test
    void applyAuraWhenModIncreaseSwimSpeedOnCreatureShouldSendSplineSetSwimSpeed() {
        Creature mob = new Creature();
        mob.guid = 0xF13000000000000EL;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        new SpellEngine().apply(new Player(), mob, SWIM);
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_SET_SWIM_SPEED));
        assertEquals(mob.guid, pkt.getPackedGuid());
        assertEquals(Unit.BASE_SWIM_SPEED * 2.0f, pkt.getFloat(), 0.001f);
    }

    @Test
    void applyWhenTargetMissingOrZeroAmountShouldNoOp() {
        new AuraEngine().apply(null, SWIM);
        new AuraEngine().unapply(null, SWIM);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                999058, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_SWIM_SPEED,
                0, 0, 0, 0, 0f);
        new AuraEngine().apply(p, zero);
        assertEquals(Unit.BASE_SWIM_SPEED, p.swimSpeed(), 0.001f);
    }

    private static Sink login(World world, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86"));
        Player created = world.characters.create(1, name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
