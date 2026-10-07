package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.GameObjectTemplate;
import org.tbc.world.content.ObjectMgr.PageText;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** SQL load of gameobject_template and page_text. Carved from ObjectMgr in refactoring plan cycle 4.2. */
final class GameObjectLoader {
    private static final Logger log = LoggerFactory.getLogger(GameObjectLoader.class);
    private final ObjectMgr m;

    private GameObjectLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order as ObjectMgr.load. */
    static void load(ObjectMgr m, Connection c) {
        GameObjectLoader l = new GameObjectLoader(m);
        l.loadGameObjects(c);
        l.loadPageTexts(c);
    }
    private void loadGameObjects(Connection c) {
        String sql = "SELECT entry, type, displayId, name, IconName, OpeningText, ClosingText, size, "
                + "data0, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, "
                + "data12, data13, data14, data15, data16, data17, data18, data19, data20, data21, data22, data23 "
                + "FROM gameobject_template LIMIT 20000";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int[] data = new int[24];
                for (int i = 0; i < 24; i++) {
                    data[i] = rs.getInt(9 + i);
                }
                int entry = rs.getInt(1);
                m.gameObjects.put(entry, new GameObjectTemplate(entry, rs.getInt(2), rs.getInt(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), data, rs.getFloat(8)));
            }
        } catch (Exception e) {
            log.debug("gameobject_template load skipped: {}", e.getMessage());
        }
    }

    private void loadPageTexts(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("SELECT entry, text, next_page FROM page_text LIMIT 20000");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.pageTexts.put(rs.getInt(1), new PageText(rs.getInt(1), ObjectMgr.nz(rs.getString(2)), rs.getInt(3)));
            }
        } catch (Exception e) {
            log.debug("page_text load skipped: {}", e.getMessage());
        }
    }
}
