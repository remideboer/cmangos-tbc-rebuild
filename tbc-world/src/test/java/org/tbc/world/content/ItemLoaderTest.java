package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** item_template loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class ItemLoaderTest {
    private static final String BASE_COLUMNS = "entry INT, class INT, subclass INT, name VARCHAR(255), displayid INT, "
            + "Quality INT, Flags INT, BuyPrice INT, SellPrice INT, InventoryType INT, AllowableClass INT, "
            + "AllowableRace INT, ItemLevel INT, RequiredLevel INT, maxcount INT, stackable INT, ContainerSlots INT, "
            + "armor INT, delay INT, bonding INT, description VARCHAR(255), MaxDurability INT, Duration INT, "
            + "RequiredDisenchantSkill INT";

    @Test
    void loadWhenBaseColumnsOnlyShouldFallBackToShortSelectAndMergeSheathAndSpells() throws Exception {
        String url = "jdbc:h2:mem:item_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE item_template (" + BASE_COLUMNS + ", sheath INT, "
                        + "spellid_1 INT, spelltrigger_1 INT, spellcharges_1 INT, "
                        + "spellid_2 INT, spelltrigger_2 INT, spellcharges_2 INT, "
                        + "spellid_3 INT, spelltrigger_3 INT, spellcharges_3 INT, "
                        + "spellid_4 INT, spelltrigger_4 INT, spellcharges_4 INT, "
                        + "spellid_5 INT, spelltrigger_5 INT, spellcharges_5 INT)");
                st.execute("INSERT INTO item_template VALUES (25, 2, 7, 'Worn Shortsword', 1542, 1, 0, 35, 7, 21, "
                        + "32767, 511, 2, 1, 0, 0, 0, 0, 1900, 0, '', 20, 0, -1, 3, "
                        + "8690, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                ItemLoader.load(m, c);
            }
            ObjectMgr.ItemTemplate sword = m.items.get(25);
            assertEquals("Worn Shortsword", sword.name);
            assertEquals(1, sword.stackable, "stackable 0 clamps to 1");
            assertEquals(-1, sword.unk);
            assertEquals(3, sword.sheath);
            assertEquals(8690, sword.spellId[0]);
            assertEquals(-1, sword.spellCharges[0]);
        }
    }

    @Test
    void loadWhenNoItemTableShouldLeaveItemsEmpty() throws Exception {
        String url = "jdbc:h2:mem:item_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                ItemLoader.load(m, c);
            }
            assertTrue(m.items.isEmpty());
        }
    }
}
