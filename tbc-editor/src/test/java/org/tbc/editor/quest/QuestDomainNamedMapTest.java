package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.AreaTable;
import org.tbc.world.map.WorldMapAreas;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDomainNamedMapTest {
    @Test
    void selectNamedMapWhenEversongWoodsShouldSetMapAndZone() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestService svc = new QuestService(mgr, Path.of("content-unused"), MapSurfaceServiceSupport.uniqueAdt(1f));
        QuestDomain domain = new QuestDomain(svc, s -> {}, WorldMapAreas.seeded(), (area) ->
                org.tbc.world.map.RegionMinimap.render(area, org.tbc.world.map.Terrain.NONE, 32, 32));
        domain.selectNamedMap("Eversong Woods");
        assertEquals(530, domain.document().mapId());
        assertEquals(AreaTable.EVERSONG_WOODS, domain.document().zoneOrSort());
        assertNotNull(domain.canvas().model().region());
        assertEquals(AreaTable.EVERSONG_WOODS, domain.canvas().model().region().areaId());
        assertEquals("Map", domain.tabTitle(0));
        assertEquals("Graph", domain.tabTitle(1));
    }

    @Test
    void selectNamedMapWhenElwynnShouldSetEasternKingdomsMap() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestService svc = new QuestService(mgr, Path.of("content-unused"), MapSurfaceServiceSupport.uniqueAdt(1f));
        QuestDomain domain = new QuestDomain(svc, s -> {}, WorldMapAreas.seeded(), (area) ->
                org.tbc.world.map.RegionMinimap.render(area, org.tbc.world.map.Terrain.NONE, 32, 32));
        domain.selectNamedMap("Elwynn Forest");
        assertEquals(0, domain.document().mapId());
        assertEquals(AreaTable.ELWYNN_FOREST, domain.document().zoneOrSort());
    }

    @Test
    void zoneComboWhenBuiltShouldListSeededZonesAndStayWide() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestService svc = new QuestService(mgr, Path.of("content-unused"), MapSurfaceServiceSupport.uniqueAdt(1f));
        QuestDomain domain = new QuestDomain(svc, s -> {}, WorldMapAreas.seeded(), (area) ->
                org.tbc.world.map.RegionMinimap.render(area, org.tbc.world.map.Terrain.NONE, 32, 32));
        javax.swing.JComboBox<org.tbc.world.map.WorldMapArea> combo = domain.zoneCombo();
        assertEquals("Zone", domain.zoneLabel().getText());
        assertTrue(combo.getPreferredSize().width >= 280);
        boolean elwynn = false;
        boolean eversong = false;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String name = combo.getItemAt(i).displayName();
            if ("Elwynn Forest".equals(name)) {
                elwynn = true;
            }
            if ("Eversong Woods".equals(name)) {
                eversong = true;
            }
        }
        assertTrue(elwynn);
        assertTrue(eversong);
        assertNotNull(findLabel(domain.view(), "Zone"));
    }

    private static javax.swing.JLabel findLabel(java.awt.Component root, String text) {
        if (root instanceof javax.swing.JLabel l && text.equals(l.getText())) {
            return l;
        }
        if (root instanceof java.awt.Container c) {
            for (java.awt.Component child : c.getComponents()) {
                javax.swing.JLabel hit = findLabel(child, text);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }
}
