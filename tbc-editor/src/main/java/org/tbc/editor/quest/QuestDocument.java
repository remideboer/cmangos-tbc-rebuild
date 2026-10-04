package org.tbc.editor.quest;

import java.util.ArrayList;
import java.util.List;

/** Authoring document: runtime overlay fields plus editor-only metadata. */
public final class QuestDocument {
    public static final int MIN_OWNED_ID = 95000;
    public static final int MAX_OWNED_ID = 99999;

    public static final class EditorMeta {
        private String overlayPath = "";
        private String overlayHash = "";
        private String notes = "";
        private String calibrationAnchors = "";
        private final List<MarkerSurface> surfaces = new ArrayList<>();
        private final GraphView graph = new GraphView();

        public String overlayPath() {
            return overlayPath;
        }

        public void setOverlayPath(String overlayPath) {
            this.overlayPath = overlayPath == null ? "" : overlayPath;
        }

        public String overlayHash() {
            return overlayHash;
        }

        public void setOverlayHash(String overlayHash) {
            this.overlayHash = overlayHash == null ? "" : overlayHash;
        }

        public String notes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes == null ? "" : notes;
        }

        public String calibrationAnchors() {
            return calibrationAnchors;
        }

        public void setCalibrationAnchors(String calibrationAnchors) {
            this.calibrationAnchors = calibrationAnchors == null ? "" : calibrationAnchors;
        }

        public List<MarkerSurface> surfaces() {
            return surfaces;
        }

        public GraphView graph() {
            return graph;
        }
    }

    public static final class GraphNodePos {
        private final String id;
        private double x;
        private double y;

        public GraphNodePos(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public String id() {
            return id;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final class GraphView {
        private double panX;
        private double panY;
        private double zoom = 1;
        private final List<String> collapsed = new ArrayList<>();
        private final List<GraphNodePos> nodes = new ArrayList<>();

        public double panX() {
            return panX;
        }

        public double panY() {
            return panY;
        }

        public double zoom() {
            return zoom;
        }

        public void setView(double panX, double panY, double zoom) {
            this.panX = panX;
            this.panY = panY;
            this.zoom = zoom <= 0 ? 1 : zoom;
        }

        public List<String> collapsed() {
            return collapsed;
        }

        public List<GraphNodePos> nodes() {
            return nodes;
        }

        public GraphNodePos node(String id) {
            for (GraphNodePos n : nodes) {
                if (id.equals(n.id())) {
                    return n;
                }
            }
            return null;
        }
    }

    public record MarkerSurface(String markerId, float hintZ, boolean manualZ) {}

    public record NpcDraft(int entry, String name, int display, int faction, int npcFlags, int movementType) {}

    public record SpawnDraft(int guid, int entry, int map, float x, float y, float z, float o) {}

    public record QuestMarker(String id, QuestMapModel.MarkerKind kind, int mapId, double x, double y, Float z,
                              float orientation, int refEntry, int count, boolean zResolved, boolean manualZ) {}

    private int id;
    private String title = "";
    private String details = "";
    private String objectives = "";
    private int minLevel;
    private int questLevel;
    private int type;
    private int rewMoney;
    private int prevQuestId;
    private int requiredRaces;
    private int zoneOrSort;
    private int mapId;
    private int giverNpc;
    private int turnInNpc;
    private int rewSpell;
    private int rewItemId;
    private int rewItemCount;
    private final int[] reqCreatureId = new int[4];
    private final int[] reqCreatureCount = new int[4];
    private final int[] reqItemId = new int[4];
    private final int[] reqItemCount = new int[4];
    private final EditorMeta editor = new EditorMeta();
    private final List<NpcDraft> npcDrafts = new ArrayList<>();
    private final List<SpawnDraft> spawns = new ArrayList<>();
    private final List<QuestMarker> markers = new ArrayList<>();

    public static QuestDocument newOwned(int id) {
        QuestDocument d = new QuestDocument();
        d.id = id;
        return d;
    }

    public static boolean ownedRange(int id) {
        return id >= MIN_OWNED_ID && id <= MAX_OWNED_ID;
    }

    public int id() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String details() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details == null ? "" : details;
    }

    public String objectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives == null ? "" : objectives;
    }

    public int minLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int questLevel() {
        return questLevel;
    }

    public void setQuestLevel(int questLevel) {
        this.questLevel = questLevel;
    }

    public int type() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int rewMoney() {
        return rewMoney;
    }

    public void setRewMoney(int rewMoney) {
        this.rewMoney = rewMoney;
    }

    public int prevQuestId() {
        return prevQuestId;
    }

    public void setPrevQuestId(int prevQuestId) {
        this.prevQuestId = prevQuestId;
    }

    public int requiredRaces() {
        return requiredRaces;
    }

    public void setRequiredRaces(int requiredRaces) {
        this.requiredRaces = requiredRaces;
    }

    public int zoneOrSort() {
        return zoneOrSort;
    }

    public void setZoneOrSort(int zoneOrSort) {
        this.zoneOrSort = zoneOrSort;
    }

    public int mapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public int giverNpc() {
        return giverNpc;
    }

    public void setGiverNpc(int giverNpc) {
        this.giverNpc = giverNpc;
    }

    public int turnInNpc() {
        return turnInNpc;
    }

    public void setTurnInNpc(int turnInNpc) {
        this.turnInNpc = turnInNpc;
    }

    public int rewSpell() {
        return rewSpell;
    }

    public void setRewSpell(int rewSpell) {
        this.rewSpell = rewSpell;
    }

    public int rewItemId() {
        return rewItemId;
    }

    public void setRewItemId(int rewItemId) {
        this.rewItemId = rewItemId;
    }

    public int rewItemCount() {
        return rewItemCount;
    }

    public void setRewItemCount(int rewItemCount) {
        this.rewItemCount = rewItemCount;
    }

    public void setReqCreature(int index, int id, int count) {
        if (index >= 0 && index < 4) {
            reqCreatureId[index] = id;
            reqCreatureCount[index] = count;
        }
    }

    public int reqCreatureId(int index) {
        return index >= 0 && index < 4 ? reqCreatureId[index] : 0;
    }

    public int reqCreatureCount(int index) {
        return index >= 0 && index < 4 ? reqCreatureCount[index] : 0;
    }

    public void setReqItem(int index, int id, int count) {
        if (index >= 0 && index < 4) {
            reqItemId[index] = id;
            reqItemCount[index] = count;
        }
    }

    public int reqItemId(int index) {
        return index >= 0 && index < 4 ? reqItemId[index] : 0;
    }

    public int reqItemCount(int index) {
        return index >= 0 && index < 4 ? reqItemCount[index] : 0;
    }

    int[] reqCreatureIds() {
        return reqCreatureId;
    }

    int[] reqCreatureCounts() {
        return reqCreatureCount;
    }

    int[] reqItemIds() {
        return reqItemId;
    }

    int[] reqItemCounts() {
        return reqItemCount;
    }

    public EditorMeta editor() {
        return editor;
    }

    public List<NpcDraft> npcDrafts() {
        return npcDrafts;
    }

    public List<SpawnDraft> spawns() {
        return spawns;
    }

    public List<QuestMarker> markers() {
        return markers;
    }

    public boolean hasKillOrItemObjective() {
        for (int i = 0; i < 4; i++) {
            if (reqCreatureId[i] != 0 || reqItemId[i] != 0) {
                return true;
            }
        }
        return false;
    }
}
