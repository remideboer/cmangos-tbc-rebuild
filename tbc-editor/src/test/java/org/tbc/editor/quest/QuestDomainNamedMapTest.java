package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.AreaTable;
import org.tbc.world.map.WorldMapAreas;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
