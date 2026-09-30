package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterStoreQuestStatusTest {
    @Test
    void saveWhenQuestLogAndRewardedShouldReloadFromMemory() {
        CharacterStore store = new CharacterStore(null);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = store.create(1, "Quester", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        p.questLogId[0] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
        p.questLogState[0] = 0;
        p.questLogCounts[0][0] = 4;
        p.questLogCounts[0][1] = 2;
        p.questLogItemCount[0][0] = 3;
        p.questLogItemCount[0][1] = 1;
        p.rewardedQuests.add(Content.QUEST_A_THREAT_WITHIN);
        store.save(p);
        Player loaded = store.load(1, p.guid, mgr);
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, loaded.questLogId[0]);
        assertEquals(4, loaded.questLogCounts[0][0]);
        assertEquals(2, loaded.questLogCounts[0][1]);
        assertEquals(3, loaded.questLogItemCount[0][0]);
        assertEquals(1, loaded.questLogItemCount[0][1]);
        assertTrue(loaded.rewardedQuests.contains(Content.QUEST_A_THREAT_WITHIN));
        assertFalse(loaded.rewardedQuests.contains(Content.QUEST_KOBOLD_CAMP_CLEANUP));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                loaded.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_QUEST_LOG_1_1));
        assertEquals(4 | (2 << 8),
                loaded.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_QUEST_LOG_1_1 + 2));
    }

    @Test
    void saveWhenDatabaseConnectedShouldReloadQuestStatusFromSql() throws Exception {
        String url = "jdbc:h2:mem:quest_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "quest-status-test")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE characters (
                          guid INT PRIMARY KEY, account INT, name VARCHAR(12), race INT, class INT, gender INT,
                          level INT, xp INT, money INT, playerBytes INT, playerBytes2 INT, playerFlags INT,
                          position_x FLOAT, position_y FLOAT, position_z FLOAT, map INT, dungeon_difficulty INT,
                          orientation FLOAT, online INT, cinematic INT, totaltime INT, leveltime INT,
                          logout_time BIGINT, is_logout_resting INT, rest_bonus FLOAT, zone INT, at_login INT,
                          health INT, power1 INT, power2 INT, power3 INT, power4 INT, power5 INT,
                          watchedFaction BIGINT, actionBars TINYINT, deleteDate BIGINT)
                        """);
                st.execute("""
                        CREATE TABLE character_queststatus (
                          guid INT, quest INT, status INT, rewarded INT, explored INT, timer BIGINT,
                          mobcount1 INT, mobcount2 INT, mobcount3 INT, mobcount4 INT,
                          itemcount1 INT, itemcount2 INT, itemcount3 INT, itemcount4 INT,
                          PRIMARY KEY (guid, quest))
                        """);
                st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore store = new CharacterStore(chars);
            Player p = store.create(1, "Sqlquest", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
            p.questLogId[1] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
            p.questLogState[1] = Content.QUEST_STATE_COMPLETE;
            p.questLogCounts[1][0] = 10;
            p.questLogCounts[1][3] = 1;
            p.questLogItemCount[1][2] = 4;
            p.questLogId[2] = Content.QUEST_BROTHERHOOD_OF_THIEVES;
            p.questLogState[2] = Content.QUEST_STATE_FAIL;
            p.rewardedQuests.add(Content.QUEST_A_THREAT_WITHIN);
            store.save(p);
            CharacterStore again = new CharacterStore(chars);
            Player loaded = again.load(1, p.guid, mgr);
            assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, loaded.questLogId[0]);
            assertEquals(Content.QUEST_STATE_COMPLETE, loaded.questLogState[0]);
            assertEquals(10, loaded.questLogCounts[0][0]);
            assertEquals(1, loaded.questLogCounts[0][3]);
            assertEquals(4, loaded.questLogItemCount[0][2]);
            assertEquals(Content.QUEST_BROTHERHOOD_OF_THIEVES, loaded.questLogId[1]);
            assertEquals(Content.QUEST_STATE_FAIL, loaded.questLogState[1]);
            assertTrue(loaded.rewardedQuests.contains(Content.QUEST_A_THREAT_WITHIN));
            assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                    loaded.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_QUEST_LOG_1_1));
            assertEquals(Content.QUEST_STATE_COMPLETE,
                    loaded.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_QUEST_LOG_1_1 + 1));
        }
    }
}
