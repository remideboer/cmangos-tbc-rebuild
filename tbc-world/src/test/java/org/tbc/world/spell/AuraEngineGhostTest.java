package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
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
 * TP-SL26-150 — SPELL_AURA_GHOST (95). Spell.dbc 9036 Ghost.
 * CMaNGOS HandleAuraGhost → PLAYER_FLAGS_GHOST + SetWaterWalk.
 */
class AuraEngineGhostTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final SpellEngine.SpellInfo GHOST = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_GHOST, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_GHOST,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenGhostOnPlayerShouldSetFlagAndWaterWalk() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_GHOST));
        victim.last.clear();
        eng.apply(new Player(), target, GHOST);
        assertTrue(target.hasAura(SpellEngine.SPELL_GHOST));
        assertTrue(target.ghost);
        assertEquals(Player.PLAYER_FLAGS_GHOST,
                target.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST);
        WowBuffer pkt = new WowBuffer(victim.last.get(Opcodes.SMSG_MOVE_WATER_WALK));
        assertEquals(target.guid, pkt.getPackedGuid());
    }

    @Test
    void unapplyWhenGhostOnPlayerShouldClearFlagAndLandWalk() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), target, GHOST);
        victim.last.clear();
        eng.unapplyAura(target, SpellEngine.SPELL_GHOST);
        assertFalse(target.ghost);
        assertEquals(0, target.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST);
        assertTrue(victim.last.containsKey(Opcodes.SMSG_MOVE_LAND_WALK));
    }

    @Test
    void applyWhenCreatureShouldNoOpGhostFlag() {
        Creature mob = new Creature();
        new SpellEngine().apply(new Player(), mob, GHOST);
        assertTrue(mob.hasAura(SpellEngine.SPELL_GHOST));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, GHOST);
        new AuraEngine().unapply(null, GHOST);
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
