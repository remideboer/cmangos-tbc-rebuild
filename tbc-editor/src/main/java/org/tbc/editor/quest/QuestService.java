package org.tbc.editor.quest;

import org.tbc.common.DbPool;
import org.tbc.editor.EditorException;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.FloorCandidates;
import org.tbc.world.map.MapSurfaceService;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Public quest-authoring API used by {@link QuestDomain}. */
public final class QuestService {
    public record CreatureHit(int entry, String name, int faction, int npcFlags, float x, float y, int level, int type,
                              String factionName) {
        public CreatureHit(int entry, String name, int faction, int npcFlags, float x, float y, int level, int type) {
            this(entry, name, faction, npcFlags, x, y, level, type, "");
        }

        public String label() {
            return name + " (" + entry + ")  " + creatureTypeName(type) + "  lv " + level + "  "
                    + NpcFactions.label(faction, factionName);
        }
    }

    public record ZoneQuest(int id, String title, boolean draft) {
        @Override
        public String toString() {
            return (title == null || title.isBlank() ? "Quest" : title) + " (" + id + ")" + (draft ? "  draft" : "");
        }
    }

    public record ZoneNpc(int entry, String name, int level, int faction, String factionName) {
        public ZoneNpc(int entry, String name, int level, int faction) {
            this(entry, name, level, faction, "");
        }

        @Override
        public String toString() {
            return name + " (" + entry + ")  lv " + level + "  " + NpcFactions.label(faction, factionName);
        }
    }

    /** type -1 means any creature type. Null level or faction means unbounded. */
    public record CreatureQuery(String text, int type, Integer minLevel, Integer maxLevel, Integer faction,
                                WorldMapArea zone) {}

    private final ObjectMgr mgr;
    private final QuestYamlStore store;
    private final MapSurfaceService surfaces;
    private final QuestValidator validator;
    private final DbPool world;
    private NpcFactions factionNames = NpcFactions.empty();

    public QuestService(ObjectMgr mgr, java.nio.file.Path contentRoot) {
        this(mgr, contentRoot, MapSurfaceService.unavailable());
    }

    public QuestService(ObjectMgr mgr, java.nio.file.Path contentRoot, MapSurfaceService surfaces) {
        this(mgr, contentRoot, surfaces, null);
    }

    public QuestService(ObjectMgr mgr, java.nio.file.Path contentRoot, MapSurfaceService surfaces, DbPool world) {
        this.mgr = mgr;
        this.store = new QuestYamlStore(contentRoot);
        this.surfaces = surfaces == null ? MapSurfaceService.unavailable() : surfaces;
        this.validator = new QuestValidator(mgr, this.surfaces);
        this.world = world;
    }

    public ObjectMgr creatures() {
        return mgr;
    }

    public MapSurfaceService surfaces() {
        return surfaces;
    }

    public void setFactionNames(NpcFactions factionNames) {
        this.factionNames = factionNames == null ? NpcFactions.empty() : factionNames;
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
        return validator.validate(doc, chainFor(doc));
    }

