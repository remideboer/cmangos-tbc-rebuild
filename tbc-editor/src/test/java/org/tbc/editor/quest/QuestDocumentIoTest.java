package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDocumentIoTest {
    @TempDir
    Path tmp;

    @Test
    void saveDraftWhenReloadedShouldKeepEditorMetadataAndPublishShouldStripIt() throws Exception {
        Path root = tmp.resolve("content");
        QuestYamlStore store = new QuestYamlStore(root);
        QuestDocument doc = QuestDocument.newOwned(95001);
        doc.setTitle("Goldshire Errand");
        doc.setDetails("Speak with the clerk.");
        doc.setObjectives("Speak with the clerk.");
        doc.setMinLevel(1);
        doc.setQuestLevel(1);
        doc.setZoneOrSort(12);
        doc.setGiverNpc(95002);
        doc.setTurnInNpc(95002);
        doc.editor().setOverlayPath("C:/maps/elwynn.png");
        doc.editor().setOverlayHash("abc");
        doc.editor().setNotes("author reminder");
        doc.editor().setCalibrationAnchors("""
                - {px: 0, py: 0, x: 0, y: 0}
                - {px: 10, py: 0, x: 10, y: 0}
                """);
        QuestDocument.MarkerSurface surf = new QuestDocument.MarkerSurface("m1", 56.1f, true);
        doc.editor().surfaces().add(surf);

        store.saveDraft(doc);
        QuestDocument loaded = store.loadDraft(95001);
        assertEquals("Goldshire Errand", loaded.title());
        assertEquals("C:/maps/elwynn.png", loaded.editor().overlayPath());
        assertEquals("abc", loaded.editor().overlayHash());
        assertEquals("author reminder", loaded.editor().notes());
        assertEquals(56.1f, loaded.editor().surfaces().get(0).hintZ(), 1e-4f);
        assertTrue(loaded.editor().surfaces().get(0).manualZ());

        String published = store.runtimeYaml(doc);
        assertFalse(published.contains("editor:"));
        assertFalse(published.contains("author reminder"));
        assertTrue(published.contains("title:"));
        store.publishAtomic(doc);
        String onDisk = Files.readString(store.publishedFile(95001));
        assertFalse(onDisk.contains("editor:"));
        assertTrue(Files.exists(store.publishedFile(95001)));
        assertTrue(Files.notExists(store.publishedFile(95001).resolveSibling("95001.yaml.tmp")));
    }

    @Test
    void saveDraftWhenGraphLayoutShouldRoundTripAndStayOffPublishedYaml() throws Exception {
        QuestYamlStore store = new QuestYamlStore(tmp.resolve("content"));
        QuestDocument doc = QuestDocument.newOwned(95040);
        doc.setTitle("Graph");
        doc.setDetails("d");
        doc.setObjectives("o");
        doc.setGiverNpc(823);
        doc.setTurnInNpc(823);
        doc.editor().graph().setView(4, 5, 1.5);
        doc.editor().graph().collapsed().add("quest:95040");
        doc.editor().graph().nodes().add(new QuestDocument.GraphNodePos("quest:95040", 12, 34));

        store.saveDraft(doc);
        QuestDocument loaded = store.loadDraft(95040);
        assertEquals(4, loaded.editor().graph().panX(), 1e-6);
        assertEquals(5, loaded.editor().graph().panY(), 1e-6);
        assertEquals(1.5, loaded.editor().graph().zoom(), 1e-6);
        assertEquals("quest:95040", loaded.editor().graph().collapsed().get(0));
        assertEquals(12, loaded.editor().graph().nodes().get(0).x(), 1e-6);
        assertEquals(34, loaded.editor().graph().nodes().get(0).y(), 1e-6);

        String published = store.runtimeYaml(doc);
        assertFalse(published.contains("graph:"));
        assertFalse(published.contains("quest:95040"));
        store.publishAtomic(doc);
        String onDisk = Files.readString(store.publishedFile(95040));
        assertFalse(onDisk.contains("graph:"));
    }
}
