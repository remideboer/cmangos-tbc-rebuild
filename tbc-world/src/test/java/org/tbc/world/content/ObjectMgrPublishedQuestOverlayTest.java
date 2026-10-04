package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMgrPublishedQuestOverlayTest {
    @TempDir
    Path tmp;

    @Test
    void loadPublishedQuestOverlaysWhenYamlPresentShouldInstallQuestNpcAndSpawnWithoutDb() throws Exception {
        Path published = tmp.resolve("published");
        Files.createDirectories(published);
        Files.writeString(published.resolve("95020.yaml"), """
                kind: quest
                id: 95020
                title: Overlay Quest
                details: Details
                objectives: Objectives
                minLevel: 1
                questLevel: 1
                zoneOrSort: 12
                giverNpc: 95021
                turnInNpc: 95021
                reqCreature:
                  - {id: 6, count: 4}
                creatures:
                  - {entry: 95021, name: Overlay Clerk, display: 1290, faction: 12, npcFlags: 3, movementType: 0}
                spawns:
                  - {guid: 195021, entry: 95021, map: 0, worldX: -9465.0, worldY: 62.0, worldZ: 56.0, orientation: 0.0}
                editor:
                  notes: must-not-matter
                """);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.loadPublishedQuestOverlays(published);
        ObjectMgr.QuestTemplate q = mgr.quests.get(95020);
        assertNotNull(q);
        assertEquals("Overlay Quest", q.title());
        assertEquals(6, q.reqCreatureOrGOId1());
        assertEquals(4, q.reqCreatureOrGOCount1());
        assertTrue(mgr.questGivers.get(95021).contains(95020));
        assertEquals("Overlay Clerk", mgr.creatures.get(95021).name());
        assertTrue(mgr.spawns.stream().anyMatch(s -> s.guid() == 195021 && s.z() == 56f));
    }

    @Test
    void loadPublishedQuestOverlaysWhenDirMissingShouldNoOp() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        int quests = mgr.quests.size();
        mgr.loadPublishedQuestOverlays(tmp.resolve("missing"));
        assertEquals(quests, mgr.quests.size());
    }
}