    private List<QuestDocument> chainFor(QuestDocument doc) {
        List<QuestDocument> chain = new ArrayList<>();
        try {
            for (QuestDocument other : store.loadAllDrafts()) {
                chain.add(other.id() == doc.id() ? doc : other);
            }
        } catch (RuntimeException ignored) {
            chain.clear();
        }
        if (chain.stream().noneMatch(d -> d.id() == doc.id())) {
            chain.add(doc);
        }
        return chain;
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
            out.add(hit(t, sx, sy));
        }
        return out;
    }

    public List<ZoneQuest> questsInArea(WorldMapArea area) {
        if (area == null || mgr == null) {
            return List.of();
        }
        Map<Integer, ZoneQuest> byId = new LinkedHashMap<>();
        for (ObjectMgr.QuestTemplate t : mgr.quests.values()) {
            if (t.zoneOrSort() == area.areaId()) {
                byId.put(t.id(), new ZoneQuest(t.id(), t.title(), false));
            }
        }
        for (QuestDocument d : store.loadAllDrafts()) {
            if (d.zoneOrSort() == area.areaId()) {
                byId.put(d.id(), new ZoneQuest(d.id(), d.title(), true));
            }
        }
        List<ZoneQuest> out = new ArrayList<>(byId.values());
        out.sort(Comparator.comparing(z -> z.title() == null ? "" : z.title(), String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<ZoneNpc> npcsInArea(WorldMapArea area) {
        if (area == null || mgr == null) {
            return List.of();
        }
        Map<Integer, ZoneNpc> byEntry = new LinkedHashMap<>();
        for (ObjectMgr.Spawn s : mgr.spawns) {
            if (!spawnInArea(s, area)) {
                continue;
            }
            ObjectMgr.CreatureTemplate t = mgr.creatures.get(s.entry());
            String name = t == null || t.name() == null || t.name().isBlank() ? "NPC " + s.entry() : t.name();
            int level = t == null ? 0 : t.level();
            int faction = t == null ? 0 : t.faction();
            byEntry.putIfAbsent(s.entry(), new ZoneNpc(s.entry(), name, level, faction, factionNames.name(faction)));
        }
        List<ZoneNpc> out = new ArrayList<>(byEntry.values());
        out.sort(Comparator.comparing(ZoneNpc::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<CreatureHit> searchCreatures(CreatureQuery query) {
        if (mgr == null || query == null) {
            return List.of();
        }
        String text = query.text() == null ? "" : query.text().trim().toLowerCase(Locale.ROOT);
        boolean anyFilter = !text.isEmpty() || query.type() >= 0 || query.minLevel() != null
                || query.maxLevel() != null || query.faction() != null || query.zone() != null;
        if (!anyFilter) {
            return List.of();
        }
        List<CreatureHit> out = new ArrayList<>();
        for (ObjectMgr.CreatureTemplate t : mgr.creatures.values()) {
            if (!text.isEmpty()) {
                boolean match = String.valueOf(t.entry()).equals(text)
                        || (t.name() != null && t.name().toLowerCase(Locale.ROOT).contains(text));
                if (!match) {
                    continue;
                }
            }
            if (query.type() >= 0 && t.type() != query.type()) {
                continue;
            }
            if (query.minLevel() != null && t.level() < query.minLevel()) {
                continue;
            }
            if (query.maxLevel() != null && t.level() > query.maxLevel()) {
                continue;
            }
            if (query.faction() != null && t.faction() != query.faction()) {
                continue;
            }
            float sx = 0;
            float sy = 0;
            boolean inZone = query.zone() == null;
            for (ObjectMgr.Spawn s : mgr.spawns) {
                if (s.entry() != t.entry()) {
                    continue;
                }
                if (query.zone() != null && !spawnInArea(s, query.zone())) {
                    continue;
                }
                sx = s.x();
                sy = s.y();
                inZone = true;
                break;
            }
            if (!inZone) {
                continue;
            }
            out.add(hit(t, sx, sy));
        }
        out.sort(Comparator.comparing(CreatureHit::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public String creatureName(int entry) {
        if (mgr == null) {
            return null;
        }
        ObjectMgr.CreatureTemplate t = mgr.creatures.get(entry);
        if (t == null || t.name() == null || t.name().isBlank()) {
            return null;
        }
        return t.name();
    }

    public int nextOwnedId() {
        int next = QuestDocument.MIN_OWNED_ID;
        for (QuestDocument other : store.loadAllDrafts()) {
            if (other.id() >= next) {
                next = other.id() + 1;
            }
        }
        if (next > QuestDocument.MAX_OWNED_ID) {
            throw new EditorException("No free quest id in the server-owned range.");
        }
        return next;
    }

    public QuestDocument copyCatalogQuest(int questId, int ownedId) {
        if (mgr == null || mgr.quests.get(questId) == null) {
            throw new EditorException("Catalog quest " + questId + " is not loaded.");
        }
        int id = ownedId > 0 ? ownedId : nextOwnedId();
        if (!QuestDocument.ownedRange(id)) {
            throw new EditorException("Quest id must be in the server-owned range 95000–99999.");
        }
        ObjectMgr.QuestTemplate t = mgr.quests.get(questId);
        QuestDocument doc = create(id);
        doc.setTitle(t.title());
        doc.setDetails(t.details());
        doc.setObjectives(t.objectives());
        doc.setMinLevel(t.minLevel());
        doc.setQuestLevel(t.questLevel());
        doc.setType(t.type());
        doc.setRewMoney(t.rewMoney());
        doc.setPrevQuestId(t.prevQuestId());
        doc.setRequiredRaces(t.requiredRaces());
        doc.setZoneOrSort(t.zoneOrSort());
        doc.setReqCreature(0, t.reqCreatureOrGOId1(), t.reqCreatureOrGOCount1());
        doc.setReqCreature(1, t.reqCreatureOrGOId2(), t.reqCreatureOrGOCount2());
        doc.setReqCreature(2, t.reqCreatureOrGOId3(), t.reqCreatureOrGOCount3());
        doc.setReqCreature(3, t.reqCreatureOrGOId4(), t.reqCreatureOrGOCount4());
        doc.setReqItem(0, t.reqItemId1(), t.reqItemCount1());
        doc.setReqItem(1, t.reqItemId2(), t.reqItemCount2());
        doc.setReqItem(2, t.reqItemId3(), t.reqItemCount3());
        doc.setReqItem(3, t.reqItemId4(), t.reqItemCount4());
        doc.setGiverNpc(relationEntry(mgr.questGivers, questId));
        doc.setTurnInNpc(relationEntry(mgr.questInvolved, questId));
        return doc;
    }

    public static String creatureTypeName(int type) {
        return switch (type) {
            case 1 -> "Beast";
            case 2 -> "Dragonkin";
            case 3 -> "Demon";
            case 4 -> "Elemental";
            case 5 -> "Giant";
            case 6 -> "Undead";
            case 7 -> "Humanoid";
            case 8 -> "Critter";
            case 9 -> "Mechanical";
            case 11 -> "Totem";
            case 12 -> "Non-combat Pet";
            case 13 -> "Gas Cloud";
            default -> "Type " + type;
        };
    }

    private static int relationEntry(Map<Integer, List<Integer>> relations, int questId) {
        for (Map.Entry<Integer, List<Integer>> e : relations.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(questId)) {
                return e.getKey();
            }
        }
        return 0;
    }

    private static boolean spawnInArea(ObjectMgr.Spawn spawn, WorldMapArea area) {
        if (spawn.map() != area.mapId()) {
            return false;
        }
        return new WorldMapAreaMapper(area).contains(spawn.x(), spawn.y());
    }

    /**
     * Writes moved guids and edited templates, then updates the in-memory catalog.
     * A logged-in world process keeps its own copy until it restarts.
     */
    public NpcEditStore.Result saveNpcEdits(NpcEditSession session) {
        if (session == null || !session.dirty()) {
            return new NpcEditStore.Result(0, 0, 0);
        }
        if (world == null) {
            throw new EditorException("No world database.");
        }
        try (Connection c = world.get()) {
            NpcEditStore.Result written = NpcEditStore.save(c, session,
                    guid -> mgr == null ? null : mgr.creatureSpawn(guid),
                    guid -> {
                        if (mgr != null) {
                            mgr.markDbCreature(guid);
                        }
                    });
            applyNpcEdits(session);
            session.accept();
            return written;
        } catch (SQLException e) {
            throw new EditorException(e.getMessage() == null ? "NPC save failed." : e.getMessage());
        }
    }

    /** Replace matching spawns and templates in memory. Does not touch MySQL. */
    public void applyNpcEdits(NpcEditSession session) {
        if (session == null || mgr == null) {
            return;
        }
        for (NpcEditSession.Pose pose : session.moved()) {
            for (int i = 0; i < mgr.spawns.size(); i++) {
                ObjectMgr.Spawn spawn = mgr.spawns.get(i);
                if (spawn.guid() != pose.guid()) {
                    continue;
                }
                mgr.spawns.set(i, new ObjectMgr.Spawn(spawn.guid(), spawn.entry(), spawn.map(),
                        pose.x(), pose.y(), pose.z(), spawn.o(), spawn.spawnDist(), spawn.movementType(),
                        spawn.respawnMinSecs(), spawn.respawnMaxSecs()));
                break;
            }
        }
        for (NpcEditSession.Look look : session.changedLooks()) {
            ObjectMgr.CreatureTemplate template = mgr.creatures.get(look.entry());
            if (template != null) {
                mgr.creatures.put(look.entry(), template.withEdited(
                        look.name(), look.displayId(), look.creatureType(), look.faction()));
            }
            if (look.equipmentId() <= 0) {
                mgr.equipmentByEntry.remove(look.entry());
            } else {
                mgr.equipmentByEntry.put(look.entry(), look.equipmentId());
            }
        }
    }

    private CreatureHit hit(ObjectMgr.CreatureTemplate t, float x, float y) {
        return new CreatureHit(t.entry(), t.name(), t.faction(), t.npcFlags(), x, y, t.level(), t.type(),
                factionNames.name(t.faction()));
    }
}
