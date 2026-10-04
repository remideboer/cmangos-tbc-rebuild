package org.tbc.editor.quest;

import org.tbc.world.content.QuestOverlayYaml;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Atomic draft/publish YAML under {@code content/quests}. */
public final class QuestYamlStore {
    private final Path contentRoot;

    public QuestYamlStore(Path contentRoot) {
        this.contentRoot = contentRoot;
    }

    public Path draftsDir() {
        return contentRoot.resolve("quests").resolve("drafts");
    }

    public Path publishedDir() {
        return contentRoot.resolve("quests").resolve("published");
    }

    public Path draftFile(int id) {
        return draftsDir().resolve(id + ".yaml");
    }

    public Path publishedFile(int id) {
        return publishedDir().resolve(id + ".yaml");
    }

    public void saveDraft(QuestDocument doc) {
        try {
            Files.createDirectories(draftsDir());
            Map<String, Object> map = QuestOverlayYaml.toRuntimeMap(toOverlay(doc));
            map.put("editor", editorMap(doc));
            writeAtomic(draftFile(doc.id()), dump(map));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<QuestDocument> loadAllDrafts() {
        if (!Files.isDirectory(draftsDir())) {
            return List.of();
        }
        List<QuestDocument> out = new ArrayList<>();
        try (var files = Files.list(draftsDir())) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".yaml")).toList()) {
                String name = file.getFileName().toString();
                int id = Integer.parseInt(name.substring(0, name.length() - ".yaml".length()));
                out.add(loadDraft(id));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    public QuestDocument loadDraft(int id) {
        try {
            Path file = draftFile(id);
            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(file)) {
                Object raw = yaml.load(in);
                if (!(raw instanceof Map<?, ?> map)) {
                    throw new IOException(file + ": expected mapping");
                }
                QuestOverlayYaml.Overlay overlay = QuestOverlayYaml.parse(map);
                QuestDocument doc = fromOverlay(overlay);
                Object editor = map.get("editor");
                if (editor instanceof Map<?, ?> em) {
                    doc.editor().setOverlayPath(str(em.get("overlayPath")));
                    doc.editor().setOverlayHash(str(em.get("overlayHash")));
                    doc.editor().setNotes(str(em.get("notes")));
                    doc.editor().setCalibrationAnchors(str(em.get("calibration")));
                    Object surfaces = em.get("surfaces");
                    if (surfaces instanceof List<?> list) {
                        for (Object row : list) {
                            if (row instanceof Map<?, ?> sm) {
                                doc.editor().surfaces().add(new QuestDocument.MarkerSurface(
                                        str(sm.get("markerId")),
                                        floatVal(sm.get("hintZ")),
                                        boolVal(sm.get("manualZ"))));
                            }
                        }
                    }
                    readGraph(doc, em.get("graph"));
                }
                return doc;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String runtimeYaml(QuestDocument doc) {
        return dump(QuestOverlayYaml.toRuntimeMap(toOverlay(doc)));
    }

    public void publishAtomic(QuestDocument doc) {
        try {
            Files.createDirectories(publishedDir());
            writeAtomic(publishedFile(doc.id()), runtimeYaml(doc));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static QuestOverlayYaml.Overlay toOverlay(QuestDocument doc) {
        QuestOverlayYaml.Overlay o = new QuestOverlayYaml.Overlay();
        o.id = doc.id();
        o.title = doc.title();
        o.details = doc.details();
        o.objectives = doc.objectives();
        o.minLevel = doc.minLevel();
        o.questLevel = doc.questLevel();
        o.type = doc.type();
        o.rewMoney = doc.rewMoney();
        o.prevQuestId = doc.prevQuestId();
        o.requiredRaces = doc.requiredRaces();
        o.zoneOrSort = doc.zoneOrSort();
        o.giverNpc = doc.giverNpc();
        o.turnInNpc = doc.turnInNpc();
        o.rewSpell = doc.rewSpell();
        o.rewItemId = doc.rewItemId();
        o.rewItemCount = doc.rewItemCount();
        System.arraycopy(doc.reqCreatureIds(), 0, o.reqCreatureId, 0, 4);
        System.arraycopy(doc.reqCreatureCounts(), 0, o.reqCreatureCount, 0, 4);
        System.arraycopy(doc.reqItemIds(), 0, o.reqItemId, 0, 4);
        System.arraycopy(doc.reqItemCounts(), 0, o.reqItemCount, 0, 4);
        for (QuestDocument.NpcDraft n : doc.npcDrafts()) {
            o.creatures.add(new QuestOverlayYaml.CreatureOverlay(
                    n.entry(), n.name(), n.display(), n.faction(), n.npcFlags(), n.movementType()));
        }
        for (QuestDocument.SpawnDraft s : doc.spawns()) {
            o.spawns.add(new QuestOverlayYaml.SpawnOverlay(
                    s.guid(), s.entry(), s.map(), s.x(), s.y(), s.z(), s.o(), 0));
        }
        return o;
    }

    static QuestDocument fromOverlay(QuestOverlayYaml.Overlay o) {
        QuestDocument doc = QuestDocument.newOwned(o.id);
        doc.setTitle(o.title);
        doc.setDetails(o.details);
        doc.setObjectives(o.objectives);
        doc.setMinLevel(o.minLevel);
        doc.setQuestLevel(o.questLevel);
        doc.setType(o.type);
        doc.setRewMoney(o.rewMoney);
        doc.setPrevQuestId(o.prevQuestId);
        doc.setRequiredRaces(o.requiredRaces);
        doc.setZoneOrSort(o.zoneOrSort);
        doc.setGiverNpc(o.giverNpc);
        doc.setTurnInNpc(o.turnInNpc);
        doc.setRewSpell(o.rewSpell);
        doc.setRewItemId(o.rewItemId);
        doc.setRewItemCount(o.rewItemCount);
        for (int i = 0; i < 4; i++) {
            doc.setReqCreature(i, o.reqCreatureId[i], o.reqCreatureCount[i]);
            doc.setReqItem(i, o.reqItemId[i], o.reqItemCount[i]);
        }
        for (QuestOverlayYaml.CreatureOverlay n : o.creatures) {
            doc.npcDrafts().add(new QuestDocument.NpcDraft(
                    n.entry(), n.name(), n.display(), n.faction(), n.npcFlags(), n.movementType()));
        }
        for (QuestOverlayYaml.SpawnOverlay s : o.spawns) {
            doc.spawns().add(new QuestDocument.SpawnDraft(
                    s.guid(), s.entry(), s.map(), s.x(), s.y(), s.z(), s.o()));
        }
        return doc;
    }

    private static Map<String, Object> editorMap(QuestDocument doc) {
        Map<String, Object> editor = new LinkedHashMap<>();
        editor.put("overlayPath", doc.editor().overlayPath());
        editor.put("overlayHash", doc.editor().overlayHash());
        editor.put("notes", doc.editor().notes());
        editor.put("calibration", doc.editor().calibrationAnchors());
        List<Map<String, Object>> surfaces = new ArrayList<>();
        for (QuestDocument.MarkerSurface s : doc.editor().surfaces()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("markerId", s.markerId());
            row.put("hintZ", s.hintZ());
            row.put("manualZ", s.manualZ());
            surfaces.add(row);
        }
        editor.put("surfaces", surfaces);
        editor.put("graph", graphMap(doc.editor().graph()));
        return editor;
    }

    private static Map<String, Object> graphMap(QuestDocument.GraphView view) {
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("panX", view.panX());
        graph.put("panY", view.panY());
        graph.put("zoom", view.zoom());
        graph.put("collapsed", new ArrayList<>(view.collapsed()));
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (QuestDocument.GraphNodePos pos : view.nodes()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", pos.id());
            row.put("x", pos.x());
            row.put("y", pos.y());
            nodes.add(row);
        }
        graph.put("nodes", nodes);
        return graph;
    }

    private static String dump(Map<String, Object> map) {
        DumperOptions opt = new DumperOptions();
        opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opt.setPrettyFlow(true);
        return new Yaml(opt).dump(map);
    }

    private static void writeAtomic(Path target, String text) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void readGraph(QuestDocument doc, Object raw) {
        if (!(raw instanceof Map<?, ?> graph)) {
            return;
        }
        doc.editor().graph().setView(doubleVal(graph.get("panX")), doubleVal(graph.get("panY")),
                doubleVal(graph.get("zoom")));
        Object collapsed = graph.get("collapsed");
        if (collapsed instanceof List<?> list) {
            for (Object id : list) {
                doc.editor().graph().collapsed().add(str(id));
            }
        }
        Object nodes = graph.get("nodes");
        if (nodes instanceof List<?> list) {
            for (Object row : list) {
                if (row instanceof Map<?, ?> nm) {
                    doc.editor().graph().nodes().add(new QuestDocument.GraphNodePos(
                            str(nm.get("id")), doubleVal(nm.get("x")), doubleVal(nm.get("y"))));
                }
            }
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double doubleVal(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return 0d;
    }

    private static float floatVal(Object o) {
        if (o instanceof Number n) {
            return n.floatValue();
        }
        return 0f;
    }

    private static boolean boolVal(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(String.valueOf(o));
    }
}
