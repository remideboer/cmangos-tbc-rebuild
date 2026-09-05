package org.tbc.world.content;

import org.tbc.common.DbPool;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMgrGossipTest {
    @Test
    void gossipOptionsForWhenVendorStockShouldIncludeVendorIcon() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(Content.NPC_CORINA_STEELE, 0, 0, 0, 0, 0, null);
        Player p = new Player();
        p.clazz = 1;
        List<ObjectMgr.GossipMenuItem> items = mgr.gossipOptionsFor(p, c);
        assertTrue(items.stream().anyMatch(it -> it.icon() == Content.GOSSIP_ICON_VENDOR && it.optionId() == 3));
        assertEquals(0, mgr.gossipMenuId(Content.NPC_CORINA_STEELE));
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, mgr.gossipTextId(0));
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, mgr.gossipTextId(99));
        assertTrue(mgr.gossipOptionsFor(p, null).isEmpty());
        Creature inn = mgr.spawnCreature(Content.NPC_INNKEEPER_FARLEY, 0, 0, 0, 0, 0, null);
        assertTrue(mgr.gossipOptionsFor(p, inn).stream()
                .anyMatch(it -> Content.GOSSIP_FARLEY_INN_INFO.equals(it.text())));
        assertEquals(Content.GOSSIP_MENU_FARLEY, mgr.gossipMenuId(Content.NPC_INNKEEPER_FARLEY));
        assertEquals(Content.GOSSIP_TEXT_FARLEY, mgr.gossipTextId(Content.GOSSIP_MENU_FARLEY));
        assertEquals(Content.GOSSIP_TEXT_FARLEY_INN_INFO, mgr.gossipTextId(Content.GOSSIP_MENU_FARLEY_INN_INFO));
        assertTrue(mgr.gossipOptionsFor(p, inn, Content.GOSSIP_MENU_FARLEY_INN_INFO).isEmpty());
    }

    @Test
    void gossipOptionsForWhenConditionIdShouldOmitOption() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                mgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 95, 0, "Locked",
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", 0, 0, 1));
        mgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
        Creature inn = mgr.spawnCreature(Content.NPC_INNKEEPER_FARLEY, 0, 0, 0, 0, 0, null);
        Player p = new Player();
        assertTrue(mgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY).stream()
                .anyMatch(it -> "Locked".equals(it.text()) && it.conditionId() == 1));
        assertTrue(mgr.gossipOptionsFor(p, inn).stream().noneMatch(it -> "Locked".equals(it.text())));
    }

    @Test
    void loadWhenGossipTablesShouldIndexTextOptionsAndMenuId() throws Exception {
        String url = "jdbc:h2:mem:gossip_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "gossip-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE gossip_menu (
                          entry INT,
                          text_id INT,
                          script_id INT,
                          condition_id INT
                        )
                        """);
                st.execute("INSERT INTO gossip_menu VALUES (42, 500, 0, 0)");
                st.execute("INSERT INTO gossip_menu VALUES (42, 501, 0, 1)");
                st.execute("""
                        CREATE TABLE gossip_menu_option (
                          menu_id INT,
                          id INT,
                          option_icon INT,
                          option_text VARCHAR(64),
                          option_id INT,
                          npc_option_npcflag INT,
                          action_menu_id INT,
                          box_coded INT,
                          box_money INT,
                          box_text VARCHAR(64)
                        )
                        """);
                st.execute("INSERT INTO gossip_menu_option VALUES (42, 0, 1, 'GOSSIP_OPTION_VENDOR', 3, 128, 0, 0, 0, '')");
                st.execute("""
                        CREATE TABLE creature_template (
                          Entry INT,
                          GossipMenuId INT
                        )
                        """);
                st.execute("INSERT INTO creature_template VALUES (9002, 42)");
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            assertEquals(42, mgr.gossipMenuId(9002));
            assertEquals(500, mgr.gossipTextId(42));
            assertEquals(1, mgr.gossipOptions.get(42).get(0).icon());
            assertEquals(0, mgr.gossipOptions.get(42).get(0).actionMenu());
            assertEquals(0, mgr.gossipOptions.get(42).get(0).actionPoi());
            assertTrue(mgr.vendorItems.get(Content.NPC_CORINA_STEELE).contains(Content.ITEM_WORN_SHORTSWORD));
        }
    }

    @Test
    void loadWhenPointsOfInterestAndActionPoiShouldIndexBoth() throws Exception {
        String url = "jdbc:h2:mem:gossip_poi_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        ObjectMgr.PointOfInterest inn = ObjectMgr.lionsPrideInnPoi();
        try (DbPool worldDb = new DbPool(url, "sa", "", "gossip-poi-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE points_of_interest (
                          entry INT,
                          x FLOAT,
                          y FLOAT,
                          icon INT,
                          flags INT,
                          data INT,
                          icon_name VARCHAR(64)
                        )
                        """);
                st.execute("INSERT INTO points_of_interest VALUES (" + inn.entry() + ", " + inn.x() + ", "
                        + inn.y() + ", " + inn.icon() + ", " + inn.flags() + ", " + inn.data()
                        + ", 'Lion''s Pride Inn')");
                st.execute("INSERT INTO points_of_interest VALUES (2, 99999, 0, 0, 0, 0, 'bad')");
                st.execute("""
                        CREATE TABLE gossip_menu (
                          entry INT,
                          text_id INT,
                          script_id INT,
                          condition_id INT
                        )
                        """);
                st.execute("INSERT INTO gossip_menu VALUES (42, 500, 0, 0)");
                st.execute("""
                        CREATE TABLE gossip_menu_option (
                          menu_id INT,
                          id INT,
                          option_icon INT,
                          option_text VARCHAR(64),
                          option_id INT,
                          npc_option_npcflag INT,
                          action_menu_id INT,
                          action_poi_id INT,
                          box_coded INT,
                          box_money INT,
                          box_text VARCHAR(64)
                        )
                        """);
                st.execute("INSERT INTO gossip_menu_option VALUES (42, 0, 0, 'Inn', 1, 1, 0, "
                        + inn.entry() + ", 0, 0, '')");
                st.execute("INSERT INTO gossip_menu_option VALUES (42, 1, 0, 'Ghost', 1, 1, 0, 99, 0, 0, '')");
                st.execute("""
                        CREATE TABLE creature_template (
                          Entry INT,
                          GossipMenuId INT
                        )
                        """);
                st.execute("INSERT INTO creature_template VALUES (9002, 42)");
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            assertEquals(inn.entry(), mgr.pointsOfInterest.get(inn.entry()).entry());
            assertEquals(inn.x(), mgr.pointsOfInterest.get(inn.entry()).x(), 0.0001f);
            assertEquals(inn.y(), mgr.pointsOfInterest.get(inn.entry()).y(), 0.0001f);
            assertEquals(inn.icon(), mgr.pointsOfInterest.get(inn.entry()).icon());
            assertEquals(inn.flags(), mgr.pointsOfInterest.get(inn.entry()).flags());
            assertEquals(inn.data(), mgr.pointsOfInterest.get(inn.entry()).data());
            assertEquals(inn.iconName(), mgr.pointsOfInterest.get(inn.entry()).iconName());
            assertFalse(mgr.pointsOfInterest.containsKey(2));
            assertEquals(inn.entry(), mgr.gossipOptions.get(42).get(0).actionPoi());
            assertEquals(0, mgr.gossipOptions.get(42).get(1).actionPoi());
        }
    }

    @Test
    void loadWhenGossipOptionConditionIdShouldStore() throws Exception {
        String url = "jdbc:h2:mem:gossip_cond_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "gossip-cond-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE gossip_menu (
                          entry INT,
                          text_id INT,
                          script_id INT,
                          condition_id INT
                        )
                        """);
                st.execute("INSERT INTO gossip_menu VALUES (42, 500, 0, 0)");
                st.execute("""
                        CREATE TABLE gossip_menu_option (
                          menu_id INT,
                          id INT,
                          option_icon INT,
                          option_text VARCHAR(64),
                          option_id INT,
                          npc_option_npcflag INT,
                          action_menu_id INT,
                          action_poi_id INT,
                          box_coded INT,
                          box_money INT,
                          box_text VARCHAR(64),
                          condition_id INT
                        )
                        """);
                st.execute("INSERT INTO gossip_menu_option VALUES (42, 0, 0, 'Locked', 1, 1, 0, 0, 0, 0, '', 1)");
                st.execute("INSERT INTO gossip_menu_option VALUES (42, 1, 0, 'Open', 1, 1, 0, 0, 0, 0, '', 0)");
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            assertEquals(1, mgr.gossipOptions.get(42).get(0).conditionId());
            assertEquals(0, mgr.gossipOptions.get(42).get(1).conditionId());
        }
    }

    @Test
    void loadWhenNpcTextRowsShouldIndexSlotsAndSkipIdZero() throws Exception {
        String url = "jdbc:h2:mem:npctext_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "npc-text-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute(npcTextDdl());
                st.execute("INSERT INTO npc_text (ID) VALUES (0)");
                st.execute("INSERT INTO npc_text (ID, text0_0, text0_1, lang0, prob0, em0_0) "
                        + "VALUES (42, 'Hello there', '', 1, 1, 9)");
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            assertTrue(mgr.npcTexts.containsKey(42));
            assertFalse(mgr.npcTexts.containsKey(0));
            ObjectMgr.NpcTextSlot slot = mgr.npcTexts.get(42).slots()[0];
            assertEquals("Hello there", slot.text0());
            assertEquals(1, slot.language());
            assertEquals(1f, slot.probability());
            assertEquals(9, slot.emotes()[0]);
        }
    }

    private static String npcTextDdl() {
        StringBuilder sql = new StringBuilder("CREATE TABLE npc_text (ID INT");
        for (int i = 0; i < Content.MAX_GOSSIP_TEXT_OPTIONS; i++) {
            sql.append(", text").append(i).append("_0 VARCHAR(64) DEFAULT ''");
            sql.append(", text").append(i).append("_1 VARCHAR(64) DEFAULT ''");
            sql.append(", lang").append(i).append(" INT DEFAULT 0");
            sql.append(", prob").append(i).append(" FLOAT DEFAULT 0");
            for (int e = 0; e < 6; e++) {
                sql.append(", em").append(i).append("_").append(e).append(" INT DEFAULT 0");
            }
        }
        return sql.append(")").toString();
    }
}
