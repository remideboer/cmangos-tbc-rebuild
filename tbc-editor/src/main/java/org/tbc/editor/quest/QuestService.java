package org.tbc.editor.quest;

import org.tbc.editor.EditorException;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.FloorCandidates;
import org.tbc.world.map.MapSurfaceService;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Public quest-authoring API used by {@link QuestDomain}. */
public final class QuestService {
    public record CreatureHit(int entry, String name, int faction, int npcFlags, float x, float y) {}

    private final ObjectMgr mgr;
    private final QuestYamlStore store;
    private final MapSurfaceService surfaces;
    private final QuestValidator validator;

    public QuestService(ObjectMgr mgr, java.nio.file.Path contentRoot) {
        this(mgr, contentRoot, MapSurfaceService.unavailable());
    }

    public QuestService(ObjectMgr mgr, java.nio.file.Path contentRoot, MapSurfaceService surfaces) {
        this.mgr = mgr;
        this.store = new QuestYamlStore(contentRoot);
        this.surfaces = surfaces == null ? MapSurfaceService.unavailable() : surfaces;
        this.validator = new QuestValidator(mgr, this.surfaces);
    }

    public QuestYamlStore store() {
        return store;
    }

    public QuestDocument create(int id) {
        if (!QuestDocument.ownedRange(id)) {
            throw new EditorException("Quest id must be in the server-owned range 95000–99999.");
        }
        return QuestDocument.newOwned(id);
    }

    public void saveDraft(QuestDocument doc) {
        store.saveDraft(doc);
    }

    public QuestDocument loadDraft(int id) {
        return store.loadDraft(id);
    }

    public QuestValidator.Report validate(QuestDocument doc) {
        return validator.validate(doc);
    }

    public String previewRuntimeYaml(QuestDocument doc) {
        return store.runtimeYaml(doc);
    }

    public String publishDiff(QuestDocument doc) {
        String next = store.runtimeYaml(doc);
        java.nio.file.Path existing = store.publishedFile(doc.id());
        if (!Files.isRegularFile(existing)) {
            return next;
        }
        try {
            String prev = Files.readString(existing);
            return "--- published\n+++ next\n" + prev + "\n---\n" + next;
        } catch (Exception e) {
            return next;
        }
    }

    public void publish(QuestDocument doc, boolean acknowledgeDuplicateSpawns) {
        QuestValidator.Report report = validator.validate(doc);
        if (report.hasErrors()) {
            throw new EditorException(report.issues().get(0).message());
        }
        if (report.hasDuplicateSpawnWarning() && !acknowledgeDuplicateSpawns) {
            throw new EditorException("Acknowledge duplicate spawn warning before publish.");
        }
        store.publishAtomic(doc);
    }

    public void placeGiver(QuestDocument doc, int mapId, float x, float y) {
        doc.setMapId(mapId);
        FloorCandidates floors = surfaces.candidateFloors(mapId, x, y);
        Float z = null;
        boolean resolved = floors.status() == FloorCandidates.Status.UNIQUE && floors.suggestedZ() != null;
        if (resolved) {
            z = floors.suggestedZ();
        }
        doc.markers().removeIf(m -> m.kind() == QuestMapModel.MarkerKind.GIVER);
        doc.markers().add(new QuestDocument.QuestMarker("giver", QuestMapModel.MarkerKind.GIVER,
                mapId, x, y, z, 0f, doc.giverNpc(), 0, resolved, false));
        if (resolved) {
            int guid = 100000 + doc.id();
            int entry = doc.giverNpc() == 0 ? doc.id() : doc.giverNpc();
            doc.spawns().removeIf(s -> s.guid() == guid);
            doc.spawns().add(new QuestDocument.SpawnDraft(guid, entry, mapId, x, y, z, 0f));
        }
    }

    public List<CreatureHit> lookupCreatures(String query) {
        if (mgr == null) {
            return List.of();
        }
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return List.of();
        }
        List<CreatureHit> out = new ArrayList<>();
        for (ObjectMgr.CreatureTemplate t : mgr.creatures.values()) {
            boolean match = String.valueOf(t.entry()).equals(q)
                    || (t.name() != null && t.name().toLowerCase(Locale.ROOT).contains(q));
            if (!match) {
                continue;
            }
            float sx = 0;
            float sy = 0;
            for (ObjectMgr.Spawn s : mgr.spawns) {
                if (s.entry() == t.entry()) {
                    sx = s.x();
                    sy = s.y();
                    break;
                }
            }
            out.add(new CreatureHit(t.entry(), t.name(), t.faction(), t.npcFlags(), sx, sy));
        }
        return out;
    }
}
