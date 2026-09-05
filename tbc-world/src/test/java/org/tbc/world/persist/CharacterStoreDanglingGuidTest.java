package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CharacterStoreDanglingGuidTest {
    @Test
    void constructorWhenDanglingItemRefsShouldDeleteRowsAtOrAboveNextGuid() throws Exception {
        String url = "jdbc:h2:mem:dang_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "dangling-guid-test")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
                st.execute("INSERT INTO item_instance (guid) VALUES (10)");
                st.execute("""
                        CREATE TABLE character_inventory (
                          guid INT, bag INT, slot INT, item INT PRIMARY KEY, item_template INT)
                        """);
                st.execute("INSERT INTO character_inventory VALUES (1,0,23,10,25)");
                st.execute("INSERT INTO character_inventory VALUES (1,0,24,11,25)");
                st.execute("""
                        CREATE TABLE mail_items (
                          mail_id INT, item_guid INT, item_template INT, receiver INT,
                          PRIMARY KEY (mail_id, item_guid))
                        """);
                st.execute("INSERT INTO mail_items VALUES (1,12,25,1)");
                st.execute("""
                        CREATE TABLE auction (
                          id INT PRIMARY KEY, itemguid INT)
                        """);
                st.execute("INSERT INTO auction VALUES (1,13)");
                st.execute("""
                        CREATE TABLE guild_bank_item (
                          guildid INT, TabId INT, SlotId INT, item_guid INT, item_entry INT,
                          PRIMARY KEY (guildid, TabId, SlotId))
                        """);
                st.execute("INSERT INTO guild_bank_item VALUES (1,0,0,14,25)");
            }
            CharacterStore store = new CharacterStore(chars);
            assertEquals(11, store.nextItemGuid());
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                assertEquals(1, count(st, "SELECT COUNT(*) FROM character_inventory"));
                assertEquals(10, scalar(st, "SELECT item FROM character_inventory"));
                assertEquals(0, count(st, "SELECT COUNT(*) FROM mail_items"));
                assertEquals(0, count(st, "SELECT COUNT(*) FROM auction"));
                assertEquals(0, count(st, "SELECT COUNT(*) FROM guild_bank_item"));
            }
        }
    }

    @Test
    void constructorWhenItemTablesMissingShouldNotThrow() {
        String url = "jdbc:h2:mem:dang_miss_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        assertDoesNotThrow(() -> {
            try (DbPool chars = new DbPool(url, "sa", "", "dangling-missing-test")) {
                new CharacterStore(chars);
            }
        });
    }

    @Test
    void constructorWhenOnlyItemInstanceShouldAllocateNextAndIgnoreMissingRefTables() throws Exception {
        String url = "jdbc:h2:mem:dang_only_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "dangling-only-instance")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
                st.execute("INSERT INTO item_instance (guid) VALUES (5)");
            }
            CharacterStore store = new CharacterStore(chars);
            assertEquals(6, store.nextItemGuid());
        }
    }

    private static int count(Statement st, String sql) throws Exception {
        return scalar(st, sql);
    }

    private static int scalar(Statement st, String sql) throws Exception {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
