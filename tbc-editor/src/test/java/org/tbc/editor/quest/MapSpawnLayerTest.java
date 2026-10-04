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
                15271, "Mana Wyrm", 1, 14, 40, 1, 0, "", "", 0,
                "", "", 0, 0, 0, 0, 1, 0, 0, 0, 1f, 1f, 0));
        mgr.gameObjects.put(1, new ObjectMgr.GameObjectTemplate(
                1, 3, 1, "Sunstrider Crate", "", "", "", new int[24], 1f));
        mgr.spawns.add(new ObjectMgr.Spawn(1, 15271, 530, 10349.6f, -6357.29f, 33f, 0f));
        mgr.spawns.add(new ObjectMgr.Spawn(2, 15271, 530, 9000f, -8000f, 20f, 0f));
        mgr.goSpawns.add(new ObjectMgr.Spawn(3, 1, 530, 10360f, -6360f, 33f, 0f));
        List<MapSpawnLayer.Pin> pins = MapSpawnLayer.inArea(mgr, WorldMapAreas.SUNSTRIDER);
        assertTrue(pins.stream().anyMatch(p -> p.kind() == MapSpawnLayer.Kind.CREATURE && p.entry() == 15271
                && "Mana Wyrm".equals(p.name()) && "Beast".equals(p.typeName())));
        assertTrue(pins.stream().anyMatch(p -> p.kind() == MapSpawnLayer.Kind.OBJECT
                && "Sunstrider Crate".equals(p.name()) && "Chest".equals(p.typeName())));
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
                MapSpawnLayer.Kind.CREATURE, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f)));
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 0L, 0,
                (int) pix[0], (int) pix[1], 0, false, MouseEvent.NOBUTTON));
        String hover = canvas.hoverText();
        assertTrue(hover.contains("Mana Wyrm"));
        assertTrue(hover.contains("Beast"));
        assertTrue(hover.contains("15271"));
        assertNull(MapSpawnLayer.nearest(canvas.spawns(), 0f, 0f, 10f));
    }

    @Test
    void doubleClickWhenOnSpawnShouldSelectItAndSkipPlacingAMarker() {
        QuestMapCanvas canvas = new QuestMapCanvas();
        canvas.loadRegion(WorldMapAreas.SUNSTRIDER,
                org.tbc.world.map.RegionMinimap.render(WorldMapAreas.SUNSTRIDER,
                        org.tbc.world.map.Terrain.NONE, 200, 100));
        canvas.setSize(200, 100);
        canvas.model().setTool(QuestMapModel.Tool.PLACE_NOTE);
        canvas.setSpawns(List.of(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f)));
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        int x = (int) pix[0];
        int y = (int) pix[1];
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0L, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
        assertEquals(0, canvas.model().markers().size());
        assertEquals(null, canvas.selectedSpawn());
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0L, 0,
                x, y, 2, false, MouseEvent.BUTTON1));
        assertEquals(15271, canvas.selectedSpawn().entry());
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0L, 0,
                2, 2, 1, false, MouseEvent.BUTTON1));
        assertEquals(1, canvas.model().markers().size());
    }
}
