package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestGraphModelTest {
    @Test
    void rebuildWhenGiverTurnInPrevAndObjectiveShouldCreateThoseEdges() {
        QuestDocument prev = quest(95000, "Start");
        QuestDocument focus = quest(95001, "Errand");
        focus.setGiverNpc(823);
        focus.setTurnInNpc(824);
        focus.setPrevQuestId(95000);
        focus.setReqCreature(0, 6, 3);
        QuestDocument next = quest(95002, "After");
        next.setPrevQuestId(95001);

        QuestGraphModel graph = new QuestGraphModel();
        graph.rebuild(focus, List.of(prev, focus, next));

        assertTrue(hasEdge(graph, QuestGraphModel.EdgeKind.START, "giver:95001", "quest:95001"));
        assertTrue(hasEdge(graph, QuestGraphModel.EdgeKind.TURN_IN, "quest:95001", "turnin:95001"));
        assertTrue(hasEdge(graph, QuestGraphModel.EdgeKind.PREV, "quest:95000", "quest:95001"));
        assertTrue(hasEdge(graph, QuestGraphModel.EdgeKind.NEXT, "quest:95001", "quest:95002"));
        assertTrue(hasEdge(graph, QuestGraphModel.EdgeKind.OBJECTIVE, "quest:95001", "obj:95001:0"));
        QuestGraphModel.Node giver = graph.node("giver:95001");
        QuestGraphModel.Node quest = graph.node("quest:95001");
        QuestGraphModel.Node turnIn = graph.node("turnin:95001");
        assertTrue(giver.x() < quest.x());
        assertTrue(turnIn.x() > quest.x());
        assertTrue(graph.node("quest:95000").y() < quest.y());
        assertTrue(graph.node("quest:95002").y() > quest.y());
        assertEquals("Errand", quest.title());
        assertEquals("95001", quest.subtitle());
    }

    @Test
    void rebuildWhenCreatureNameKnownShouldPreferNameAndKeepEntrySubtitle() {
        QuestDocument focus = quest(95001, "Errand");
        focus.setGiverNpc(823);
        QuestGraphModel graph = new QuestGraphModel();
        graph.setCreatureNames(java.util.Map.of(823, "Marshal McBride"));
        graph.rebuild(focus, List.of(focus));
        assertEquals("Marshal McBride", graph.node("giver:95001").title());
        assertEquals("823", graph.node("giver:95001").subtitle());
    }

    @Test
    void moveNodeWhenDraggingShouldChangeLayoutOnly() {
        QuestDocument focus = quest(95001, "Errand");
        focus.setPrevQuestId(95000);
        QuestGraphModel graph = new QuestGraphModel();
        graph.rebuild(focus, List.of(focus));
        graph.moveNode("quest:95001", 40, 50);
        assertEquals(95000, focus.prevQuestId());
        assertEquals(40, graph.node("quest:95001").x(), 1e-6);
        assertEquals(50, graph.node("quest:95001").y(), 1e-6);
    }

    @Test
    void linkPrevWhenCalledShouldWriteDocumentAndRebuild() {
        QuestDocument focus = quest(95001, "Errand");
        QuestGraphModel graph = new QuestGraphModel();
        graph.rebuild(focus, List.of(focus));
        graph.linkPrev(95000);
        assertEquals(95000, focus.prevQuestId());
        assertTrue(hasEdge(graph, QuestGraphModel.EdgeKind.PREV, "quest:95000", "quest:95001"));
        graph.detachPrev();
        assertEquals(0, focus.prevQuestId());
        assertFalse(hasEdge(graph, QuestGraphModel.EdgeKind.PREV, "quest:95000", "quest:95001"));
    }

    @Test
    void hasCycleWhenMutualPrevShouldBeTrue() {
        QuestDocument a = quest(95001, "A");
        QuestDocument b = quest(95002, "B");
        a.setPrevQuestId(95002);
        b.setPrevQuestId(95001);
        assertTrue(QuestGraphModel.hasCycle(List.of(a, b)));
        b.setPrevQuestId(0);
        assertFalse(QuestGraphModel.hasCycle(List.of(a, b)));
    }

    @Test
    void rebuildWhenSavedLayoutShouldOverrideRadialPosition() {
        QuestDocument focus = quest(95001, "Errand");
        focus.editor().graph().nodes().add(new QuestDocument.GraphNodePos("quest:95001", 12, 34));
        QuestGraphModel graph = new QuestGraphModel();
        graph.rebuild(focus, List.of(focus));
        assertEquals(12, graph.node("quest:95001").x(), 1e-6);
        assertEquals(34, graph.node("quest:95001").y(), 1e-6);
    }

    private static boolean hasEdge(QuestGraphModel graph, QuestGraphModel.EdgeKind kind, String from, String to) {
        for (QuestGraphModel.Edge e : graph.edges()) {
            if (e.kind() == kind && from.equals(e.from()) && to.equals(e.to())) {
                return true;
            }
        }
        return false;
    }

    private static QuestDocument quest(int id, String title) {
        QuestDocument doc = QuestDocument.newOwned(id);
        doc.setTitle(title);
        return doc;
    }
}
