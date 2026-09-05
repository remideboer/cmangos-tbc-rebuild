package org.tbc.world.content;

import org.tbc.common.DbPool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMgrVendorTest {
    @Test
    void loadWhenNpcVendorRowsShouldIndexItemsByEntry() throws Exception {
        String url = "jdbc:h2:mem:vendor_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "vendor-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE npc_vendor (
                          entry INT,
                          item INT,
                          slot INT
                        )
                        """);
                st.execute("INSERT INTO npc_vendor (entry, item, slot) VALUES (9002, 117, 0)");
                st.execute("INSERT INTO npc_vendor (entry, item, slot) VALUES (9002, 0, 1)");
                st.execute("INSERT INTO npc_vendor (entry, item, slot) VALUES (9002, 117, 2)");
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            assertEquals(List.of(117), mgr.vendorItems.get(9002));
            assertTrue(mgr.vendorItems.get(Content.NPC_CORINA_STEELE).contains(Content.ITEM_WORN_SHORTSWORD));
        }
    }
}
