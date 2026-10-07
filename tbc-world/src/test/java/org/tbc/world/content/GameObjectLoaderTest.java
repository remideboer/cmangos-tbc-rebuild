package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** gameobject_template and page_text loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class GameObjectLoaderTest {
    @Test
    void loadWhenTablesPresentShouldIndexTemplatesAndPages() throws Exception {
        String url = "jdbc:h2:mem:go_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "go-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                StringBuilder ddl = new StringBuilder("CREATE TABLE gameobject_template (entry INT, type INT, "
                        + "displayId INT, name VARCHAR(64), IconName VARCHAR(64), OpeningText VARCHAR(64), "
                        + "ClosingText VARCHAR(64), size FLOAT");
                StringBuilder row = new StringBuilder("INSERT INTO gameobject_template VALUES (181582, 9, 7510, "
                        + "'Mailbox', '', '', '', 1.5");
                for (int i = 0; i < 24; i++) {
                    ddl.append(", data").append(i).append(" INT");
                    row.append(", ").append(i == 3 ? 42 : 0);
                }
                st.execute(ddl.append(")").toString());
                st.execute(row.append(")").toString());
                st.execute("CREATE TABLE page_text (entry INT, text VARCHAR(255), next_page INT)");
                st.execute("INSERT INTO page_text VALUES (1, 'Page one', 2)");
                st.execute("INSERT INTO page_text VALUES (2, NULL, 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                GameObjectLoader.load(m, c);
            }
            ObjectMgr.GameObjectTemplate mailbox = m.gameObjects.get(181582);
            assertEquals("Mailbox", mailbox.name);
            assertEquals(1.5f, mailbox.size);
            assertEquals(42, mailbox.data[3]);
            assertEquals(2, m.pageTexts.get(1).nextPage());
            assertEquals("", m.pageTexts.get(2).text(), "NULL text reads as empty");
        }
    }

    @Test
    void loadWhenNoTablesShouldLeaveMapsEmpty() throws Exception {
        String url = "jdbc:h2:mem:go_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "go-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                GameObjectLoader.load(m, c);
            }
            assertTrue(m.gameObjects.isEmpty());
            assertTrue(m.pageTexts.isEmpty());
        }
    }
}
