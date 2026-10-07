package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.CreateInfo;
import org.tbc.world.content.ObjectMgr.CreateItem;
import org.tbc.world.content.ObjectMgr.CreateSkill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * SQL load of playercreateinfo and its _spell / _action / _skills / _item side tables. A missing base table
 * throws so ObjectMgr falls back to the in-memory seeds. Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class PlayerCreateLoader {
    private static final Logger log = LoggerFactory.getLogger(PlayerCreateLoader.class);
    private final ObjectMgr m;

    private PlayerCreateLoader(ObjectMgr m) {
        this.m = m;
    }

    static void load(ObjectMgr m, Connection c) throws Exception {
        new PlayerCreateLoader(m).all(c);
    }
    private void all(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT race, class, map, zone, position_x, position_y, position_z, orientation FROM playercreateinfo");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int race = rs.getInt(1);
            int clazz = rs.getInt(2);
            m.createInfo.put(ObjectMgr.key(race, clazz), new CreateInfo(race, clazz, rs.getInt(3), rs.getInt(4),
                    rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), rs.getFloat(8)));
        }
        try {
            PreparedStatement sp = c.prepareStatement("SELECT race, class, Spell FROM playercreateinfo_spell");
            ResultSet sr = sp.executeQuery();
            while (sr.next()) {
                int k = (int) ObjectMgr.key(sr.getInt(1), sr.getInt(2));
                m.createSpells.computeIfAbsent(k, x -> new ArrayList<>()).add(sr.getInt(3));
            }
        } catch (Exception ignored) {
            // table name Spell vs spell
        }
        try {
            PreparedStatement ac = c.prepareStatement(
                    "SELECT race, `class`, button, action, `type` FROM playercreateinfo_action");
            ResultSet ar = ac.executeQuery();
            while (ar.next()) {
                m.putCreateAction(ar.getInt(1), ar.getInt(2), ar.getInt(3), ar.getInt(4), ar.getInt(5));
            }
            log.info("loaded {} playercreateinfo_action race/class keys", m.createActions.size());
        } catch (Exception e) {
            log.warn("playercreateinfo_action load failed: {}", e.getMessage());
        }
        try {
            PreparedStatement sk = c.prepareStatement(
                    "SELECT raceMask, classMask, skill, step FROM playercreateinfo_skills");
            ResultSet kr = sk.executeQuery();
            while (kr.next()) {
                m.createSkills.add(new CreateSkill(kr.getInt(1), kr.getInt(2), kr.getInt(3), kr.getInt(4)));
            }
            log.info("loaded {} playercreateinfo_skills", m.createSkills.size());
        } catch (Exception e) {
            log.warn("playercreateinfo_skills load failed: {}", e.getMessage());
        }
        try {
            PreparedStatement it = c.prepareStatement(
                    "SELECT race, class, itemid, amount FROM playercreateinfo_item");
            ResultSet ir = it.executeQuery();
            while (ir.next()) {
                int itemId = ir.getInt(3);
                int amount = ir.getInt(4);
                if (itemId <= 0 || amount <= 0) {
                    continue;
                }
                int k = (int) ObjectMgr.key(ir.getInt(1), ir.getInt(2));
                m.createItems.computeIfAbsent(k, x -> new ArrayList<>()).add(new CreateItem(itemId, amount));
            }
        } catch (Exception ignored) {
        }
    }
}
