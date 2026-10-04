package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestValidatorTest {
    @Test
    void validateWhenMissingGiverAndBadCalibrationShouldError() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestDocument doc = QuestDocument.newOwned(95030);
        doc.setTitle("x");
        doc.setDetails("d");
        doc.setObjectives("o");
        doc.setMinLevel(1);
        doc.setMapId(0);
        QuestValidator.Report r = new QuestValidator(mgr, org.tbc.world.map.MapSurfaceService.unavailable())
                .validate(doc);
        assertTrue(r.hasErrors());
        assertTrue(r.issues().stream().anyMatch(i -> i.message().contains("giver")));
    }

    @Test
    void validateWhenKillObjectiveWithoutCountShouldError() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestDocument doc = QuestDocument.newOwned(95031);
        doc.setTitle("x");
        doc.setDetails("d");
        doc.setObjectives("o");
        doc.setMinLevel(1);
        doc.setMapId(0);
        doc.setGiverNpc(823);
        doc.setTurnInNpc(823);
        doc.setReqCreature(0, 6, 0);
        QuestValidator.Report r = new QuestValidator(mgr, MapSurfaceServiceSupport.uniqueAdt(50f)).validate(doc);
        assertTrue(r.issues().stream().anyMatch(i -> i.message().toLowerCase().contains("count")));
        assertFalse(r.issues().isEmpty());
        assertEquals(QuestValidator.Severity.ERROR, r.issues().get(0).severity());
    }

    @Test
    void validateWhenGiverWithoutTurnInShouldWarnDisconnectedFlow() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestDocument doc = QuestDocument.newOwned(95032);
        doc.setTitle("x");
        doc.setDetails("d");
        doc.setObjectives("o");
        doc.setMinLevel(1);
        doc.setMapId(0);
        doc.setGiverNpc(823);
        doc.setTurnInNpc(0);
        QuestValidator.Report r = new QuestValidator(mgr, MapSurfaceServiceSupport.uniqueAdt(50f)).validate(doc);
        assertTrue(r.issues().stream().anyMatch(i ->
                i.severity() == QuestValidator.Severity.WARNING && i.message().toLowerCase().contains("disconnected")));
    }

    @Test
    void validateWhenPrevCycleShouldErrorAndBlockPublish() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        QuestDocument a = ready(95033);
        QuestDocument b = ready(95034);
        a.setPrevQuestId(b.id());
        b.setPrevQuestId(a.id());
        QuestValidator validator = new QuestValidator(mgr, MapSurfaceServiceSupport.uniqueAdt(50f));
        QuestValidator.Report r = validator.validate(a, java.util.List.of(a, b));
        assertTrue(r.hasErrors());
        assertTrue(r.issues().stream().anyMatch(i -> i.message().toLowerCase().contains("cycle")));
    }

    private static QuestDocument ready(int id) {
        QuestDocument doc = QuestDocument.newOwned(id);
        doc.setTitle("q" + id);
        doc.setDetails("d");
        doc.setObjectives("o");
        doc.setMinLevel(1);
        doc.setMapId(0);
        doc.setGiverNpc(823);
        doc.setTurnInNpc(823);
        return doc;
    }
}
