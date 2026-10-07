package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.GossipMenuItem;
import org.tbc.world.content.ObjectMgr.NpcText;
import org.tbc.world.content.ObjectMgr.NpcTextSlot;
import org.tbc.world.content.ObjectMgr.PointOfInterest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * SQL load of the gossip family (points_of_interest, gossip_menu, gossip_menu_option, creature GossipMenuId,
 * npc_text) with the same column fallbacks as CMaNGOS ObjectMgr::LoadGossip*. Carved from ObjectMgr in
 * refactoring plan cycle 4.2.
 */
final class GossipLoader {
    private static final Logger log = LoggerFactory.getLogger(GossipLoader.class);
    private final ObjectMgr m;

    private GossipLoader(ObjectMgr m) {
        this.m = m;
    }

    static void load(ObjectMgr m, Connection c) {
        new GossipLoader(m).all(c);
    }
    private void all(Connection c) {
        loadPointsOfInterest(c);
        loadGossipMenus(c);
        loadGossipOptions(c);
        loadGossipMenuIds(c);
        loadNpcTexts(c);
    }

    private void loadPointsOfInterest(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, x, y, icon, flags, data, icon_name FROM points_of_interest");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                float x = rs.getFloat(2);
                float y = rs.getFloat(3);
                if (!org.tbc.world.map.MapCoords.valid(x, y)) {
                    log.debug("points_of_interest entry {} invalid coordinates, ignored", rs.getInt(1));
                    continue;
                }
                int entry = rs.getInt(1);
                m.pointsOfInterest.put(entry, new PointOfInterest(entry, x, y, rs.getInt(4), rs.getInt(5),
                        rs.getInt(6), SqlText.nz(rs.getString(7))));
                n++;
            }
            log.info("loaded {} points_of_interest", n);
        } catch (Exception e) {
            log.debug("points_of_interest load skipped: {}", e.getMessage());
        }
    }

    private void loadGossipMenus(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, text_id, condition_id FROM gossip_menu");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int menuId = rs.getInt(1);
                int conditionId = rs.getInt(3);
                if (conditionId != 0) {
                    continue;
                }
                m.gossipTextIds.putIfAbsent(menuId, rs.getInt(2));
                n++;
            }
            log.info("loaded {} gossip_menu rows", n);
        } catch (Exception e) {
            log.debug("gossip_menu load skipped: {}", e.getMessage());
        }
    }

    private void loadGossipOptions(Connection c) {
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "action_menu_id, action_poi_id, box_coded, box_money, box_text, condition_id "
                        + "FROM gossip_menu_option ORDER BY menu_id, id")) {
            return;
        }
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "action_menu_id, action_poi_id, box_coded, box_money, box_text FROM gossip_menu_option "
                        + "ORDER BY menu_id, id")) {
            return;
        }
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "action_menu_id, box_coded, box_money, box_text FROM gossip_menu_option "
                        + "ORDER BY menu_id, id")) {
            return;
        }
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "box_coded, box_money, box_text FROM gossip_menu_option ORDER BY menu_id, id")) {
            return;
        }
        loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag "
                        + "FROM gossip_menu_option ORDER BY menu_id, id");
    }

    private boolean loadGossipOptionQuery(Connection c, String sql) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean action = sql.contains("action_menu_id");
            boolean poiCol = sql.contains("action_poi_id");
            boolean boxed = sql.contains("box_coded");
            boolean condCol = sql.contains("condition_id");
            int n = 0;
            while (rs.next()) {
                int menuId = rs.getInt(1);
                int actionMenu = 0;
                int actionPoi = 0;
                int coded = 0;
                int boxMoney = 0;
                String boxText = "";
                int conditionId = 0;
                if (action) {
                    actionMenu = rs.getInt(7);
                    int col = 8;
                    if (poiCol) {
                        actionPoi = rs.getInt(col++);
                    }
                    if (boxed) {
                        coded = rs.getInt(col++);
                        boxMoney = rs.getInt(col++);
                        boxText = SqlText.nz(rs.getString(col++));
                    }
                    if (condCol) {
                        conditionId = rs.getInt(col);
                    }
                } else if (boxed) {
                    coded = rs.getInt(7);
                    boxMoney = rs.getInt(8);
                    boxText = SqlText.nz(rs.getString(9));
                }
                if (actionPoi != 0 && !m.pointsOfInterest.containsKey(actionPoi)) {
                    actionPoi = 0;
                }
                m.gossipOptions.computeIfAbsent(menuId, k -> new ArrayList<>()).add(new GossipMenuItem(
                        menuId, rs.getInt(2), rs.getInt(3), SqlText.nz(rs.getString(4)), rs.getInt(5), rs.getInt(6),
                        coded, boxMoney, boxText, actionMenu, actionPoi, conditionId));
                n++;
            }
            log.info("loaded {} gossip_menu_option rows", n);
            return true;
        } catch (Exception e) {
            log.debug("gossip_menu_option load skipped: {}", e.getMessage());
            return false;
        }
    }

    private void loadGossipMenuIds(Connection c) {
        if (loadGossipMenuIdQuery(c, "SELECT Entry, GossipMenuId FROM creature_template")) {
            return;
        }
        loadGossipMenuIdQuery(c, "SELECT entry, GossipMenuId FROM creature_template");
    }

    private boolean loadGossipMenuIdQuery(Connection c, String sql) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int menuId = rs.getInt(2);
                if (menuId != 0) {
                    m.gossipMenuIds.put(rs.getInt(1), menuId);
                    n++;
                }
            }
            log.info("loaded {} creature GossipMenuId values", n);
            return true;
        } catch (Exception e) {
            log.debug("creature GossipMenuId load skipped: {}", e.getMessage());
            return false;
        }
    }

    private void loadNpcTexts(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(npcTextSelectSql()); ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int id = rs.getInt(1);
                if (id == 0) {
                    continue;
                }
                NpcTextSlot[] slots = new NpcTextSlot[Content.MAX_GOSSIP_TEXT_OPTIONS];
                int col = 2;
                for (int i = 0; i < slots.length; i++) {
                    String text0 = SqlText.nz(rs.getString(col++));
                    String text1 = SqlText.nz(rs.getString(col++));
                    int language = rs.getInt(col++);
                    float probability = rs.getFloat(col++);
                    int[] emotes = new int[6];
                    for (int e = 0; e < 6; e++) {
                        emotes[e] = rs.getInt(col++);
                    }
                    slots[i] = new NpcTextSlot(probability, text0, text1, language, emotes);
                }
                m.npcTexts.put(id, new NpcText(id, slots));
                n++;
            }
            log.info("loaded {} npc_text rows", n);
        } catch (Exception e) {
            log.debug("npc_text load skipped: {}", e.getMessage());
        }
    }

    private static String npcTextSelectSql() {
        StringBuilder sql = new StringBuilder("SELECT ID");
        for (int i = 0; i < Content.MAX_GOSSIP_TEXT_OPTIONS; i++) {
            sql.append(", text").append(i).append("_0, text").append(i).append("_1, lang").append(i)
                    .append(", prob").append(i);
            for (int e = 0; e < 6; e++) {
                sql.append(", em").append(i).append("_").append(e);
            }
        }
        return sql.append(" FROM npc_text").toString();
    }
}
