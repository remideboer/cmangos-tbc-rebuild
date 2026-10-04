package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestGraphCanvasMouseTest {
    @Test
    void middleMouseDragShouldPanTheGraph() {
        QuestDocument focus = quest();
        QuestGraphCanvas canvas = canvas(focus);
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON2, 40, 40, 1));
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_DRAGGED, MouseEvent.BUTTON2, 55, 48, 1));
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON2, 55, 48, 1));
        assertEquals(15, focus.editor().graph().panX(), 1e-6);
        assertEquals(8, focus.editor().graph().panY(), 1e-6);
    }

    @Test
    void leftDragShouldMoveLayoutWithoutChangingPrev() {
        QuestDocument focus = quest();
        focus.setPrevQuestId(95000);
        QuestGraphCanvas canvas = canvas(focus);
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 200, 150, 1));
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_DRAGGED, MouseEvent.BUTTON1, 230, 160, 1));
        assertEquals(95000, focus.prevQuestId());
        assertEquals(30, canvas.model().node("quest:95001").x(), 1e-6);
        assertEquals(10, canvas.model().node("quest:95001").y(), 1e-6);
    }

    @Test
    void rightClickConnectShouldLinkPrerequisite() {
        QuestDocument focus = quest();
        QuestGraphCanvas canvas = canvas(focus);
        canvas.armPrerequisite(95000);
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3, 200, 150, 1));
        clickMenu(canvas.contextMenu(), "Connect as prerequisite");
        assertEquals(95000, focus.prevQuestId());
        assertTrue(canvas.model().edges().stream().anyMatch(e -> e.kind() == QuestGraphModel.EdgeKind.PREV));
    }

    @Test
    void rightClickCreateChildShouldInvokeFactoryWithoutPublishing() {
        QuestDocument focus = quest();
        QuestGraphCanvas canvas = canvas(focus);
        boolean[] called = {false};
        canvas.setCreateChild(() -> called[0] = true);
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3, 200, 150, 1));
        clickMenu(canvas.contextMenu(), "Create child quest");
        assertTrue(called[0]);
        assertEquals(0, focus.prevQuestId());
    }

    @Test
    void doubleClickGiverShouldRequestMap() {
        QuestDocument focus = quest();
        focus.setGiverNpc(823);
        QuestGraphCanvas canvas = canvas(focus);
        int[] jumps = {0};
        canvas.setJumpToMap(() -> jumps[0]++);
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON1, 20, 150, 2));
        assertEquals(1, jumps[0]);
    }

    private static QuestGraphCanvas canvas(QuestDocument focus) {
        QuestGraphModel model = new QuestGraphModel();
        model.rebuild(focus, List.of(focus));
        QuestGraphCanvas canvas = new QuestGraphCanvas();
        canvas.setModel(model);
        canvas.setSize(400, 300);
        return canvas;
    }

    private static QuestDocument quest() {
        QuestDocument doc = QuestDocument.newOwned(95001);
        doc.setTitle("Errand");
        return doc;
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

    private static MouseEvent mouse(QuestGraphCanvas c, int id, int button, int x, int y, int clicks) {
        int mask = switch (button) {
            case MouseEvent.BUTTON1 -> MouseEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2 -> MouseEvent.BUTTON2_DOWN_MASK;
            default -> MouseEvent.BUTTON3_DOWN_MASK;
        };
        return new MouseEvent(c, id, 0L, mask, x, y, clicks, button == MouseEvent.BUTTON3, button);
    }
}
