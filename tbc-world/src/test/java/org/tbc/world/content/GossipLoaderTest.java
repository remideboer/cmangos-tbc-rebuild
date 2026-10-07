package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gossip tables loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class GossipLoaderTest {
    @Test
    void loadWhenGossipTablesPresentShouldIndexMenusOptionsAndMenuIds() throws Exception {
        String url = "jdbc:h2:mem:gossip_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "gossip-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE gossip_menu (entry INT, text_id INT, script_id INT, condition_id INT)");
                st.execute("INSERT INTO gossip_menu VALUES (42, 500, 0, 0)");
                st.execute("INSERT INTO gossip_menu VALUES (42, 501, 0, 1)");
                st.execute("CREATE TABLE gossip_menu_option (menu_id INT, id INT, option_icon INT, "
                        + "option_text VARCHAR(64), option_id INT, npc_option_npcflag INT)");
                st.execute("INSERT INTO gossip_menu_option VALUES (42, 0, 1, 'GOSSIP_OPTION_VENDOR', 3, 128)");
                st.execute("CREATE TABLE creature_template (Entry INT, GossipMenuId INT)");
                st.execute("INSERT INTO creature_template VALUES (9002, 42)");
                st.execute("INSERT INTO creature_template VALUES (9003, 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                GossipLoader.load(m, c);
            }
            assertEquals(42, m.gossipMenuId(9002));
            assertEquals(0, m.gossipMenuId(9003));
            assertEquals(500, m.gossipTextId(42));
            assertEquals(1, m.gossipOptions.get(42).get(0).icon());
            assertEquals("GOSSIP_OPTION_VENDOR", m.gossipOptions.get(42).get(0).text());
            assertTrue(m.npcTexts.isEmpty());
            assertTrue(m.pointsOfInterest.isEmpty());
        }
    }

    @Test
    void loadWhenNoGossipTablesShouldLeaveMapsEmpty() throws Exception {
        String url = "jdbc:h2:mem:gossip_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "gossip-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                GossipLoader.load(m, c);
            }
            assertTrue(m.gossipMenuIds.isEmpty());
            assertTrue(m.gossipTextIds.isEmpty());
            assertFalse(m.gossipOptions.containsKey(42));
        }
    }
}
