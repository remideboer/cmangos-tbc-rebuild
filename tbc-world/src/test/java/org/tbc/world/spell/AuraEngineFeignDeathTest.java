package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.tbc.common.WowBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-152 — SPELL_AURA_FEIGN_DEATH (66). Spell.dbc 5384 Feign Death.
 * CMaNGOS SetFeignDeath → UNIT_FLAG2_FEIGN_DEATH + UNIT_DYNFLAG_DEAD.
 * TP-SL26-169 — PLAYER_CONTROLLED success path CombatStop (inCombat/victim cleared).
 * TP-SL26-175 — PLAYER_CONTROLLED resist roll vs NPC attackers; fail → SMSG_FEIGN_DEATH_RESISTED.
 */
class AuraEngineFeignDeathTest {
    private static final SpellEngine.SpellInfo FEIGN_DEATH = new SpellEngine.SpellInfo(
            SpellEngine.FEIGN_DEATH, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_FEIGN_DEATH,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenFeignDeathOnPlayerShouldSetFlags2AndDynDead() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_FEIGN_DEATH));
        Player player = new Player();
        eng.apply(player, player, FEIGN_DEATH);
        assertTrue(player.hasAura(SpellEngine.FEIGN_DEATH));
        assertTrue(player.isFeigningDeath());
        assertEquals(Unit.UNIT_FLAG2_FEIGN_DEATH,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & Unit.UNIT_FLAG2_FEIGN_DEATH);
        assertEquals(Unit.UNIT_DYNFLAG_DEAD,
                player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_DEAD);
    }

    @Test
    void unapplyWhenFeignDeathOnPlayerShouldClearFlags() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FEIGN_DEATH);
        eng.unapplyAura(player, SpellEngine.FEIGN_DEATH);
        assertFalse(player.isFeigningDeath());
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & Unit.UNIT_FLAG2_FEIGN_DEATH);
        assertEquals(0, player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_DEAD);
    }

    /** TP-SL26-169 — SetFeignDeath success + PLAYER_CONTROLLED → CombatStop. */
    @Test
    void applyAuraWhenFeignDeathOnPlayerInCombatShouldCombatStop() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.inCombat = true;
        player.victim = 0x100000000000006L;
        player.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IN_COMBAT);

        eng.apply(player, player, FEIGN_DEATH);

        assertTrue(player.isFeigningDeath());
        assertFalse(player.inCombat);
        assertEquals(0L, player.victim);
    }

    @Test
    void applyWhenFeignDeathOnCreatureShouldNotCombatStop() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.inCombat = true;
        mob.victim = 0x100000000000001L;

        eng.apply(new Player(), mob, FEIGN_DEATH);

        assertTrue(mob.isFeigningDeath());
        assertTrue(mob.inCombat);
        assertEquals(0x100000000000001L, mob.victim);
    }

    /** TP-SL26-175 — max miss vs NPC attackers; 100% always SendFeignDeathResisted, no CombatStop. */
    @Test
    void applyWhenNpcAttackerMissesCompletelyShouldSendFeignDeathResistedAndKeepCombat() {
        World world = World.inMemory();
        Sink sink = login(world, "Hunter");
        Player player = sink.session.player();
        player.inCombat = true;
        player.victim = 0xF130000000000006L;
        player.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IN_COMBAT);
        player.adjustSpellHitChance(-100f);
        Creature mob = new Creature();
        mob.guid = 0xF130000000000006L;
        player.addAttacker(mob);

        new SpellEngine().apply(player, player, FEIGN_DEATH);

        assertTrue(player.isFeigningDeath());
        assertTrue(player.inCombat);
        assertEquals(0xF130000000000006L, player.victim);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_FEIGN_DEATH_RESISTED));
        assertEquals(0, sink.last.get(Opcodes.SMSG_FEIGN_DEATH_RESISTED).length);
    }

    @Test
    void applyWhenOnlyPlayerControlledAttackerShouldStillCombatStop() {
        Player player = new Player();
        player.inCombat = true;
        player.victim = 0x100000000000002L;
        player.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IN_COMBAT);
        player.adjustSpellHitChance(-100f);
        Player other = new Player();
        player.addAttacker(other);

        new SpellEngine().apply(player, player, FEIGN_DEATH);

        assertTrue(player.isFeigningDeath());
        assertFalse(player.inCombat);
        assertEquals(0L, player.victim);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FEIGN_DEATH);
        new AuraEngine().unapply(null, FEIGN_DEATH);
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

    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private static final class Sink implements PacketSink {
        final java.util.List<Integer> ops = new java.util.ArrayList<>();
        final java.util.Map<Integer, byte[]> last = new java.util.HashMap<>();
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
