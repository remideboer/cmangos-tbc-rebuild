package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** playercreateinfo tables loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class PlayerCreateLoaderTest {
    @Test
    void loadWhenCreateTablesPresentShouldIndexInfoSpellsActionsSkillsAndItems() throws Exception {
        String url = "jdbc:h2:mem:create_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "create-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE playercreateinfo (race INT, class INT, map INT, zone INT, position_x FLOAT, "
                        + "position_y FLOAT, position_z FLOAT, orientation FLOAT)");
                st.execute("INSERT INTO playercreateinfo VALUES (1, 1, 0, 12, -8949.95, -132.493, 83.5312, 0)");
                st.execute("CREATE TABLE playercreateinfo_spell (race INT, class INT, Spell INT)");
                st.execute("INSERT INTO playercreateinfo_spell VALUES (1, 1, 6603)");
                st.execute("CREATE TABLE playercreateinfo_action (race INT, class INT, button INT, action INT, type INT)");
                st.execute("INSERT INTO playercreateinfo_action VALUES (1, 1, 72, 6603, 0)");
                st.execute("CREATE TABLE playercreateinfo_skills (raceMask INT, classMask INT, skill INT, step INT)");
                st.execute("INSERT INTO playercreateinfo_skills VALUES (1, 1, 26, 0)");
                st.execute("CREATE TABLE playercreateinfo_item (race INT, class INT, itemid INT, amount INT)");
                st.execute("INSERT INTO playercreateinfo_item VALUES (1, 1, 25, 1)");
                st.execute("INSERT INTO playercreateinfo_item VALUES (1, 1, 117, 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                PlayerCreateLoader.load(m, c);
            }
            assertEquals(12, m.createInfo.get(ObjectMgr.key(1, 1)).zone());
            assertEquals(List.of(6603), m.createSpells.get((int) ObjectMgr.key(1, 1)));
            assertEquals(6603, m.createActions.get((int) ObjectMgr.key(1, 1))[72]);
            assertEquals(26, m.createSkills.get(0).skill());
            assertEquals(1, m.createItems.get((int) ObjectMgr.key(1, 1)).size(), "amount 0 rows are skipped");
            assertEquals(25, m.createItems.get((int) ObjectMgr.key(1, 1)).get(0).itemId());
        }
    }

    @Test
    void loadWhenNoPlayerCreateInfoShouldThrowSoObjectMgrFallsBackToSeeds() throws Exception {
        String url = "jdbc:h2:mem:create_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "create-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            boolean threw = false;
            try (Connection c = worldDb.get()) {
                PlayerCreateLoader.load(m, c);
            } catch (Exception e) {
                threw = true;
            }
            assertTrue(threw);
            assertTrue(m.createInfo.isEmpty());
            assertFalse(m.createActions.containsKey((int) ObjectMgr.key(1, 1)));
        }
    }
}
