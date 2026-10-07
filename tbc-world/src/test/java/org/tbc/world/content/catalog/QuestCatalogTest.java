package org.tbc.world.content.catalog;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Narrow read-only quest view over ObjectMgr (refactoring plan cycle 4.3). */
class QuestCatalogTest {
    @Test
    void questWhenSeededIdShouldReturnTemplate() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        QuestCatalog quests = m;
        assertEquals("A Threat Within", quests.quest(Content.QUEST_A_THREAT_WITHIN).title());
    }

    @Test
    void questWhenUnknownIdShouldReturnNull() {
        QuestCatalog quests = new ObjectMgr();
        assertNull(quests.quest(999_999));
    }
}
