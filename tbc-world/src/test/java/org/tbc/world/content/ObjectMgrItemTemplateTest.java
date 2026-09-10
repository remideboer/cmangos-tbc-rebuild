package org.tbc.world.content;

import org.tbc.common.DbPool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ObjectMgrItemTemplateTest {
    /**
     * TP-SL14-013 — ObjectMgr::LoadItemPrototypes. SQL item_template must carry dmg_min1 and
     * stat_type1/stat_value1 so equipped mods are not stuck at the Java defaults (0).
     */
    @Test
    void loadItemsWhenTemplateHasWeaponAndVestShouldCarryDmgMin1AndStamina() throws Exception {
        String url = "jdbc:h2:mem:items_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE item_template (
                          entry INT,
                          class INT,
                          subclass INT,
                          name VARCHAR(255),
                          displayid INT,
                          Quality INT,
                          Flags INT,
                          BuyPrice INT,
                          SellPrice INT,
                          InventoryType INT,
                          AllowableClass INT,
                          AllowableRace INT,
                          ItemLevel INT,
                          RequiredLevel INT,
                          maxcount INT,
                          stackable INT,
                          ContainerSlots INT,
                          armor INT,
                          delay INT,
                          bonding INT,
                          description VARCHAR(255),
                          MaxDurability INT,
                          Duration INT,
                          RequiredDisenchantSkill INT,
                          dmg_min1 FLOAT,
                          dmg_max1 FLOAT,
                          stat_type1 INT,
                          stat_value1 INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1)
                        VALUES (25, 2, 7, 'Worn Shortsword', 1542, 1, 0, 35, 7, 21, 32767, 511, 2, 1, 0,
                          1, 0, 0, 1900, 0, '', 20, 0, -1, 1, 3, 0, 0)
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1)
                        VALUES (821, 4, 2, 'Riverpaw Leather Vest', 17102, 2, 0, 0, 0, 5, -1, -1, 13, 1, 0,
                          1, 0, 65, 0, 2, '', 60, 0, -1, 0, 0, 7, 2)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate sword = mgr.items.get(Content.ITEM_WORN_SHORTSWORD);
            assertNotNull(sword);
            assertEquals(1f, sword.dmgMin[0]);
            assertEquals(3f, sword.dmgMax[0]);
            ObjectMgr.ItemTemplate vest = mgr.items.get(Content.ITEM_RIVERPAW_LEATHER_VEST);
            assertNotNull(vest);
            assertEquals(7, vest.statType[0]);
            assertEquals(2, vest.statValue[0]);
            assertEquals(65, vest.armor);
        }
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes extra proto stats. SQL item_template must carry
     * stat_type2/stat_value2 (Tunic of Westfall 2041 AGILITY then STAMINA).
     */
    @Test
    void loadItemsWhenTemplateHasSecondStatSlotShouldCarryAgilityAndStamina() throws Exception {
        String url = "jdbc:h2:mem:items2_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-stat2-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE item_template (
                          entry INT,
                          class INT,
                          subclass INT,
                          name VARCHAR(255),
                          displayid INT,
                          Quality INT,
                          Flags INT,
                          BuyPrice INT,
                          SellPrice INT,
                          InventoryType INT,
                          AllowableClass INT,
                          AllowableRace INT,
                          ItemLevel INT,
                          RequiredLevel INT,
                          maxcount INT,
                          stackable INT,
                          ContainerSlots INT,
                          armor INT,
                          delay INT,
                          bonding INT,
                          description VARCHAR(255),
                          MaxDurability INT,
                          Duration INT,
                          RequiredDisenchantSkill INT,
                          dmg_min1 FLOAT,
                          dmg_max1 FLOAT,
                          stat_type1 INT,
                          stat_value1 INT,
                          stat_type2 INT,
                          stat_value2 INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2)
                        VALUES (2041, 4, 2, 'Tunic of Westfall', 0, 3, 0, 0, 1412, 5, -1, -1, 24, 0, 0,
                          1, 0, 92, 0, 1, '', 90, 0, -1, 0, 0, 3, 11, 7, 5)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate tunic = mgr.items.get(Content.ITEM_TUNIC_OF_WESTFALL);
            assertNotNull(tunic);
            assertEquals(3, tunic.statType[0]);
            assertEquals(11, tunic.statValue[0]);
            assertEquals(7, tunic.statType[1]);
            assertEquals(5, tunic.statValue[1]);
            assertEquals(92, tunic.armor);
        }
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes extra proto stats. SQL must carry stat_type3
     * (Blackened Defias Armor 10399 STR / AGI / STA).
     */
    @Test
    void loadItemsWhenTemplateHasThirdStatSlotShouldCarryStrengthAgilityStamina() throws Exception {
        String url = "jdbc:h2:mem:items3_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-stat3-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE item_template (
                          entry INT,
                          class INT,
                          subclass INT,
                          name VARCHAR(255),
                          displayid INT,
                          Quality INT,
                          Flags INT,
                          BuyPrice INT,
                          SellPrice INT,
                          InventoryType INT,
                          AllowableClass INT,
                          AllowableRace INT,
                          ItemLevel INT,
                          RequiredLevel INT,
                          maxcount INT,
                          stackable INT,
                          ContainerSlots INT,
                          armor INT,
                          delay INT,
                          bonding INT,
                          description VARCHAR(255),
                          MaxDurability INT,
                          Duration INT,
                          RequiredDisenchantSkill INT,
                          dmg_min1 FLOAT,
                          dmg_max1 FLOAT,
                          stat_type1 INT,
                          stat_value1 INT,
                          stat_type2 INT,
                          stat_value2 INT,
                          stat_type3 INT,
                          stat_value3 INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2, stat_type3, stat_value3)
                        VALUES (10399, 4, 2, 'Blackened Defias Armor', 0, 3, 0, 0, 1467, 5, -1, -1, 24, 19, 0,
                          1, 0, 92, 0, 1, '', 90, 0, -1, 0, 0, 4, 4, 3, 3, 7, 11)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate chest = mgr.items.get(Content.ITEM_BLACKENED_DEFIAS_ARMOR);
            assertNotNull(chest);
            assertEquals(4, chest.statType[0]);
            assertEquals(4, chest.statValue[0]);
            assertEquals(3, chest.statType[1]);
            assertEquals(3, chest.statValue[1]);
            assertEquals(7, chest.statType[2]);
            assertEquals(11, chest.statValue[2]);
            assertEquals(92, chest.armor);
        }
    }
}
