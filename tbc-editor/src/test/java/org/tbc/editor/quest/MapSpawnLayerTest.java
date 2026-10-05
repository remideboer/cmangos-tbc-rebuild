package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.FloorCandidates;
import org.tbc.world.map.WorldMapAreas;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
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
        mgr.spawns.add(new ObjectMgr.Spawn(1, 15271, 530, 10349.6f, -6357.29f, 33f, 1.5f));
        mgr.spawns.add(new ObjectMgr.Spawn(2, 15271, 530, 9000f, -8000f, 20f, 0f));
        mgr.goSpawns.add(new ObjectMgr.Spawn(3, 1, 530, 10360f, -6360f, 33f, 0f));
        List<MapSpawnLayer.Pin> pins = MapSpawnLayer.inArea(mgr, WorldMapAreas.SUNSTRIDER);
        assertTrue(pins.stream().anyMatch(p -> p.kind() == MapSpawnLayer.Kind.CREATURE && p.guid() == 1
                && p.entry() == 15271 && p.z() == 33f && p.o() == 1.5f
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
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f)));
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
    void clickWhenOnSpawnShouldSelectItAndEscapeClearsIt() {
        QuestMapCanvas canvas = new QuestMapCanvas();
        canvas.loadRegion(WorldMapAreas.SUNSTRIDER,
                org.tbc.world.map.RegionMinimap.render(WorldMapAreas.SUNSTRIDER,
                        org.tbc.world.map.Terrain.NONE, 200, 100));
        canvas.setSize(200, 100);
        canvas.model().setTool(QuestMapModel.Tool.PLACE_NOTE);
        canvas.setSpawns(List.of(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f)));
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        int x = (int) pix[0];
        int y = (int) pix[1];
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0L, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
        assertEquals(0, canvas.model().markers().size());
        assertEquals(15271, canvas.selectedSpawn().entry());
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0L, 0,
                2, 2, 1, false, MouseEvent.BUTTON1));
        assertEquals(1, canvas.model().markers().size());
        canvas.getActionMap().get("clearSpawn").actionPerformed(new ActionEvent(canvas, 0, "clearSpawn"));
        assertEquals(null, canvas.selectedSpawn());
    }

    @Test
    void rightClickWhenOnCreatureShouldOpenTheEditMenu() {
        QuestMapCanvas canvas = sunstrider(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f));
        JPopupMenu edit = new JPopupMenu();
        edit.add(new JMenuItem("Name"));
        canvas.setNpcEditMenu(edit);
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        JPopupMenu onPin = canvas.menuFor((int) pix[0], (int) pix[1]);
        assertEquals("Name", ((JMenuItem) onPin.getComponent(0)).getText());
        JPopupMenu onMap = canvas.menuFor(2, 2);
        boolean placeGiver = false;
        for (int i = 0; i < onMap.getComponentCount(); i++) {
            if (onMap.getComponent(i) instanceof JMenuItem item && "Place giver".equals(item.getText())) {
                placeGiver = true;
            }
        }
        assertTrue(placeGiver);
    }

    @Test
    void dragWhenSelectedCreatureShouldMoveAndSnapToTheSurface() {
        QuestMapCanvas canvas = sunstrider(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f));
        canvas.setGround((map, x, y) -> FloorCandidates.unique(List.of(), 12.5f));
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        int x = (int) pix[0];
        int y = (int) pix[1];
        int tx = x + 30;
        int ty = y + 15;
        press(canvas, x, y, 2);
        drag(canvas, tx, ty);
        float[] world = canvas.model().viewToWorld(tx, ty);
        MapSpawnLayer.Pin pin = canvas.spawns().get(0);
        assertEquals(world[0], pin.x(), 0.05f);
        assertEquals(world[1], pin.y(), 0.05f);
        assertEquals(12.5f, pin.z(), 0.01f);
        assertEquals(0, canvas.model().markers().size());
    }

    @Test
    void dragWhenFloorMissingShouldKeepTheOldHeight() {
        QuestMapCanvas canvas = sunstrider(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f));
        canvas.setGround((map, x, y) -> FloorCandidates.unresolved());
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        int tx = (int) pix[0] + 30;
        int ty = (int) pix[1] + 15;
        press(canvas, (int) pix[0], (int) pix[1], 2);
        drag(canvas, tx, ty);
        assertEquals(33f, canvas.spawns().get(0).z(), 0.01f);
        assertEquals(0, canvas.model().markers().size());
    }

    @Test
    void dragWhenGameObjectShouldLeaveItInPlace() {
        MapSpawnLayer.Pin creature = new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f);
        MapSpawnLayer.Pin chest = new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.OBJECT, 3, 1, "Sunstrider Crate", "Chest", 10420f, -6200f, 33f);
        QuestMapCanvas canvas = sunstrider(creature, chest);
        float[] creaturePix = canvas.model().worldToPixel(creature.x(), creature.y());
        press(canvas, (int) creaturePix[0], (int) creaturePix[1], 2);
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 0L, 0,
                (int) creaturePix[0], (int) creaturePix[1], 1, false, MouseEvent.BUTTON1));
        float[] chestPix = canvas.model().worldToPixel(chest.x(), chest.y());
        press(canvas, (int) chestPix[0], (int) chestPix[1], 1);
        drag(canvas, (int) chestPix[0] + 40, (int) chestPix[1] + 10);
        MapSpawnLayer.Pin stayed = canvas.spawns().stream()
                .filter(p -> p.kind() == MapSpawnLayer.Kind.OBJECT).findFirst().orElseThrow();
        assertEquals(10420f, stayed.x(), 0.01f);
        assertEquals(-6200f, stayed.y(), 0.01f);
        assertEquals(0, canvas.model().markers().size());
    }

    @Test
    void arrowWhenSelectedCreatureShouldRotateOneSixteenth() {
        QuestMapCanvas canvas = sunstrider(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f, 0f));
        float[] pix = canvas.model().worldToPixel(10349.6f, -6357.29f);
        press(canvas, (int) pix[0], (int) pix[1], 1);
        canvas.getActionMap().get("rotateRight").actionPerformed(new ActionEvent(canvas, 0, "rotateRight"));
        assertEquals(NpcFacing.right(0f), canvas.selectedSpawn().o(), 1e-5f);
        canvas.getActionMap().get("rotateLeft").actionPerformed(new ActionEvent(canvas, 0, "rotateLeft"));
        assertEquals(0f, canvas.selectedSpawn().o(), 1e-5f);
    }

    @Test
    void arrowWhenNothingSelectedShouldLeaveFacing() {
        QuestMapCanvas canvas = sunstrider(new MapSpawnLayer.Pin(
                MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast", 10349.6f, -6357.29f, 33f, 0.4f));
        canvas.getActionMap().get("rotateRight").actionPerformed(new ActionEvent(canvas, 0, "rotateRight"));
        assertEquals(0.4f, canvas.spawns().get(0).o(), 1e-5f);
    }

    private static QuestMapCanvas sunstrider(MapSpawnLayer.Pin... pins) {
        QuestMapCanvas canvas = new QuestMapCanvas();
        canvas.loadRegion(WorldMapAreas.SUNSTRIDER,
                org.tbc.world.map.RegionMinimap.render(WorldMapAreas.SUNSTRIDER,
                        org.tbc.world.map.Terrain.NONE, 200, 100));
        canvas.setSize(200, 100);
        canvas.model().setTool(QuestMapModel.Tool.PLACE_NOTE);
        canvas.setSpawns(List.of(pins));
        return canvas;
    }

    private static void press(QuestMapCanvas canvas, int x, int y, int clicks) {
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0L, 0,
                x, y, clicks, false, MouseEvent.BUTTON1));
    }

    private static void drag(QuestMapCanvas canvas, int x, int y) {
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 0L,
                MouseEvent.BUTTON1_DOWN_MASK, x, y, 0, false, MouseEvent.NOBUTTON));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 0L, 0,
                x, y, 1, false, MouseEvent.BUTTON1));
    }
}
