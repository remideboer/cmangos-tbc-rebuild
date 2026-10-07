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

/** creature_template and loot tables loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class CreatureLoaderTest {
    @Test
    void loadWhenSimpleColumnsOnlyShouldFallBackAndIndexLootEquipmentAndMana() throws Exception {
        String url = "jdbc:h2:mem:creature_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "creature-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE creature_template (Entry INT, Name VARCHAR(64), ModelId1 INT, Faction INT, "
                        + "MinLevelHealth INT, MinLevel INT, NpcFlags INT, ScriptName VARCHAR(64), "
                        + "EquipmentTemplateId INT, MinLevelMana INT)");
                st.execute("INSERT INTO creature_template VALUES (6, 'Kobold Vermin', 10913, 7, 0, 1, 0, '', 0, 0)");
                st.execute("INSERT INTO creature_template VALUES (15274, 'Mana Wyrm', 15404, 7, 55, 1, 0, '', 3, 65)");
                st.execute("CREATE TABLE creature_equip_template (entry INT, equipentry1 INT, equipentry2 INT, "
                        + "equipentry3 INT)");
                st.execute("INSERT INTO creature_equip_template VALUES (3, 25, 0, 2362)");
                st.execute("CREATE TABLE creature_loot_template (entry INT, item INT, ChanceOrQuestChance FLOAT, "
                        + "mincountOrRef INT, maxcount INT)");
                st.execute("INSERT INTO creature_loot_template VALUES (6, 117, -100, 1, 1)");
                st.execute("INSERT INTO creature_loot_template VALUES (6, 24001, 100, -1, 1)");
                st.execute("INSERT INTO creature_loot_template VALUES (6, 2589, 50, 2, 1)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                CreatureLoader.load(m, c);
            }
            assertEquals("Kobold Vermin", m.creatures.get(6).name());
            assertEquals(1, m.creatures.get(6).hp(), "MinLevelHealth 0 clamps to 1");
            assertEquals(55, m.creatures.get(15274).hp());
            assertEquals(65, m.creatureMana.get(15274));
            assertFalse(m.creatureMana.containsKey(6));
            assertEquals(3, m.equipmentByEntry.get(15274));
            assertFalse(m.equipmentByEntry.containsKey(6));
            assertEquals(2362, m.equipmentItems.get(3)[2]);
            List<ObjectMgr.LootRow> loot = m.creatureLoot.get(6);
            assertEquals(2, loot.size(), "reference row (mincountOrRef < 0) is skipped");
            assertTrue(loot.get(0).needsQuest());
            assertEquals(100f, loot.get(0).chance());
            assertEquals(2, loot.get(1).maxCount(), "maxcount below mincount is raised");
            assertTrue(m.gameObjectLoot.isEmpty());
            assertTrue(m.modelCombatReach.isEmpty());
        }
    }

    @Test
    void loadWhenNoCreatureTablesShouldLeaveCreaturesEmpty() throws Exception {
        String url = "jdbc:h2:mem:creature_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "creature-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                CreatureLoader.load(m, c);
            }
            assertTrue(m.creatures.isEmpty());
            assertTrue(m.creatureLoot.isEmpty());
        }
    }
}
