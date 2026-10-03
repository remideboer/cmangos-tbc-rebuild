package org.tbc.world.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** quest_template text and creature quest relations are loaded without a row cap. */
class ObjectMgrQuestLoadTest {
    @Test
    void questQueriesWhenLoadingShouldIncludeDetailsRelationsAndNoLimit() {
        String templates = String.join("\n", ObjectMgr.questTemplateQueries());
        assertTrue(templates.contains("Details"), templates);
        assertTrue(templates.contains("Objectives"), templates);
        assertTrue(templates.contains("ReqCreatureOrGOId2"), templates);
        assertTrue(templates.contains("ReqItemId2"), templates);
        assertTrue(templates.contains("PrevQuestId"), templates);
        assertTrue(templates.contains("RewOrReqMoney"), templates);
        assertTrue(templates.contains("RequiredRaces"), templates);
        assertTrue(templates.contains("ZoneOrSort"), templates);
        assertFalse(templates.toUpperCase().contains("LIMIT"), templates);
        String relations = String.join("\n", ObjectMgr.questRelationQueries());
        assertTrue(relations.contains("creature_questrelation"), relations);
        assertTrue(relations.contains("creature_involvedrelation"), relations);
        assertFalse(relations.toUpperCase().contains("LIMIT"), relations);
    }
}
