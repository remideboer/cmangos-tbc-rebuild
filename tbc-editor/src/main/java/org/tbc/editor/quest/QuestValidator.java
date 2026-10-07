package org.tbc.editor.quest;

import org.tbc.world.combat.Factions;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.FloorCandidates;
import org.tbc.world.map.MapSurfaceService;

import java.util.ArrayList;
import java.util.List;

/** Blocking errors vs warnings for quest publish. */
public final class QuestValidator {
    public static final float MAP_HALFSIZE = 533.33333f * 64 / 2;
    public static final float DUPLICATE_SPAWN_YARDS = 5f;

    public enum Severity {
        ERROR,
        WARNING
    }

    public record Issue(Severity severity, String markerId, String message) {}

    public static final class Report {
        private final List<Issue> issues;

        Report(List<Issue> issues) {
            this.issues = List.copyOf(issues);
        }

        public List<Issue> issues() {
            return issues;
        }

        public boolean hasErrors() {
            for (Issue i : issues) {
                if (i.severity() == Severity.ERROR) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasDuplicateSpawnWarning() {
            for (Issue i : issues) {
                if (i.severity() == Severity.WARNING && i.message().toLowerCase().contains("duplicate spawn")) {
                    return true;
                }
            }
            return false;
        }
    }

    private final ObjectMgr mgr;
    private final MapSurfaceService surfaces;
    private final Factions factions;

    public QuestValidator(ObjectMgr mgr, MapSurfaceService surfaces) {
        this.mgr = mgr;
        this.surfaces = surfaces == null ? MapSurfaceService.unavailable() : surfaces;
        this.factions = Factions.seeded();
    }

    public Report validate(QuestDocument doc) {
        return validate(doc, java.util.List.of(doc));
    }

    public Report validate(QuestDocument doc, java.util.List<QuestDocument> chain) {
        List<Issue> issues = new ArrayList<>();
        if (doc == null) {
            issues.add(err(null, "Quest document is missing."));
            return new Report(issues);
        }
        if (!QuestDocument.ownedRange(doc.id())) {
            issues.add(err(null, "Quest id must be in the server-owned range 95000–99999."));
        }
        if (doc.title() == null || doc.title().isBlank()) {
            issues.add(err(null, "Title is required."));
        }
        if (doc.mapId() < 0) {
            issues.add(err(null, "Map id is invalid."));
        }
        if (doc.giverNpc() == 0) {
            issues.add(err(null, "Quest giver NPC is required."));
        } else if (!npcKnown(doc, doc.giverNpc())) {
            issues.add(err(null, "Quest giver NPC is missing from templates."));
        }
        if (doc.turnInNpc() == 0) {
            issues.add(err(null, "Turn-in NPC is required."));
            if (doc.giverNpc() != 0) {
                issues.add(warn("quest:" + doc.id(), "Disconnected flow: giver is set and turn-in is missing."));
            }
        } else if (!npcKnown(doc, doc.turnInNpc())) {
            issues.add(err(null, "Turn-in NPC is missing from templates."));
        }
        for (int i = 0; i < 4; i++) {
            int cid = doc.reqCreatureId(i);
            int cc = doc.reqCreatureCount(i);
            if (cid != 0 && cc <= 0) {
                issues.add(err(null, "Kill objective count must be positive."));
            }
            if (cid != 0 && mgr != null && mgr.creatures.get(cid) == null && !draftNpc(doc, cid)) {
                issues.add(err(null, "Kill target creature is missing."));
            }
            int iid = doc.reqItemId(i);
            int ic = doc.reqItemCount(i);
            if (iid != 0 && ic <= 0) {
                issues.add(err(null, "Item objective count must be positive."));
            }
            if (iid != 0 && mgr != null && mgr.items.get(iid) == null) {
                issues.add(err(null, "Required item reference is missing."));
            }
        }
        if (doc.rewItemId() != 0 && mgr != null && mgr.items.get(doc.rewItemId()) == null) {
            issues.add(err(null, "Reward item reference is missing."));
        }
        if (doc.rewSpell() < 0) {
            issues.add(err(null, "Reward spell id is invalid."));
        }
        boolean prevInChain = chain != null && chain.stream().anyMatch(d -> d.id() == doc.prevQuestId());
        if (doc.prevQuestId() != 0 && !prevInChain && mgr != null && mgr.quests.get(doc.prevQuestId()) == null) {
            issues.add(err(null, "Prerequisite quest reference is missing."));
        }
        if (chain != null && QuestGraphModel.cycleTouches(doc.id(), chain)) {
            issues.add(err("quest:" + doc.id(), "Prerequisite quest chain has a cycle."));
        }
        for (QuestDocument.NpcDraft n : doc.npcDrafts()) {
            if (n.display() <= 0) {
                issues.add(err(null, "NPC draft display/model is missing."));
            }
            if (n.faction() != 0 && factions.get(n.faction()) == null) {
                issues.add(err(null, "NPC draft faction is missing."));
            }
            if (n.movementType() < 0 || n.movementType() > 2) {
                issues.add(err(null, "NPC movement type is unsupported."));
            }
        }
        boolean needsWorldPos = !doc.spawns().isEmpty();
        for (QuestDocument.SpawnDraft s : doc.spawns()) {
            if (!Float.isFinite(s.x()) || !Float.isFinite(s.y()) || !Float.isFinite(s.z()) || !Float.isFinite(s.o())) {
                issues.add(err(null, "Spawn coordinates must be finite."));
            }
            if (Math.abs(s.x()) > MAP_HALFSIZE || Math.abs(s.y()) > MAP_HALFSIZE) {
                issues.add(err(null, "Spawn is outside map bounds."));
            }
            FloorCandidates floors = surfaces.candidateFloors(s.map(), s.x(), s.y());
            if (floors.status() == FloorCandidates.Status.UNRESOLVED) {
                issues.add(err(null, "Required Z is unresolved."));
            } else if (floors.status() == FloorCandidates.Status.AMBIGUOUS
                    && !surfaces.manualZStillValid(s.z(), floors)) {
                issues.add(err(null, "Ambiguous floor requires an explicit Z selection."));
            }
        }
        if (needsWorldPos) {
            // spawn loop already added unresolved
        }
        for (QuestDocument.QuestMarker m : doc.markers()) {
            if (m.kind() == QuestMapModel.MarkerKind.AREA || m.kind() == QuestMapModel.MarkerKind.ROUTE) {
                if (!doc.hasKillOrItemObjective()) {
                    issues.add(err(m.id(), "Area/route markers require a supported kill or item objective."));
                }
            }
            if ((m.kind() == QuestMapModel.MarkerKind.GIVER
                    || m.kind() == QuestMapModel.MarkerKind.TURN_IN
                    || m.kind() == QuestMapModel.MarkerKind.QUEST_OBJECT)
                    && !m.zResolved()) {
                issues.add(err(m.id(), "Required Z is unresolved."));
            }
        }
        if (doc.spawns().isEmpty() && doc.markers().stream().noneMatch(m ->
                m.kind() == QuestMapModel.MarkerKind.GIVER && m.zResolved())) {
            // giver may reuse existing world NPC without new spawn
        }
        detectDuplicateSpawns(doc, issues);
        if (doc.editor().overlayPath() != null && !doc.editor().overlayPath().isBlank()) {
            java.nio.file.Path overlay = java.nio.file.Path.of(doc.editor().overlayPath());
            if (!java.nio.file.Files.isRegularFile(overlay)) {
                issues.add(warn(null, "Overlay image is missing."));
            }
        }
        return new Report(issues);
    }

    private void detectDuplicateSpawns(QuestDocument doc, List<Issue> issues) {
        if (mgr == null) {
            return;
        }
        for (QuestDocument.SpawnDraft s : doc.spawns()) {
            for (ObjectMgr.Spawn existing : mgr.creatureSpawns()) {
                if (existing.map() != s.map()) {
                    continue;
                }
                double d = Math.hypot(existing.x() - s.x(), existing.y() - s.y());
                if (d <= DUPLICATE_SPAWN_YARDS && existing.guid() != s.guid()) {
                    issues.add(warn(null, "Near-duplicate spawn at existing NPC " + existing.entry()));
                    return;
                }
            }
        }
    }

    private boolean npcKnown(QuestDocument doc, int entry) {
        if (draftNpc(doc, entry)) {
            return true;
        }
        return mgr != null && mgr.creatures.get(entry) != null;
    }

    private static boolean draftNpc(QuestDocument doc, int entry) {
        for (QuestDocument.NpcDraft n : doc.npcDrafts()) {
            if (n.entry() == entry) {
                return true;
            }
        }
        return false;
    }

    private static Issue err(String markerId, String message) {
        return new Issue(Severity.ERROR, markerId, message);
    }

    private static Issue warn(String markerId, String message) {
        return new Issue(Severity.WARNING, markerId, message);
    }
}
