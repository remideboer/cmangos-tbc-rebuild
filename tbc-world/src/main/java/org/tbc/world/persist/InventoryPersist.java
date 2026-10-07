package org.tbc.world.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;

/** character_inventory and item_instance rows (CMaNGOS Player::_SaveInventory / _LoadInventory), with the legacy data-blob fallback. Split out of CharacterStore; CharacterStore.save/load stay the public surface. */
final class InventoryPersist {

    private InventoryPersist() {
    }
    static void load(Connection c, Player p) throws Exception {
        try {
            loadInventoryJoin(c, p, true);
        } catch (Exception e) {
            loadInventoryJoin(c, p, false);
        }
    }

    static void loadInventoryJoin(Connection c, Player p, boolean itemEntry) throws Exception {
        String sql = itemEntry
                ? "SELECT ci.bag, ci.slot, ci.item, ci.item_template, ii.count, ii.durability FROM character_inventory ci JOIN item_instance ii ON ci.item = ii.guid WHERE ci.guid = ? ORDER BY ci.bag, ci.slot"
                : "SELECT ci.bag, ci.slot, ci.item, ci.item_template, ii.data FROM character_inventory ci JOIN item_instance ii ON ci.item = ii.guid WHERE ci.guid = ? ORDER BY ci.bag, ci.slot";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int itemGuid = rs.getInt("item");
            int entry = rs.getInt("item_template");
            Item it = new Item(itemGuid, entry);
            it.bag = rs.getInt("bag");
            it.slot = rs.getInt("slot");
            it.ownerGuid = Guid.low(p.guid);
            if (itemEntry) {
                it.count = Math.max(1, rs.getInt("count"));
                it.durability = rs.getInt("durability");
            } else {
                String data = rs.getString("data");
                parseItemData(it, data);
            }
            p.items.put(itemGuid, it);
        }
    }

    static void parseItemData(Item it, String data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        int colon = data.indexOf(':');
        if (colon <= 0) {
            return;
        }
        try {
            it.entry = Integer.parseInt(data.substring(0, colon));
            it.count = Math.max(1, Integer.parseInt(data.substring(colon + 1)));
        } catch (NumberFormatException ignored) {
        }
    }

    static void write(Connection c, Player p) throws Exception {
        PreparedStatement delInv = c.prepareStatement("DELETE FROM character_inventory WHERE guid = ?");
        delInv.setInt(1, Guid.low(p.guid));
        delInv.executeUpdate();
        PreparedStatement delInst = c.prepareStatement("DELETE FROM item_instance WHERE owner_guid = ?");
        delInst.setInt(1, Guid.low(p.guid));
        delInst.executeUpdate();
        for (Item it : p.items.values()) {
            if (!writeItemInstance(c, p, it)) {
                continue;
            }
            PreparedStatement inv = c.prepareStatement(
                    "INSERT INTO character_inventory (guid, bag, slot, item, item_template) VALUES (?,?,?,?,?)");
            inv.setInt(1, Guid.low(p.guid));
            inv.setInt(2, it.bag);
            inv.setInt(3, it.slot);
            inv.setInt(4, Guid.low(it.guid));
            inv.setInt(5, it.entry);
            inv.executeUpdate();
        }
    }

    static boolean writeItemInstance(Connection c, Player p, Item it) {
        try (PreparedStatement ii = c.prepareStatement(
                "INSERT INTO item_instance (guid,owner_guid,itemEntry,creatorGuid,giftCreatorGuid,count,duration,charges,flags,enchantments,randomPropertyId,durability,itemTextId) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            int i = 1;
            ii.setInt(i++, Guid.low(it.guid));
            ii.setInt(i++, Guid.low(p.guid));
            ii.setInt(i++, it.entry);
            ii.setInt(i++, 0);
            ii.setInt(i++, 0);
            ii.setInt(i++, Math.max(1, it.count));
            ii.setInt(i++, 0);
            ii.setString(i++, "0 0 0 0 0");
            ii.setInt(i++, 0);
            ii.setString(i++, "0");
            ii.setInt(i++, 0);
            ii.setInt(i++, it.durability);
            ii.setInt(i++, 0);
            ii.executeUpdate();
            return true;
        } catch (Exception e) {
            try (PreparedStatement ii = c.prepareStatement(
                    "INSERT INTO item_instance (guid,owner_guid,data) VALUES (?,?,?)")) {
                ii.setInt(1, Guid.low(it.guid));
                ii.setInt(2, Guid.low(p.guid));
                ii.setString(3, it.entry + ":" + it.count);
                ii.executeUpdate();
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
