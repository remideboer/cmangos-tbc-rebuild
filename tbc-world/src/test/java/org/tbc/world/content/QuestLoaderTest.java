package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** quest_template and quest relations loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class QuestLoaderTest {
    @Test
    void questQueriesWhenLoadingShouldIncludeDetailsRelationsAndNoLimit() {
        String templates = String.join("\n", QuestLoader.questTemplateQueries());
        assertTrue(templates.contains("Details"), templates);
        assertTrue(templates.contains("ZoneOrSort"), templates);
        assertFalse(templates.toUpperCase().contains("LIMIT"), templates);
        String relations = String.join("\n", QuestLoader.questRelationQueries());
        assertTrue(relations.contains("creature_questrelation"), relations);
        assertTrue(relations.contains("creature_involvedrelation"), relations);
    }

    @Test
    void loadWhenMinimalQuestColumnsShouldFallBackAndIndexRelations() throws Exception {
        String url = "jdbc:h2:mem:quest_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "quest-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE quest_template (entry INT, Title VARCHAR(64), MinLevel INT, Type INT, "
                        + "ReqCreatureOrGOId1 INT, ReqCreatureOrGOCount1 INT)");
                st.execute("INSERT INTO quest_template VALUES (783, 'A Threat Within', 1, 0, 0, 0)");
                st.execute("INSERT INTO quest_template VALUES (7, 'Kobold Camp Cleanup', 1, 0, 6, 10)");
                st.execute("CREATE TABLE creature_questrelation (id INT, quest INT)");
                st.execute("INSERT INTO creature_questrelation VALUES (197, 783)");
                st.execute("INSERT INTO creature_questrelation VALUES (197, 783)");
                st.execute("CREATE TABLE creature_involvedrelation (id INT, quest INT)");
                st.execute("INSERT INTO creature_involvedrelation VALUES (197, 783)");
                st.execute("CREATE TABLE areatrigger_involvedrelation (id INT, quest INT)");
                st.execute("INSERT INTO areatrigger_involvedrelation VALUES (2230, 7)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                QuestLoader.load(m, c);
            }
            assertEquals("A Threat Within", m.quests.get(783).title());
            assertEquals(6, m.quests.get(7).reqCreatureOrGOId1());
            assertEquals(10, m.quests.get(7).reqCreatureOrGOCount1());
            assertEquals(List.of(783), m.questGivers.get(197), "duplicate relation rows collapse");
            assertEquals(List.of(783), m.questInvolved.get(197));
            assertEquals(7, m.areaTriggerQuests.get(2230));
            assertTrue(m.questExtras.isEmpty(), "quest_template without extras columns leaves extras empty");
        }
    }

    @Test
    void loadWhenNoQuestTablesShouldLeaveQuestsEmpty() throws Exception {
        String url = "jdbc:h2:mem:quest_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "quest-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                QuestLoader.load(m, c);
            }
            assertTrue(m.quests.isEmpty());
            assertTrue(m.questGivers.isEmpty());
        }
    }
}
