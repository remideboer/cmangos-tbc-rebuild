package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.WorldMapAreas;

import java.awt.event.MouseEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSpawnLayerTest {
    @Test
    void inAreaWhenSpawnInsideSunstriderShouldIncludeItAndSkipOutside() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15271, new ObjectMgr.CreatureTemplate(
                15271, "Mana Wyrm", 1, 14, 40, 1, 0, "", "", 0));
        mgr.gameObjects.put(1, new ObjectMgr.GameObjectTemplate(
                1, 3, 1, "Sunstrider Crate", "", "", "", new int[24], 1f));
        mgr.spawns.add(new ObjectMgr.Spawn(1, 15271, 530, 10349.6f, -6357.29f, 33f, 0f));
        mgr.spawns.add(new ObjectMgr.Spawn(2, 15271, 530, 9000f, -8000f, 20f, 0f));
        mgr.goSpawns.add(new ObjectMgr.Spawn(3, 1, 530, 10360f, -6360f, 33f, 0f));
        List<MapSpawnLayer.Pin> pins = MapSpawnLayer.inArea(mgr, WorldMapAreas.SUNSTRIDER);
        assertTrue(pins.stream().anyMatch(p -> p.kind() == MapSpawnLayer.Kind.CREATURE && p.entry() == 15271
                && "Mana Wyrm".equals(p.name())));
        assertTrue(pins.stream().anyMatch(p -> p.kind() == MapSpawnLayer.Kind.OBJECT && "Sunstrider Crate".equals(p.name())));
        assertTrue(pins.stream().noneMatch(p -> p.x() == 9000f));
    }

    @Test
    void hoverWhenNearSpawnShouldNameIt() {
        QuestMapCanvas canvas = new QuestMapCanvas();
        canvas.loadRegion(WorldMapAreas.SUNSTRIDER,
                org.tbc.world.map.RegionMinimap.render(WorldMapAreas.SUNSTRIDER,
                        org.tbc.world.map.Terrain.NONE, 200, 100));
        canvas.setSize(200, 100);
        canvas.setSpawns(List.of(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 15271, "Mana Wyrm", 10349.6f, -6357.29f)));
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 0L, 0,
                (int) pix[0], (int) pix[1], 0, false, MouseEvent.NOBUTTON));
        assertEquals("Mana Wyrm (15271)", canvas.hoverText());
        assertNull(MapSpawnLayer.nearest(canvas.spawns(), 0f, 0f, 10f));
    }
}
