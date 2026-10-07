package org.tbc.world.content.catalog;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Narrow read-only gameobject_template view over ObjectMgr (refactoring plan cycle 4.3). */
class GameObjectCatalogTest {
    @Test
    void gameObjectWhenLoadedEntryShouldReturnTemplate() {
        ObjectMgr m = new ObjectMgr();
        m.gameObjects.put(181582, new ObjectMgr.GameObjectTemplate(181582, 9, 7510, "Mailbox", "", "", "",
                null, 1.5f));
        GameObjectCatalog gameObjects = m;
        assertEquals("Mailbox", gameObjects.gameObject(181582).name);
    }

    @Test
    void gameObjectWhenUnknownEntryShouldReturnNull() {
        GameObjectCatalog gameObjects = new ObjectMgr();
        assertNull(gameObjects.gameObject(999_999));
    }
}
