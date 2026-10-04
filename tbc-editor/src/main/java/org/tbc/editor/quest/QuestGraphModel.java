package org.tbc.editor.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Headless questline graph. Positions are authoring layout only; relationship
 * edits write through {@link QuestDocument}.
 */
public final class QuestGraphModel {
    public enum Kind { QUEST, GIVER, TURN_IN, OBJECTIVE, NOTE }

    public enum EdgeKind { START, TURN_IN, PREV, NEXT, OBJECTIVE }

    public static final class Node {
        private final String id;
        private final Kind kind;
        private final int questId;
        private final String title;
        private final String subtitle;
        private final String hover;
        private double x;
        private double y;
        private boolean issue;

        Node(String id, Kind kind, int questId, String title, String subtitle, String hover, double x, double y) {
            this.id = id;
            this.kind = kind;
            this.questId = questId;
            this.title = title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.hover = hover;
            this.x = x;
            this.y = y;
        }

        public String id() {
            return id;
        }

        public Kind kind() {
            return kind;
        }

        public int questId() {
            return questId;
        }

        public String title() {
            return title;
        }

        public String subtitle() {
            return subtitle;
        }

        public String hover() {
            return hover;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public boolean issue() {
            return issue;
        }

        void setLayout(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void setIssue(boolean issue) {
            this.issue = issue;
        }
    }

    public record Edge(String from, String to, EdgeKind kind) {}

    private static final double STEP = 180;

    private QuestDocument focus;
    private List<QuestDocument> chain = List.of();
    private Map<Integer, String> creatureNames = Map.of();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private String selectedId = "";
    private final Set<Integer> issueQuests = new HashSet<>();

    public void setCreatureNames(Map<Integer, String> names) {
        this.creatureNames = names == null ? Map.of() : Map.copyOf(names);
    }

    public void rebuild(QuestDocument focus, List<QuestDocument> chain) {
        this.focus = focus;
        this.chain = chain == null ? List.of() : List.copyOf(chain);
        nodes.clear();
        edges.clear();
        if (focus == null) {
            return;
        }
        Map<Integer, QuestDocument> byId = new HashMap<>();
        for (QuestDocument d : this.chain) {
            byId.put(d.id(), d);
        }
        byId.put(focus.id(), focus);
        Set<String> collapsed = new HashSet<>(focus.editor().graph().collapsed());
        boolean hideChildren = collapsed.contains(questId(focus.id()));

        placeQuest(focus, 0, 0);
        if (focus.giverNpc() != 0) {
            Node giver = place("giver:" + focus.id(), Kind.GIVER, focus.id(),
                    creatureTitle(focus.giverNpc(), "Giver"), String.valueOf(focus.giverNpc()),
                    hover(focus), -STEP, 0);
            edges.add(new Edge(giver.id(), questId(focus.id()), EdgeKind.START));
        }
        if (focus.turnInNpc() != 0 && !hideChildren) {
            Node turn = place("turnin:" + focus.id(), Kind.TURN_IN, focus.id(),
                    creatureTitle(focus.turnInNpc(), "Turn-in"), String.valueOf(focus.turnInNpc()),
                    hover(focus), STEP, 0);
            edges.add(new Edge(questId(focus.id()), turn.id(), EdgeKind.TURN_IN));
        }
        if (focus.prevQuestId() != 0) {
            QuestDocument prev = byId.get(focus.prevQuestId());
            String title = prev == null ? "Quest " + focus.prevQuestId() : prev.title();
            placeQuest(prev == null ? stub(focus.prevQuestId(), title) : prev, 0, -STEP);
            edges.add(new Edge(questId(focus.prevQuestId()), questId(focus.id()), EdgeKind.PREV));
        }
        if (!hideChildren) {
            for (QuestDocument other : this.chain) {
                if (other.id() != focus.id() && other.prevQuestId() == focus.id()) {
                    placeQuest(other, 0, STEP);
                    edges.add(new Edge(questId(focus.id()), questId(other.id()), EdgeKind.NEXT));
                }
            }
            int obj = 0;
            for (int i = 0; i < 4; i++) {
                if (focus.reqCreatureId(i) != 0) {
                    String id = "obj:" + focus.id() + ":" + obj;
                    double ang = -Math.PI / 4 + obj * 0.45;
                    Node n = place(id, Kind.OBJECTIVE, focus.id(),
                            killTitle(focus.reqCreatureId(i), focus.reqCreatureCount(i)),
                            String.valueOf(focus.reqCreatureId(i)),
                            hover(focus), Math.cos(ang) * 140, Math.sin(ang) * 140);
                    edges.add(new Edge(questId(focus.id()), n.id(), EdgeKind.OBJECTIVE));
                    obj++;
                }
                if (focus.reqItemId(i) != 0) {
                    String id = "obj:" + focus.id() + ":" + obj;
                    double ang = -Math.PI / 4 + obj * 0.45;
                    Node n = place(id, Kind.OBJECTIVE, focus.id(),
                            "Item " + focus.reqItemId(i) + " x" + focus.reqItemCount(i),
                            String.valueOf(focus.reqItemId(i)),
                            hover(focus), Math.cos(ang) * 140, Math.sin(ang) * 140);
                    edges.add(new Edge(questId(focus.id()), n.id(), EdgeKind.OBJECTIVE));
                    obj++;
                }
            }
        }
        applySavedLayout(focus);
        for (Node n : nodes) {
            n.setIssue(issueQuests.contains(n.questId()));
        }
    }

    public void moveNode(String id, double x, double y) {
        Node n = node(id);
        if (n == null || focus == null) {
            return;
        }
        n.setLayout(x, y);
        QuestDocument.GraphNodePos saved = focus.editor().graph().node(id);
        if (saved == null) {
            focus.editor().graph().nodes().add(new QuestDocument.GraphNodePos(id, x, y));
        } else {
            saved.set(x, y);
        }
    }

    public void linkPrev(int fromQuestId) {
        if (focus == null || fromQuestId == 0 || fromQuestId == focus.id()) {
            return;
        }
        focus.setPrevQuestId(fromQuestId);
        rebuild(focus, chain);
    }

    public void setTurnIn(int npc) {
        if (focus == null || npc == 0) {
            return;
        }
        focus.setTurnInNpc(npc);
        rebuild(focus, chain);
    }

    public void detachPrev() {
        if (focus == null) {
            return;
        }
        focus.setPrevQuestId(0);
        rebuild(focus, chain);
    }

    public void collapseSelected() {
        if (focus == null || selectedId == null || selectedId.isBlank()) {
            return;
        }
        List<String> collapsed = focus.editor().graph().collapsed();
        if (collapsed.contains(selectedId)) {
            collapsed.remove(selectedId);
        } else {
            collapsed.add(selectedId);
        }
        rebuild(focus, chain);
    }

    public void pan(double dx, double dy) {
        if (focus == null) {
            return;
        }
        QuestDocument.GraphView view = focus.editor().graph();
        view.setView(view.panX() + dx, view.panY() + dy, view.zoom());
    }

    public void zoomBy(double factor) {
        if (focus == null || factor <= 0) {
            return;
        }
        QuestDocument.GraphView view = focus.editor().graph();
        double z = view.zoom() * factor;
        if (z < 0.2) {
            z = 0.2;
        }
        if (z > 4) {
            z = 4;
        }
        view.setView(view.panX(), view.panY(), z);
    }

    public void fit() {
        if (focus == null) {
            return;
        }
        focus.editor().graph().setView(0, 0, 1);
    }

    public void select(String id) {
        selectedId = id == null ? "" : id;
    }

    public String selectedId() {
        return selectedId;
    }

    public void markIssues(Set<Integer> questIds) {
        issueQuests.clear();
        if (questIds != null) {
            issueQuests.addAll(questIds);
        }
        for (Node n : nodes) {
            n.setIssue(issueQuests.contains(n.questId()));
        }
    }

    public Node node(String id) {
        for (Node n : nodes) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }

    public Node hit(double x, double y) {
        Node found = null;
        for (Node n : nodes) {
            if (Math.abs(n.x() - x) <= 72 && Math.abs(n.y() - y) <= 28) {
                found = n;
            }
        }
        return found;
    }

    public List<Node> nodes() {
        return List.copyOf(nodes);
    }

    public List<Edge> edges() {
        return List.copyOf(edges);
    }

    public QuestDocument focus() {
        return focus;
    }

    public static boolean cycleTouches(int questId, List<QuestDocument> docs) {
        Map<Integer, Integer> prev = prevMap(docs);
        Set<Integer> seen = new HashSet<>();
        int cur = questId;
        while (cur != 0 && prev.containsKey(cur)) {
            if (!seen.add(cur)) {
                return true;
            }
            cur = prev.getOrDefault(cur, 0);
        }
        return false;
    }

    public static boolean hasCycle(List<QuestDocument> docs) {
        Map<Integer, Integer> prev = prevMap(docs);
        for (int start : prev.keySet()) {
            Set<Integer> seen = new HashSet<>();
            int cur = start;
            while (cur != 0 && prev.containsKey(cur)) {
                if (!seen.add(cur)) {
                    return true;
                }
                cur = prev.get(cur);
            }
        }
        return false;
    }

    private static Map<Integer, Integer> prevMap(List<QuestDocument> docs) {
        Map<Integer, Integer> prev = new HashMap<>();
        if (docs == null) {
            return prev;
        }
        for (QuestDocument d : docs) {
            prev.put(d.id(), d.prevQuestId());
        }
        return prev;
    }

    public static String questId(int id) {
        return "quest:" + id;
    }

    private void placeQuest(QuestDocument doc, double x, double y) {
        String id = questId(doc.id());
        if (node(id) != null) {
            return;
        }
        String title = doc.title().isBlank() ? "Quest" : doc.title();
        place(id, Kind.QUEST, doc.id(), title, String.valueOf(doc.id()), hover(doc), x, y);
    }

    private String creatureTitle(int entry, String fallback) {
        String name = creatureNames.get(entry);
        return name == null || name.isBlank() ? fallback : name;
    }

    private String killTitle(int entry, int count) {
        String name = creatureNames.get(entry);
        String who = name == null || name.isBlank() ? "creature " + entry : name;
        return "Kill " + who + " x" + count;
    }

    private Node place(String id, Kind kind, int questId, String title, String subtitle, String hover,
                       double x, double y) {
        Node existing = node(id);
        if (existing != null) {
            return existing;
        }
        Node n = new Node(id, kind, questId, title, subtitle, hover, x, y);
        nodes.add(n);
        return n;
    }

    private void applySavedLayout(QuestDocument doc) {
        for (QuestDocument.GraphNodePos pos : doc.editor().graph().nodes()) {
            Node n = node(pos.id());
            if (n != null) {
                n.setLayout(pos.x(), pos.y());
            }
        }
    }

    private static QuestDocument stub(int id, String title) {
        QuestDocument doc = QuestDocument.newOwned(id);
        doc.setTitle(title);
        return doc;
    }

    private static String hover(QuestDocument doc) {
        int objectives = 0;
        for (int i = 0; i < 4; i++) {
            if (doc.reqCreatureId(i) != 0 || doc.reqItemId(i) != 0) {
                objectives++;
            }
        }
        return doc.title() + " giver " + doc.giverNpc() + " objectives " + objectives;
    }
}
