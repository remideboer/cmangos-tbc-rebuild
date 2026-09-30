package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterStoreAccountDataTest {
    @Test
    void saveWhenInMemoryShouldReloadGlobalAndCharacterSlots() {
        CharacterStore store = new CharacterStore(null);
        store.saveAccountData(true, 9, 0, 0L, "global-cfg");
        store.saveAccountData(false, 42, 6, 0L, "layout");
        List<CharacterStore.AccountDataRow> global = store.loadAccountData(true, 9);
        assertEquals(1, global.size());
        assertEquals(0, global.get(0).type());
        assertEquals("global-cfg", global.get(0).data());
        List<CharacterStore.AccountDataRow> perChar = store.loadAccountData(false, 42);
        assertEquals(1, perChar.size());
        assertEquals(6, perChar.get(0).type());
        assertEquals("layout", perChar.get(0).data());
    }

    @Test
    void saveWhenDatabaseConnectedShouldReloadFromSql() throws Exception {
        String url = "jdbc:h2:mem:acctdata_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "account-data-test")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE account_data (
                          account INT, type INT, time BIGINT, data BLOB,
                          PRIMARY KEY (account, type))
                        """);
                st.execute("""
                        CREATE TABLE character_account_data (
                          guid INT, type INT, time BIGINT, data BLOB,
                          PRIMARY KEY (guid, type))
                        """);
                st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
                st.execute("""
                        CREATE TABLE characters (
                          guid INT PRIMARY KEY, account INT, name VARCHAR(12), race INT, class INT, gender INT,
                          level INT, xp INT, money INT, playerBytes INT, playerBytes2 INT, playerFlags INT,
                          position_x FLOAT, position_y FLOAT, position_z FLOAT, map INT, dungeon_difficulty INT,
                          orientation FLOAT, online INT, cinematic INT, totaltime INT, leveltime INT,
                          logout_time BIGINT, is_logout_resting INT, rest_bonus FLOAT, zone INT, at_login INT,
                          health INT, power1 INT, power2 INT, power3 INT, power4 INT, power5 INT,
                          watchedFaction BIGINT, deleteDate BIGINT)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore store = new CharacterStore(chars);
            Player p = store.create(3, "Prefs", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
            store.saveAccountData(true, 3, 2, 0L, "bindings");
            store.saveAccountData(false, Guid.low(p.guid), 6, 0L, "ui-layout");
            CharacterStore again = new CharacterStore(chars);
            List<CharacterStore.AccountDataRow> global = again.loadAccountData(true, 3);
            assertTrue(global.stream().anyMatch(r -> r.type() == 2 && "bindings".equals(r.data())));
            List<CharacterStore.AccountDataRow> perChar = again.loadAccountData(false, Guid.low(p.guid));
            assertTrue(perChar.stream().anyMatch(r -> r.type() == 6 && "ui-layout".equals(r.data())));
        }
    }

    @Test
    void saveWhenPerCharacterAndGuidZeroShouldSkip() {
        CharacterStore store = new CharacterStore(null);
        store.saveAccountData(false, 0, 6, 0L, "nope");
        assertTrue(store.loadAccountData(false, 0).isEmpty());
    }
}
