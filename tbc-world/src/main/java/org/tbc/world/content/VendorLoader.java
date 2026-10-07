package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL load of the vendor family: npc_vendor / npc_vendor_template (slot-ordered first, then plain) and
 * creature_template.VendorTemplateId. Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class VendorLoader {
    private static final Logger log = LoggerFactory.getLogger(VendorLoader.class);
    private final ObjectMgr m;

    private VendorLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order and failure handling as ObjectMgr.load. */
    static void load(ObjectMgr m, Connection c) {
        VendorLoader l = new VendorLoader(m);
        l.loadNpcVendors(c);
        try {
            l.loadVendorMeta(c);
        } catch (Exception e) {
            log.debug("vendor meta load skipped: {}", e.getMessage());
        }
    }
    private void loadNpcVendors(Connection c) {
        loadNpcVendorTable(c, "npc_vendor", m.vendorItems);
        loadNpcVendorTable(c, "npc_vendor_template", m.vendorTemplateItems);
    }

    private void loadNpcVendorTable(Connection c, String table, Map<Integer, List<Integer>> into) {
        if (loadNpcVendorQuery(c, "SELECT entry, item FROM " + table + " ORDER BY slot, item", into)) {
            return;
        }
        loadNpcVendorQuery(c, "SELECT entry, item FROM " + table, into);
    }

    private boolean loadNpcVendorQuery(Connection c, String sql, Map<Integer, List<Integer>> into) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int item = rs.getInt(2);
                if (item <= 0) {
                    continue;
                }
                List<Integer> stock = into.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>());
                if (!stock.contains(item)) {
                    stock.add(item);
                }
                n++;
            }
            log.info("loaded {} rows via {}", n, sql.contains("template") ? "npc_vendor_template" : "npc_vendor");
            return true;
        } catch (Exception e) {
            log.debug("vendor load skipped ({}): {}", sql, e.getMessage());
            return false;
        }
    }

    /** creature_template.VendorTemplateId (CMaNGOS Creature::GetVendorTemplateItems). */
    private void loadVendorMeta(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement("SELECT Entry, VendorTemplateId FROM creature_template");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int entry = rs.getInt(1);
            int tmpl = rs.getInt(2);
            if (tmpl != 0) {
                m.vendorTemplateId.put(entry, tmpl);
                n++;
            }
        }
        log.info("loaded VendorTemplateId for {} creatures", n);
    }
}
