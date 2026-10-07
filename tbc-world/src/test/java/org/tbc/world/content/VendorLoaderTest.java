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

/** npc_vendor, npc_vendor_template and VendorTemplateId loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class VendorLoaderTest {
    @Test
    void loadWhenVendorTablesWithoutSlotShouldFallBackAndMergeTemplateStock() throws Exception {
        String url = "jdbc:h2:mem:vendor_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "vendor-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE npc_vendor (entry INT, item INT)");
                st.execute("INSERT INTO npc_vendor VALUES (" + Content.NPC_CORINA_STEELE + ", "
                        + Content.ITEM_WORN_SHORTSWORD + ")");
                st.execute("INSERT INTO npc_vendor VALUES (" + Content.NPC_CORINA_STEELE + ", "
                        + Content.ITEM_WORN_SHORTSWORD + ")");
                st.execute("INSERT INTO npc_vendor VALUES (" + Content.NPC_CORINA_STEELE + ", 0)");
                st.execute("CREATE TABLE npc_vendor_template (entry INT, item INT)");
                st.execute("INSERT INTO npc_vendor_template VALUES (5, " + Content.ITEM_TOUGH_JERKY + ")");
                st.execute("CREATE TABLE creature_template (Entry INT, VendorTemplateId INT)");
                st.execute("INSERT INTO creature_template VALUES (" + Content.NPC_CORINA_STEELE + ", 5)");
                st.execute("INSERT INTO creature_template VALUES (6, 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                VendorLoader.load(m, c);
            }
            assertEquals(List.of(Content.ITEM_WORN_SHORTSWORD), m.vendorItems.get(Content.NPC_CORINA_STEELE),
                    "duplicate and item 0 rows are dropped");
            assertEquals(List.of(Content.ITEM_TOUGH_JERKY), m.vendorTemplateItems.get(5));
            assertEquals(5, m.vendorTemplateId.get(Content.NPC_CORINA_STEELE));
            assertFalse(m.vendorTemplateId.containsKey(6));
            assertEquals(List.of(Content.ITEM_WORN_SHORTSWORD, Content.ITEM_TOUGH_JERKY),
                    m.itemsForVendor(Content.NPC_CORINA_STEELE));
        }
    }

    @Test
    void loadWhenNoVendorTablesShouldLeaveMapsEmpty() throws Exception {
        String url = "jdbc:h2:mem:vendor_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "vendor-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                VendorLoader.load(m, c);
            }
            assertTrue(m.vendorItems.isEmpty());
            assertTrue(m.vendorTemplateId.isEmpty());
        }
    }
}
