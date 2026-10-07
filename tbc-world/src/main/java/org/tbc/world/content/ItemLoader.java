package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.ItemTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SQL load of item_template: base rows with progressively shorter SELECT fallbacks, then the spell and
 * sheath columns merged onto the loaded templates. Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class ItemLoader {
    private static final Logger log = LoggerFactory.getLogger(ItemLoader.class);
    private final ObjectMgr m;

    private ItemLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order as ObjectMgr.load: items, then spell columns, then sheath. */
    static void load(ObjectMgr m, Connection c) {
        ItemLoader l = new ItemLoader(m);
        l.loadItems(c);
        l.loadItemSpells(c);
        l.loadItemSheath(c);
    }
    /**
     * item_template spellid_1..5 / spelltrigger_1..5 / spellcharges_1..5 — not in the base SELECT.
     * Merges onto templates already loaded by {@link #loadItems(Connection)}.
     */
    private void loadItemSpells(Connection c) {
        String sql = "SELECT entry, spellid_1, spelltrigger_1, spellcharges_1, "
                + "spellid_2, spelltrigger_2, spellcharges_2, "
                + "spellid_3, spelltrigger_3, spellcharges_3, "
                + "spellid_4, spelltrigger_4, spellcharges_4, "
                + "spellid_5, spelltrigger_5, spellcharges_5 FROM item_template";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate t = m.items.get(rs.getInt(1));
                if (t == null) {
                    continue;
                }
                for (int i = 0; i < 5; i++) {
                    int base = 2 + i * 3;
                    t.spellId[i] = rs.getInt(base);
                    t.spellTrigger[i] = rs.getInt(base + 1);
                    t.spellCharges[i] = rs.getInt(base + 2);
                }
            }
        } catch (Exception e) {
            log.warn("item_template spell columns load failed: {}", e.getMessage());
        }
    }

    /**
     * item_template.sheath — not in the base SELECT. Client uses this for sheathed
     * attachment points (back/hip/shield); 0 = SHEATHETYPE_NONE (models despawn).
     */
    private void loadItemSheath(Connection c) {
        String sql = "SELECT entry, sheath FROM item_template";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate t = m.items.get(rs.getInt(1));
                if (t == null) {
                    continue;
                }
                t.sheath = rs.getInt(2);
            }
        } catch (Exception e) {
            log.warn("item_template sheath load failed: {}", e.getMessage());
        }
    }

    private void loadItems(Connection c) {
        String[] sqls = {
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block`, dmg_min3, dmg_max3, dmg_type3, dmg_min4, dmg_max4, dmg_type4, "
                        + "dmg_min5, dmg_max5, dmg_type5, stat_type8, stat_value8, stat_type9, stat_value9, "
                        + "stat_type10, stat_value10 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block`, dmg_min3, dmg_max3, dmg_type3, dmg_min4, dmg_max4, dmg_type4, "
                        + "dmg_min5, dmg_max5, dmg_type5 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block`, dmg_min3, dmg_max3, dmg_type3 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block` FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7 "
                        + "FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill FROM item_template LIMIT 50000"
        };
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    ItemTemplate t = new ItemTemplate();
                    t.entry = rs.getInt(1);
                    t.itemClass = rs.getInt(2);
                    t.subClass = rs.getInt(3);
                    t.name = ObjectMgr.nz(rs.getString(4));
                    t.displayId = rs.getInt(5);
                    t.quality = rs.getInt(6);
                    t.flags = rs.getInt(7);
                    t.buyPrice = rs.getInt(8);
                    t.sellPrice = rs.getInt(9);
                    t.inventoryType = rs.getInt(10);
                    t.allowableClass = rs.getInt(11);
                    t.allowableRace = rs.getInt(12);
                    t.itemLevel = rs.getInt(13);
                    t.requiredLevel = rs.getInt(14);
                    t.maxCount = rs.getInt(15);
                    t.stackable = Math.max(1, rs.getInt(16));
                    t.containerSlots = rs.getInt(17);
                    t.armor = rs.getInt(18);
                    t.delay = rs.getInt(19);
                    t.bonding = rs.getInt(20);
                    t.description = ObjectMgr.nz(rs.getString(21));
                    t.maxDurability = rs.getInt(22);
                    t.duration = rs.getInt(23);
                    t.requiredDisenchantSkill = rs.getInt(24);
                    t.unk = -1;
                    if (cols >= 28) {
                        t.dmgMin[0] = rs.getFloat(25);
                        t.dmgMax[0] = rs.getFloat(26);
                        t.statType[0] = rs.getInt(27);
                        t.statValue[0] = rs.getInt(28);
                    }
                    if (cols >= 30) {
                        t.statType[1] = rs.getInt(29);
                        t.statValue[1] = rs.getInt(30);
                    }
                    if (cols >= 32) {
                        t.statType[2] = rs.getInt(31);
                        t.statValue[2] = rs.getInt(32);
                    }
                    if (cols >= 34) {
                        t.statType[3] = rs.getInt(33);
                        t.statValue[3] = rs.getInt(34);
                    }
                    if (cols >= 36) {
                        t.statType[4] = rs.getInt(35);
                        t.statValue[4] = rs.getInt(36);
                    }
                    if (cols >= 37) {
                        t.fireRes = rs.getInt(37);
                    }
                    if (cols >= 38) {
                        t.natureRes = rs.getInt(38);
                    }
                    if (cols >= 39) {
                        t.frostRes = rs.getInt(39);
                    }
                    if (cols >= 40) {
                        t.shadowRes = rs.getInt(40);
                    }
                    if (cols >= 41) {
                        t.arcaneRes = rs.getInt(41);
                    }
                    if (cols >= 44) {
                        t.dmgMin[1] = rs.getFloat(42);
                        t.dmgMax[1] = rs.getFloat(43);
                        t.dmgType[1] = rs.getInt(44);
                    }
                    if (cols >= 46) {
                        t.statType[5] = rs.getInt(45);
                        t.statValue[5] = rs.getInt(46);
                    }
                    if (cols >= 48) {
                        t.statType[6] = rs.getInt(47);
                        t.statValue[6] = rs.getInt(48);
                    }
                    if (cols >= 49) {
                        t.block = rs.getInt(49);
                    }
                    if (cols >= 52) {
                        t.dmgMin[2] = rs.getFloat(50);
                        t.dmgMax[2] = rs.getFloat(51);
                        t.dmgType[2] = rs.getInt(52);
                    }
                    if (cols >= 55) {
                        t.dmgMin[3] = rs.getFloat(53);
                        t.dmgMax[3] = rs.getFloat(54);
                        t.dmgType[3] = rs.getInt(55);
                    }
                    if (cols >= 58) {
                        t.dmgMin[4] = rs.getFloat(56);
                        t.dmgMax[4] = rs.getFloat(57);
                        t.dmgType[4] = rs.getInt(58);
                    }
                    if (cols >= 60) {
                        t.statType[7] = rs.getInt(59);
                        t.statValue[7] = rs.getInt(60);
                    }
                    if (cols >= 62) {
                        t.statType[8] = rs.getInt(61);
                        t.statValue[8] = rs.getInt(62);
                    }
                    if (cols >= 64) {
                        t.statType[9] = rs.getInt(63);
                        t.statValue[9] = rs.getInt(64);
                    }
                    m.items.put(t.entry, t);
                }
                return;
            } catch (Exception e) {
                log.debug("item_template load skipped: {}", e.getMessage());
            }
        }
    }
}
