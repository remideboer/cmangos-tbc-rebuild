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
 * TP-SL26-221 — SPELL_AURA_MOD_FLIGHT_SPEED (206).
 * CMaNGOS UpdateSpeed MOVE_FLIGHT uses the sum of aura 206 while the unit is not mounted.
 */
class AuraEngineModFlightSpeedTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo speed(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenFlightSpeedShouldRaiseFlightRate() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED));
        Player player = new Player();
        Creature mob = new Creature();
        eng.apply(player, player, speed(999206, 100));
        eng.apply(player, mob, speed(999206, 100));
        assertEquals(14f, player.flightSpeed(), EPS);
        assertEquals(14f, mob.flightSpeed(), EPS);
    }

    @Test
    void applyWhenTwoAurasShouldSumAndUnapplyShouldLeaveTheOther() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo first = speed(999206, 50);
        eng.apply(player, player, first);
        eng.apply(player, player, speed(999306, 50));
        assertEquals(14f, player.flightSpeed(), EPS);
        eng.auras().unapply(player, first);
        assertEquals(10.5f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenMountedOrAfterASlowShouldUseTheFlightFormula() {
        SpellEngine eng = new SpellEngine();
        Player mounted = new Player();
        mounted.mounted = true;
        eng.apply(mounted, mounted, speed(999206, 100));
        assertEquals(Unit.BASE_FLIGHT_SPEED, mounted.flightSpeed(), EPS);

        Player slowed = new Player();
        eng.apply(slowed, slowed, new SpellEngine.SpellInfo(
                999033, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_DECREASE_SPEED, 0, 0, -50, -50, 0f));
        eng.apply(slowed, slowed, speed(999206, 100));
        assertEquals(Unit.BASE_FLIGHT_SPEED, slowed.flightSpeed(), EPS);

        Player capped = new Player();
        eng.apply(capped, capped, speed(999406, 100));
        eng.apply(capped, capped, new SpellEngine.SpellInfo(
                999191, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED, 0, 0, 7, 7, 0f));
        assertEquals(Unit.BASE_FLIGHT_SPEED, capped.flightSpeed(), EPS);
    }

    @Test
    void applyAuraWhenFlightSpeedOnPlayerShouldSendForceFlightSpeedChange() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        new SpellEngine().apply(new Player(), target, speed(999206, 100));
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_FLIGHT_SPEED_CHANGE));
        assertEquals(target.guid, pkt.getPackedGuid());
        assertEquals(0, pkt.getU32());
        assertEquals(14f, pkt.getFloat(), EPS);
        assertEquals(0, pkt.remaining());
    }

    @Test
    void applyWhenAmountZeroOrTargetMissingShouldLeaveBaseFlightSpeed() {
        new AuraEngine().apply(null, speed(999206, 100));
        Player player = new Player();
        new AuraEngine().apply(player, speed(999207, 0));
        assertEquals(Unit.BASE_FLIGHT_SPEED, player.flightSpeed(), EPS);
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
