package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMapModelTest {
    @Test
    void clickEmptyWhenSelectToolShouldNotPlace() {
        QuestMapModel model = new QuestMapModel();
        model.setTool(QuestMapModel.Tool.SELECT);
        assertNull(model.clickWorld(10, 20));
        assertTrue(model.markers().isEmpty());
    }

    @Test
    void clickEmptyWhenPlaceGiverShouldAddMarkerAndUndoRedo() {
        QuestMapModel model = new QuestMapModel();
        model.setTool(QuestMapModel.Tool.PLACE_GIVER);
        QuestMapModel.Marker placed = model.clickWorld(-9465, 62);
        assertEquals(QuestMapModel.MarkerKind.GIVER, placed.kind());
        assertEquals(-9465, placed.x(), 1e-6);
        assertEquals(62, placed.y(), 1e-6);
        assertEquals(0f, placed.orientation());
        model.undo();
        assertTrue(model.markers().isEmpty());
        model.redo();
        assertEquals(1, model.markers().size());
    }

    @Test
    void dragMoveNudgeAndNumericXyShouldUpdateSelectedMarker() {
        QuestMapModel model = new QuestMapModel();
        model.setTool(QuestMapModel.Tool.PLACE_GIVER);
        QuestMapModel.Marker m = model.clickWorld(0, 0);
        model.setTool(QuestMapModel.Tool.SELECT);
        model.clickWorld(0.1, 0.1);
        model.beginDrag(m.id());
        model.dragTo(5, 7);
        model.endDrag();
        assertEquals(5, model.selected().x(), 1e-6);
        assertEquals(7, model.selected().y(), 1e-6);
        model.nudge(1, -2);
        assertEquals(6, model.selected().x(), 1e-6);
        assertEquals(5, model.selected().y(), 1e-6);
        model.setNumericXy(9, 11);
        assertEquals(9, model.selected().x(), 1e-6);
        assertEquals(11, model.selected().y(), 1e-6);
    }

    @Test
    void setOrientationShouldStoreServerRadiansDefaultingNorthZero() {
        QuestMapModel model = new QuestMapModel();
        model.setTool(QuestMapModel.Tool.PLACE_TURN_IN);
        model.clickWorld(1, 1);
        assertEquals(0f, model.selected().orientation());
        model.setOrientation(1.25f);
        assertEquals(1.25f, model.selected().orientation(), 1e-6f);
        OverlayCalibration cal = OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(0, 0, 0, 0),
                new OverlayCalibration.Anchor(10, 0, 10, 0)));
        model.setCalibration(cal);
        model.setOrientationFromPixelDelta(0, -10);
        assertEquals((float) Math.atan2(-10, 0), model.selected().orientation(), 1e-5f);
    }

    @Test
    void visibilityFilterShouldHideKindWithoutRemovingIt() {
        QuestMapModel model = new QuestMapModel();
        model.setTool(QuestMapModel.Tool.PLACE_NOTE);
        model.clickWorld(3, 4);
        model.setKindVisible(QuestMapModel.MarkerKind.NOTE, false);
        assertEquals(1, model.markers().size());
        assertEquals(0, model.visibleMarkers().size());
        model.fitToMarkers();
        assertTrue(model.zoom() > 0);
    }
}
