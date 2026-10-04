package org.tbc.editor.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.editor.EditorException;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestServiceTest {
    @TempDir
    Path tmp;

    private ObjectMgr mgr;
    private QuestService svc;

    @BeforeEach
    void setUp() {
        mgr = new ObjectMgr();
        mgr.load(null, null);
        svc = new QuestService(mgr, tmp.resolve("content"), MapSurfaceServiceSupport.uniqueAdt(56f));
    }

    @Test
    void createWhenOwnedIdShouldInitializeDraftDocument() {
        QuestDocument doc = svc.create(95010);
        assertEquals(95010, doc.id());
        assertEquals(0, doc.minLevel());
        assertTrue(doc.title().isEmpty());
    }

    @Test
    void createWhenHeroRangeIdShouldRefuse() {
        assertThrows(EditorException.class, () -> svc.create(90001));
        assertThrows(EditorException.class, () -> svc.create(83));
    }

    @Test
    void lookupCreaturesWhenNameOrEntryShouldReturnObjectMgrRows() {
        List<QuestService.CreatureHit> hits = svc.lookupCreatures("willem");
        assertFalse(hits.isEmpty());
        assertEquals(Content.NPC_DEPUTY_WILLEM, hits.get(0).entry());
        assertTrue(svc.lookupCreatures("6").stream().anyMatch(h -> h.entry() == 6));
        assertTrue(svc.lookupCreatures("zzz-nope").isEmpty());
        assertTrue(svc.lookupCreatures(null).isEmpty());
    }

    @Test
    void saveDraftWhenUnresolvedZShouldSucceedAndPublishShouldFail() {
        QuestService unresolved = new QuestService(mgr, tmp.resolve("content"),
                org.tbc.world.map.MapSurfaceService.unavailable());
        QuestDocument doc = validGiver(unresolved.create(95011));
        unresolved.placeGiver(doc, 0, -9465, 62);
        unresolved.saveDraft(doc);
        assertTrue(Files.isRegularFile(unresolved.store().draftFile(95011)));
        QuestValidator.Report report = unresolved.validate(doc);
        assertTrue(report.hasErrors());
        assertThrows(EditorException.class, () -> unresolved.publish(doc, true));
    }

    @Test
    void publishWhenValidShouldWriteOverlayObjectMgrCanLoad() {
        QuestDocument doc = validGiver(svc.create(95012));
        doc.npcDrafts().add(new QuestDocument.NpcDraft(95012, "Goldshire Clerk", 1290, 12,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, 0));
        doc.setGiverNpc(95012);
        doc.setTurnInNpc(95012);
        svc.placeGiver(doc, 0, -9465, 62);
        svc.publish(doc, true);
        ObjectMgr fresh = new ObjectMgr();
        fresh.load(null, null);
        fresh.loadPublishedQuestOverlays(svc.store().publishedDir());
        ObjectMgr.QuestTemplate q = fresh.quests.get(95012);
        assertEquals("Errand", q.title());
        assertTrue(fresh.questGivers.get(95012).contains(95012));
        assertTrue(fresh.questInvolved.get(95012).contains(95012));
        assertEquals("Goldshire Clerk", fresh.creatures.get(95012).name());
        assertTrue(fresh.spawns.stream().anyMatch(s -> s.entry() == 95012));
    }

    @Test
    void publishWhenAreaMarkerWithoutSupportedObjectiveShouldRefuse() {
        QuestDocument doc = validGiver(svc.create(95013));
        svc.placeGiver(doc, 0, -9465, 62);
        doc.markers().add(new QuestDocument.QuestMarker("area1", QuestMapModel.MarkerKind.AREA,
                0, 1, 2, 56f, 0f, 0, 0, true, false));
        assertThrows(EditorException.class, () -> svc.publish(doc, true));
    }

    @Test
    void publishWhenNearDuplicateSpawnUnackedShouldRefuseAndAckShouldAllow() {
        mgr.spawns.add(new ObjectMgr.Spawn(1, Content.NPC_MARSHAL_DUGHAN, 0, -9465f, 62f, 56f, 0f));
        QuestDocument doc = validGiver(svc.create(95014));
        svc.placeGiver(doc, 0, -9465, 62);
        doc.setGiverNpc(Content.NPC_MARSHAL_DUGHAN);
        doc.setTurnInNpc(Content.NPC_MARSHAL_DUGHAN);
        doc.spawns().add(new QuestDocument.SpawnDraft(195014, Content.NPC_MARSHAL_DUGHAN, 0, -9464.5f, 62f, 56f, 0f));
        assertTrue(svc.validate(doc).hasDuplicateSpawnWarning());
        assertThrows(EditorException.class, () -> svc.publish(doc, false));
        svc.publish(doc, true);
        assertTrue(Files.isRegularFile(svc.store().publishedFile(95014)));
    }

    @Test
    void previewRuntimeYamlShouldOmitEditorSection() {
        QuestDocument doc = validGiver(svc.create(95015));
        doc.editor().setNotes("secret");
        String yaml = svc.previewRuntimeYaml(doc);
        assertFalse(yaml.contains("secret"));
        assertFalse(yaml.contains("editor:"));
        assertTrue(svc.publishDiff(doc).contains("Errand"));
    }

    private static QuestDocument validGiver(QuestDocument doc) {
        doc.setTitle("Errand");
        doc.setDetails("Go.");
        doc.setObjectives("Go.");
        doc.setMinLevel(1);
        doc.setQuestLevel(1);
        doc.setZoneOrSort(12);
        doc.setMapId(0);
        doc.setGiverNpc(Content.NPC_DEPUTY_WILLEM);
        doc.setTurnInNpc(Content.NPC_DEPUTY_WILLEM);
        return doc;
    }
}
