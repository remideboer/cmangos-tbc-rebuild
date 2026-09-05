package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterStoreDeleteOldTest {
    @Test
    void deleteOldCharactersWhenKeepDaysZeroShouldLeaveRow() {
        CharacterStore store = new CharacterStore(null);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = store.create(1, "Kept", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        store.markDeleted(p.guid, 1);
        store.deleteOldCharacters(System.currentTimeMillis(), 0);
        assertEquals(1, store.storedCount(1));
    }

    @Test
    void deleteOldCharactersWhenSqlMissingTableShouldCatch() {
        String url = "jdbc:h2:mem:delold_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        assertDoesNotThrow(() -> {
            try (DbPool chars = new DbPool(url, "sa", "", "delete-old-test")) {
                CharacterStore store = new CharacterStore(chars);
                store.deleteOldCharacters(System.currentTimeMillis(), 30);
            }
        });
    }
}
