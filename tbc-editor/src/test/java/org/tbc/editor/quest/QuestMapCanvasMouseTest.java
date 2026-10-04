package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.map.AreaTable;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.Terrain;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;
import org.tbc.world.map.WorldMapAreas;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMapCanvasMouseTest {
    @Test
    void middleMouseDragShouldPanTheView() {
        QuestMapCanvas canvas = loadedCanvas();
        canvas.model().pan(0, 0);
        canvas.dispatchEvent(press(canvas, MouseEvent.BUTTON2, 40, 40));
        canvas.dispatchEvent(drag(canvas, MouseEvent.BUTTON2, 55, 48));
        canvas.dispatchEvent(release(canvas, MouseEvent.BUTTON2, 55, 48));
        assertEquals(15, canvas.model().panX(), 1e-6);
        assertEquals(8, canvas.model().panY(), 1e-6);
    }

    @Test
    void leftClickShouldStillPlaceWhenPlaceGiverTool() {
        QuestMapCanvas canvas = loadedCanvas();
        canvas.model().setTool(QuestMapModel.Tool.PLACE_GIVER);
        canvas.dispatchEvent(press(canvas, MouseEvent.BUTTON1, 50, 25));
        canvas.dispatchEvent(release(canvas, MouseEvent.BUTTON1, 50, 25));
        assertEquals(1, canvas.model().markers().size());
        WorldMapArea elwynn = WorldMapAreas.seeded().byAreaId(AreaTable.ELWYNN_FOREST);
        float[] world = new WorldMapAreaMapper(elwynn).toWorld(50, 25, 200, 100);
        assertEquals(world[0], canvas.model().markers().get(0).x(), 1.5);
        assertEquals(world[1], canvas.model().markers().get(0).y(), 1.5);
    }

    @Test
    void rightClickShouldOpenMenuAndPlaceGiverAtClickWithoutLeftPlace() {
        QuestMapCanvas canvas = loadedCanvas();
        canvas.model().setTool(QuestMapModel.Tool.SELECT);
        canvas.dispatchEvent(press(canvas, MouseEvent.BUTTON3, 50, 25));
        canvas.dispatchEvent(release(canvas, MouseEvent.BUTTON3, 50, 25));
        JPopupMenu menu = canvas.contextMenu();
        assertNotNull(menu);
        assertTrue(menu.getComponentCount() >= 8);
        clickMenu(menu, "Place giver");
        assertEquals(1, canvas.model().markers().size());
        assertEquals(QuestMapModel.MarkerKind.GIVER, canvas.model().markers().get(0).kind());
        assertEquals(QuestMapModel.Tool.PLACE_GIVER, canvas.model().tool());
    }

    @Test
    void contextDeleteShouldRemoveSelectedMarker() {
        QuestMapCanvas canvas = loadedCanvas();
        canvas.model().setTool(QuestMapModel.Tool.PLACE_GIVER);
        canvas.model().clickPixel(10, 10);
        assertFalse(canvas.model().markers().isEmpty());
        canvas.dispatchEvent(press(canvas, MouseEvent.BUTTON3, 10, 10));
        clickMenu(canvas.contextMenu(), "Delete selected");
        assertTrue(canvas.model().markers().isEmpty());
    }

    private static QuestMapCanvas loadedCanvas() {
        QuestMapCanvas canvas = new QuestMapCanvas();
        WorldMapArea elwynn = WorldMapAreas.seeded().byAreaId(AreaTable.ELWYNN_FOREST);
        RegionMinimap.Raster raster = RegionMinimap.render(elwynn, Terrain.NONE, 200, 100);
        canvas.loadRegion(elwynn, raster);
        canvas.setSize(200, 100);
        return canvas;
    }

    private static void clickMenu(JPopupMenu menu, String text) {
        for (int i = 0; i < menu.getComponentCount(); i++) {
            if (menu.getComponent(i) instanceof JMenuItem item && text.equals(item.getText())) {
                item.doClick();
                return;
            }
        }
        throw new AssertionError("missing menu item: " + text);
    }

    private static MouseEvent press(QuestMapCanvas c, int button, int x, int y) {
        return mouse(c, MouseEvent.MOUSE_PRESSED, button, x, y);
    }

    private static MouseEvent drag(QuestMapCanvas c, int button, int x, int y) {
        return mouse(c, MouseEvent.MOUSE_DRAGGED, button, x, y);
    }

    private static MouseEvent release(QuestMapCanvas c, int button, int x, int y) {
        return mouse(c, MouseEvent.MOUSE_RELEASED, button, x, y);
    }

    private static MouseEvent mouse(QuestMapCanvas c, int id, int button, int x, int y) {
        int mask = switch (button) {
            case MouseEvent.BUTTON1 -> MouseEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2 -> MouseEvent.BUTTON2_DOWN_MASK;
            default -> MouseEvent.BUTTON3_DOWN_MASK;
        };
        return new MouseEvent(c, id, 0L, mask, x, y, 1, button == MouseEvent.BUTTON3, button);
    }
}
