package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterStoreRenameTest {
    @Test
    void renameAtLoginWhenFlagSetShouldUpdateNameAndClearFlag() {
        CharacterStore store = new CharacterStore(null);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = store.create(1, "Oldname", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        p.atLogin |= Player.AT_LOGIN_RENAME;
        store.save(p);
        assertTrue(store.renameAtLogin(1, p.guid, "Newname"));
        Player listed = store.enumAccount(1, mgr).get(0);
        assertEquals("Newname", listed.name);
        assertEquals(0, listed.atLogin & Player.AT_LOGIN_RENAME);
    }

    @Test
    void renameAtLoginWhenNameTakenShouldReturnFalse() {
        CharacterStore store = new CharacterStore(null);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        store.create(1, "Takenone", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        Player p = store.create(1, "Renamer", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        p.atLogin |= Player.AT_LOGIN_RENAME;
        store.save(p);
        assertFalse(store.renameAtLogin(1, p.guid, "Takenone"));
        assertEquals("Renamer", store.enumAccount(1, mgr).stream()
                .filter(c -> Guid.low(c.guid) == Guid.low(p.guid)).findFirst().orElseThrow().name);
    }

    @Test
    void renameAtLoginWhenSqlMissingTableAndNoSnapshotShouldReturnFalse() {
        String url = "jdbc:h2:mem:rename_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "rename-test")) {
            CharacterStore store = new CharacterStore(chars);
            assertFalse(store.renameAtLogin(1, Guid.player(1), "Newname"));
        }
    }
}
