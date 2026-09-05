package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentHandlerTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void learnWhenWarriorSpendsRank0ShouldSendImprovedHeroicStrike() {
        World world = World.inMemory();
        Sink s = login(world, 1, 1, "Arms");
        s.session.player().setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        s.ops.clear();
        s.last.clear();
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertEquals(12282, new WowBuffer(s.last.get(Opcodes.SMSG_LEARNED_SPELL)).getU32());
        assertTrue(s.session.player().spells.contains(12282));
        assertEquals(0, s.session.player().getInt(UpdateFields.PLAYER_CHARACTER_POINTS1));
    }

    @Test
    void learnWhenNoPointsOrShortPayloadOrUnknownShouldStaySilent() {
        World world = World.inMemory();
        Sink s = login(world, 1, 1, "Arms");
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
        s.session.player().setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        TalentHandler.learn(s.session, world, new WowBuffer(new byte[4]));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
        TalentHandler.learn(s.session, world, talent(1, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
        TalentHandler.learn(s.session, world, talent(124, 5));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
    }

    @Test
    void learnWhenWrongClassOrMissingTabShouldStaySilent() {
        World world = World.inMemory();
        Sink mage = login(world, 1, 8, "Mage");
        mage.session.player().setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        TalentHandler.learn(mage.session, world, talent(124, 0));
        assertFalse(mage.ops.contains(Opcodes.SMSG_LEARNED_SPELL));

        Sink w = login(world, 1, 1, "Arms2");
        w.session.player().setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        world.objectMgr.talentTabs.remove(161);
        TalentHandler.learn(w.session, world, talent(124, 0));
        assertFalse(w.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
    }

    @Test
    void learnWhenAlreadyKnownOrNotEnoughPointsShouldStaySilent() {
        World world = World.inMemory();
        Sink s = login(world, 1, 1, "Ranked");
        s.session.player().setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        s.session.player().spells.add(12282);
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
        TalentHandler.learn(s.session, world, talent(124, 2));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));
    }

    @Test
    void learnWhenDependsOnOrRowOrSpellMissingShouldStaySilent() {
        World world = World.inMemory();
        Sink s = login(world, 1, 1, "Prereq");
        Player p = s.session.player();
        p.setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 5);
        world.objectMgr.talents.put(124, new ObjectMgr.Talent(
                124, 161, 0, 0, 12282, 12663, 12664, 0, 0, 124, 0, 0));
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));

        world.objectMgr.talents.put(124, new ObjectMgr.Talent(
                124, 161, 1, 0, 12282, 12663, 12664, 0, 0, 0, 0, 0));
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));

        world.objectMgr.talents.put(124, new ObjectMgr.Talent(
                124, 161, 0, 0, 12282, 12663, 12664, 0, 0, 0, 0, 78));
        p.spells.remove(Integer.valueOf(78));
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));

        world.objectMgr.talents.put(124, new ObjectMgr.Talent(
                124, 161, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertFalse(s.ops.contains(Opcodes.SMSG_LEARNED_SPELL));

        world.objectMgr.talents.put(124, new ObjectMgr.Talent(
                124, 161, 0, 0, 12282, 12663, 12664, 0, 0, 1, 0, 0));
        s.ops.clear();
        s.last.clear();
        TalentHandler.learn(s.session, world, talent(124, 0));
        assertEquals(12282, new WowBuffer(s.last.get(Opcodes.SMSG_LEARNED_SPELL)).getU32());
    }

    @Test
    void learnWhenRank1ShouldUnlearnRank0() {
        World world = World.inMemory();
        Sink s = login(world, 1, 1, "RankUp");
        Player p = s.session.player();
        p.setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        p.spells.add(12282);
        s.ops.clear();
        s.last.clear();
        TalentHandler.learn(s.session, world, talent(124, 1));
        assertEquals(12663, new WowBuffer(s.last.get(Opcodes.SMSG_LEARNED_SPELL)).getU32());
        assertFalse(p.spells.contains(12282));
        assertTrue(p.spells.contains(12663));
    }

    @Test
    void learnWhenDependsOnRank0KnownShouldLearnNextRank() {
        World world = World.inMemory();
        Sink s = login(world, 1, 1, "Dep");
        Player p = s.session.player();
        p.setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        p.spells.add(12282);
        world.objectMgr.talents.put(124, new ObjectMgr.Talent(
                124, 161, 0, 0, 12282, 12663, 12664, 0, 0, 124, 0, 0));
        s.ops.clear();
        s.last.clear();
        TalentHandler.learn(s.session, world, talent(124, 1));
        assertEquals(12663, new WowBuffer(s.last.get(Opcodes.SMSG_LEARNED_SPELL)).getU32());
    }

    private static WowBuffer talent(int id, int rank) {
        WowBuffer b = new WowBuffer(8);
        b.putU32(id);
        b.putU32(rank);
        return b;
    }

    private static Sink login(World world, int race, int clazz, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), name, race, clazz, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
