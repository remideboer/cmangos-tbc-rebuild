package org.tbc.world.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Player;

/** character_queststatus rows owned by QuestStatusPersist, split out of CharacterStore (plan cycle 5.3). */
class QuestStatusPersistTest {

    @Test
    void writeThenLoadShouldRoundTripLogSlotsAndRewarded() throws Exception {
        String url = "jdbc:h2:mem:qsp_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE character_queststatus (
                          guid INT, quest INT, status INT, rewarded INT, explored INT, timer BIGINT,
                          mobcount1 INT, mobcount2 INT, mobcount3 INT, mobcount4 INT,
                          itemcount1 INT, itemcount2 INT, itemcount3 INT, itemcount4 INT,
                          PRIMARY KEY (guid, quest))
                        """);
            }
            Player p = new Player();
            p.guid = 77;
            p.questLogId[3] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
            p.questLogState[3] = Content.QUEST_STATE_COMPLETE;
            p.questLogCounts[3][1] = 6;
            p.questLogItemCount[3][0] = 2;
            p.rewardedQuests.add(Content.QUEST_A_THREAT_WITHIN);

            QuestStatusPersist.write(c, p);
            Player loaded = new Player();
            loaded.guid = 77;
            QuestStatusPersist.load(c, loaded);

            assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, loaded.questLogId[0]);
            assertEquals(Content.QUEST_STATE_COMPLETE, loaded.questLogState[0]);
            assertEquals(6, loaded.questLogCounts[0][1]);
            assertEquals(2, loaded.questLogItemCount[0][0]);
            assertTrue(loaded.rewardedQuests.contains(Content.QUEST_A_THREAT_WITHIN));
        }
    }

    @Test
    void dbQuestStatusShouldMapLogStateToQuestDefValues() {
        assertEquals(1, QuestStatusPersist.dbQuestStatus(Content.QUEST_STATE_COMPLETE));
        assertEquals(5, QuestStatusPersist.dbQuestStatus(Content.QUEST_STATE_FAIL));
        assertEquals(3, QuestStatusPersist.dbQuestStatus(0));
        assertEquals(Content.QUEST_STATE_COMPLETE, QuestStatusPersist.logQuestState(1));
        assertEquals(Content.QUEST_STATE_FAIL, QuestStatusPersist.logQuestState(5));
        assertEquals(0, QuestStatusPersist.logQuestState(3));
    }
}
