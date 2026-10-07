package org.tbc.world.content.catalog;

import org.tbc.world.content.ObjectMgr;

/** Read-only quest_template view; the narrow interface handlers and Content depend on. */
public interface QuestCatalog {
    /** Template for {@code questId}, or {@code null} when unknown. */
    ObjectMgr.QuestTemplate quest(int questId);
}
