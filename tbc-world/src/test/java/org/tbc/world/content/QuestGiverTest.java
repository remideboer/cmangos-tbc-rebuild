package org.tbc.world.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Quest-giver relations owned by the QuestGiver split out of Content (plan cycle 5.2). */
class QuestGiverTest {

    @Test
    void givesWhenSeededGiverRelationShouldBeTrue() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestGiver quests = new QuestGiver(mgr);
        Map.Entry<Integer, java.util.List<Integer>> relation = mgr.questGivers.entrySet().iterator().next();

        assertTrue(quests.gives(relation.getKey(), relation.getValue().get(0)));
    }

    @Test
    void givesWhenUnknownEntryShouldBeFalse() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestGiver quests = new QuestGiver(mgr);

        assertFalse(quests.gives(424242, 1));
    }
}
