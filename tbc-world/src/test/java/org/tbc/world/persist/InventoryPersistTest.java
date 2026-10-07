package org.tbc.world.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;

/** character_inventory / item_instance rows owned by InventoryPersist, split out of CharacterStore (plan cycle 5.3). */
class InventoryPersistTest {

    @Test
    void writeThenLoadShouldRestoreBagSlotCountAndDurability() throws Exception {
        String url = "jdbc:h2:mem:ip_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE character_inventory (guid INT, bag INT, slot INT, item INT, item_template INT)");
                st.execute("""
                        CREATE TABLE item_instance (
                          guid INT PRIMARY KEY, owner_guid INT, itemEntry INT, creatorGuid INT, giftCreatorGuid INT,
                          count INT, duration INT, charges VARCHAR(32), flags INT, enchantments VARCHAR(64),
                          randomPropertyId INT, durability INT, itemTextId INT)
                        """);
            }
            Player p = new Player();
            p.guid = 11;
            Item it = new Item(501, 2362);
            it.bag = 0;
            it.slot = 23;
            it.count = 3;
            it.durability = 17;
            p.items.put(501, it);

            InventoryPersist.write(c, p);
            Player loaded = new Player();
            loaded.guid = 11;
            InventoryPersist.load(c, loaded);

            Item back = loaded.items.get(501);
            assertEquals(2362, back.entry);
            assertEquals(23, back.slot);
            assertEquals(3, back.count);
            assertEquals(17, back.durability);
            assertEquals(11, back.ownerGuid);
        }
    }

    @Test
    void parseItemDataWhenLegacyBlobShouldSetEntryAndCount() {
        Item it = new Item(1, 0);

        InventoryPersist.parseItemData(it, "2362:4");

        assertEquals(2362, it.entry);
        assertEquals(4, it.count);
    }
}
