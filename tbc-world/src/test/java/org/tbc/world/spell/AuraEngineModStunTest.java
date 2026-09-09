package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-120 — SPELL_AURA_MOD_STUN (12). Hammer of Justice 853 (effect 6 APPLY_AURA, aura 12).
 * CMaNGOS HandleAuraModStun → Unit::SetStunned → UNIT_FLAG_STUNNED on UNIT_FIELD_FLAGS.
 */
class AuraEngineModStunTest {
    private static final SpellEngine.SpellInfo HAMMER_OF_JUSTICE = new SpellEngine.SpellInfo(
            853, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STUN, 2, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModStunShouldSetStunnedFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_STUN));
        Player paladin = new Player();
        Creature mob = new Creature();
        eng.apply(paladin, mob, HAMMER_OF_JUSTICE);
        assertTrue(mob.hasAura(853));
        assertEquals(Unit.UNIT_FLAG_STUNNED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_STUNNED);
    }

    @Test
    void applyAuraWhenNotStunShouldLeaveStunnedFlagClear() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        SpellEngine.SpellInfo periodic = new SpellEngine.SpellInfo(
                172, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PERIODIC_DAMAGE, 32, 0, 0, 0, 0f);
        eng.apply(new Player(), mob, periodic);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_STUNNED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HAMMER_OF_JUSTICE);
    }

    /**
     * TP-SL26-122 — CMaNGOS SetStunned → SetImmobilizedState(stun=true) → SendMoveRoot(true):
     * a stunned player is also rooted on the wire (movement.md SMSG_FORCE_MOVE_ROOT + counter).
     */
    @Test
    void applyAuraWhenModStunOnPlayerShouldAlsoSendForceMoveRoot() {
        World world = World.inMemory();
        Sink victim = login(world, "Victim");
        Player target = victim.session.player();
        new SpellEngine().apply(new Player(), target, HAMMER_OF_JUSTICE);
        assertEquals(Unit.UNIT_FLAG_STUNNED,
                target.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_STUNNED);
        WowBuffer root = new WowBuffer(victim.last.get(Opcodes.SMSG_FORCE_MOVE_ROOT));
        assertEquals(target.guid, root.getPackedGuid());
        assertEquals(0, root.getU32());
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
