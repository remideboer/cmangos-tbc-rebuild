package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL08-004: MovementType 1 creatures wander OOC so the 8606 client sees them walk. */
class Slice08P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl08WanderWhenMovementTypeRandomShouldSendMonsterMove() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Wander", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, -5000f, -5000f, 80f, 0f, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        c.movementType = 1;
        c.spawnDist = 10f;
        int[] n = {0};
        c.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        c.startOocMotion();
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x + 40, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        world.tick(1000);
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
        WowBuffer move = new WowBuffer(client.payload(Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(c.guid, move.getPackedGuid());
        move.getFloat();
        move.getFloat();
        move.getFloat();
        move.getU32();
        move.getU8();
        move.getU32();
        int duration = move.getU32();
        assertEquals(1, move.getU32());
        float destX = move.getFloat();
        float destY = move.getFloat();
        move.getFloat();
        assertTrue(c.spawnDistance2d(destX, destY) <= 10f + 0.01f);
        float path = (float) Math.hypot(destX - c.spawnX, destY - c.spawnY);
        assertEquals(Math.max(1, (int) (path / UpdateBuilder.WALK * 1000f)), duration);
    }

    /**
     * TP-SL08-019 — HandleQuestgiverStatusQueryOpcode / getDialogStatus: Willem 823 offers 783
     * (QUEST_STATUS_NONE + CanSeeStartQuest) → SMSG_QUESTGIVER_STATUS raw guid + DIALOG_STATUS_AVAILABLE 6.
     */
    @Test
    void tpSl08QuestgiverStatusQuery() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Quester", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);

        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(willem.guid);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_STATUS_QUERY, q.array());

        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_STATUS));
        WowBuffer st = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_STATUS));
        assertEquals(willem.guid, st.getU64());
        assertEquals(DIALOG_STATUS_AVAILABLE, st.getU8());
    }

    /**
     * TP-SL08-020 — HandleQuestgiverStatusMultipleQuery / SendQuestGiverStatusMultiple:
     * empty C2S; SMSG_QUESTGIVER_STATUS_MULTIPLE count + raw guid + status for each visible
     * UNIT_NPC_FLAG_QUESTGIVER creature (kobold 6 has no flag).
     */
    @Test
    void tpSl08QuestgiverStatusMultiple() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "QIcons", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        Creature kobold = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        world.map(p.mapId, p.instanceId).add(kobold);

        client.clear();
        client.handle(world, Opcodes.CMSG_QUESTGIVER_STATUS_MULTIPLE_QUERY, new byte[0]);

        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        WowBuffer st = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        int count = st.getU32();
        boolean sawWillem = false;
        for (int i = 0; i < count; i++) {
            long guid = st.getU64();
            int status = st.getU8();
            assertTrue(guid != kobold.guid);
            if (guid == willem.guid) {
                sawWillem = true;
                assertEquals(DIALOG_STATUS_AVAILABLE, status);
            }
        }
        assertTrue(sawWillem);
    }

    /**
     * TP-SL08-021 — Player::KilledMonsterCredit / SendQuestUpdateAddCreatureOrGo:
     * Kobold Camp Cleanup 7 ReqCreatureOrGOId1 6 count 10 → SMSG_QUESTUPDATE_ADD_KILL;
     * 10th kill marks QUEST_STATE_COMPLETE + SMSG_QUESTUPDATE_COMPLETE.
     */
    @Test
    void tpSl08KillObjectiveCounts() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "KoboldHunt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature mcbride = world.objectMgr.spawnCreature(Content.NPC_MARSHAL_MCBRIDE, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(mcbride);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(mcbride.guid);
        accept.putU32(Content.QUEST_KOBOLD_CAMP_CLEANUP);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());

        Creature first = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(first);
        client.clear();
        world.onCreatureKilled(p, first);

        assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        WowBuffer add = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, add.getU32());
        assertEquals(6, add.getU32());
        assertEquals(1, add.getU32());
        assertEquals(10, add.getU32());
        assertEquals(first.guid, add.getU64());
        int counts = p.getInt(UpdateFields.PLAYER_QUEST_LOG_1_1 + 2);
        assertEquals(1, counts & 0xFF);

        for (int i = 2; i <= 10; i++) {
            Creature k = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
            world.map(p.mapId, p.instanceId).add(k);
            client.clear();
            world.onCreatureKilled(p, k);
            WowBuffer row = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
            row.getU32();
            row.getU32();
            assertEquals(i, row.getU32());
            assertEquals(10, row.getU32());
        }
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        WowBuffer done = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, done.getU32());
    }

    /** QuestDef.h DIALOG_STATUS_AVAILABLE — yellow exclamation. */
    private static final int DIALOG_STATUS_AVAILABLE = 6;
}
