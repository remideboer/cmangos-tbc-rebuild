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

    /**
     * TP-SL14-013 — LoadItemPrototypes extra proto stats. SQL must carry stat_type4
     * (Lightforge Breastplate 16726 STR / STA / INT / SPI).
     */
    @Test
    void loadItemsWhenTemplateHasFourthStatSlotShouldCarryStrengthStaminaIntellectSpirit() throws Exception {
        String url = "jdbc:h2:mem:items4_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-stat4-test")) {
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
                          stat_value3 INT,
                          stat_type4 INT,
                          stat_value4 INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4)
                        VALUES (16726, 4, 4, 'Lightforge Breastplate', 0, 3, 0, 0, 35078, 5, -1, -1, 63, 58, 0,
                          1, 0, 657, 0, 1, '', 135, 0, -1, 0, 0, 4, 13, 7, 21, 5, 16, 6, 8)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate chest = mgr.items.get(Content.ITEM_LIGHTFORGE_BREASTPLATE);
            assertNotNull(chest);
            assertEquals(4, chest.statType[0]);
            assertEquals(13, chest.statValue[0]);
            assertEquals(7, chest.statType[1]);
            assertEquals(21, chest.statValue[1]);
            assertEquals(5, chest.statType[2]);
            assertEquals(16, chest.statValue[2]);
            assertEquals(6, chest.statType[3]);
            assertEquals(8, chest.statValue[3]);
            assertEquals(657, chest.armor);
        }
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes fifth proto stat. SQL must carry stat_type5
     * (Blade of Hanna 2801 SPIRIT 11 in slot 5).
     */
    @Test
    void loadItemsWhenTemplateHasFifthStatShouldCarrySpirit() throws Exception {
        String url = "jdbc:h2:mem:items5_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-fifth-stat")) {
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
                          stat_value3 INT,
                          stat_type4 INT,
                          stat_value4 INT,
                          stat_type5 INT,
                          stat_value5 INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4,
                          stat_type5, stat_value5)
                        VALUES (2801, 2, 8, 'Blade of Hanna', 0, 4, 0, 0, 90978, 17, -1, -1, 64, 59, 1,
                          1, 0, 0, 2100, 2, '', 120, 0, -1, 101, 152, 4, 11, 3, 11, 7, 11, 5, 11, 6, 11)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate sword = mgr.items.get(Content.ITEM_BLADE_OF_HANNA);
            assertNotNull(sword);
            assertEquals(6, sword.statType[4]);
            assertEquals(11, sword.statValue[4]);
        }
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes FireRes. SQL item_template must carry fire_res
     * (Lawbringer Chestguard 16853 FireRes 10) so equipped school resist is not 0.
     */
    @Test
    void loadItemsWhenTemplateHasFireResShouldCarryFireResistance() throws Exception {
        String url = "jdbc:h2:mem:items_fireres_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-fire-res")) {
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
                          stat_value3 INT,
                          stat_type4 INT,
                          stat_value4 INT,
                          stat_type5 INT,
                          stat_value5 INT,
                          fire_res INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4,
                          stat_type5, stat_value5, fire_res)
                        VALUES (16853, 4, 4, 'Lawbringer Chestguard', 0, 4, 0, 0, 52573, 5, -1, -1, 66, 60, 0,
                          1, 0, 855, 0, 1, '', 165, 0, -1, 0, 0, 4, 8, 7, 26, 5, 21, 6, 13, 0, 0, 10)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate chest = mgr.items.get(Content.ITEM_LAWBRINGER_CHESTGUARD);
            assertNotNull(chest);
            assertEquals(10, chest.fireRes);
        }
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes NatureRes. SQL item_template must carry nature_res
     * (Living Breastplate 15059 NatureRes 5) so equipped school resist is not 0.
     */
    @Test
    void loadItemsWhenTemplateHasNatureResShouldCarryNatureResistance() throws Exception {
        String url = "jdbc:h2:mem:items_natureres_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-nature-res")) {
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
                          stat_value3 INT,
                          stat_type4 INT,
                          stat_value4 INT,
                          stat_type5 INT,
                          stat_value5 INT,
                          fire_res INT,
                          nature_res INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4,
                          stat_type5, stat_value5, fire_res, nature_res)
                        VALUES (15059, 4, 2, 'Living Breastplate', 0, 3, 0, 0, 24776, 5, -1, -1, 60, 55, 0,
                          1, 0, 169, 0, 2, '', 100, 0, -1, 0, 0, 7, 10, 6, 25, 0, 0, 0, 0, 0, 0, 0, 5)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate chest = mgr.items.get(Content.ITEM_LIVING_BREASTPLATE);
            assertNotNull(chest);
            assertEquals(5, chest.natureRes);
        }
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes FrostRes. SQL item_template must carry frost_res
     * (Icebane Breastplate 22669 FrostRes 42) so equipped school resist is not 0.
     */
    @Test
    void loadItemsWhenTemplateHasFrostResShouldCarryFrostResistance() throws Exception {
        String url = "jdbc:h2:mem:items_frostres_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-template-frost-res")) {
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
                          stat_value3 INT,
                          stat_type4 INT,
                          stat_value4 INT,
                          stat_type5 INT,
                          stat_value5 INT,
                          fire_res INT,
                          nature_res INT,
                          frost_res INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1,
                          stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4,
                          stat_type5, stat_value5, fire_res, nature_res, frost_res)
                        VALUES (22669, 4, 4, 'Icebane Breastplate', 0, 4, 0, 0, 57715, 5, -1, -1, 80, 60, 0,
                          1, 0, 1027, 0, 2, '', 165, 0, -1, 0, 0, 4, 12, 7, 24, 0, 0, 0, 0, 0, 0, 0, 0, 42)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate chest = mgr.items.get(Content.ITEM_ICEBANE_BREASTPLATE);
            assertNotNull(chest);
            assertEquals(42, chest.frostRes);
        }
    }
}
