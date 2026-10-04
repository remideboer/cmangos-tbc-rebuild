package org.tbc.editor.quest;

import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Headless 2D quest map: place/select/move/nudge/orient/undo. */
public final class QuestMapModel {
    public enum Tool {
        SELECT,
        PLACE_GIVER,
        PLACE_TURN_IN,
        PLACE_KILL,
        PLACE_OBJECT,
        PLACE_AREA,
        PLACE_ROUTE,
        PLACE_NOTE
    }

    public enum MarkerKind {
        GIVER,
        TURN_IN,
        KILL_TARGET,
        QUEST_OBJECT,
        AREA,
        ROUTE,
        NOTE
    }

    public static final class Marker {
        private final String id;
        private MarkerKind kind;
        private double x;
        private double y;
        private float orientation;

        Marker(String id, MarkerKind kind, double x, double y) {
            this.id = id;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.orientation = 0f;
        }

        public String id() {
            return id;
        }

        public MarkerKind kind() {
            return kind;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public float orientation() {
            return orientation;
        }

        Marker copy() {
            Marker m = new Marker(id, kind, x, y);
            m.orientation = orientation;
            return m;
        }
    }

    private final List<Marker> markers = new ArrayList<>();
    private final Set<MarkerKind> hidden = EnumSet.noneOf(MarkerKind.class);
    private final Deque<List<Marker>> undo = new ArrayDeque<>();
    private final Deque<List<Marker>> redo = new ArrayDeque<>();
    private Tool tool = Tool.SELECT;
    private String selectedId;
    private OverlayCalibration calibration;
    private WorldMapArea region;
    private WorldMapAreaMapper mapper;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private double panX;
    private double panY;
    private double zoom = 1;
    private boolean dragging;

    public void setTool(Tool tool) {
        this.tool = tool == null ? Tool.SELECT : tool;
    }

    public Tool tool() {
        return tool;
    }

    public void setCalibration(OverlayCalibration calibration) {
        this.calibration = calibration;
    }

    public OverlayCalibration calibration() {
        return calibration;
    }

    public void setRegion(WorldMapArea region, int imageWidth, int imageHeight) {
        this.region = region;
        this.mapper = region == null ? null : new WorldMapAreaMapper(region);
        this.imageWidth = Math.max(1, imageWidth);
        this.imageHeight = Math.max(1, imageHeight);
        fitToRegion();
    }

    public WorldMapArea region() {
        return region;
    }

    public int imageWidth() {
        return imageWidth;
    }

    public int imageHeight() {
        return imageHeight;
    }

    public Marker clickPixel(double pixelX, double pixelY) {
        if (mapper == null) {
            return null;
        }
        float[] world = mapper.toWorld((float) pixelX, (float) pixelY, imageWidth, imageHeight);
        return clickWorld(world[0], world[1]);
    }

    public float[] markerPixel(Marker m) {
        if (mapper == null || m == null) {
            return new float[]{0, 0};
        }
        return mapper.toPixel((float) m.x, (float) m.y, imageWidth, imageHeight);
    }

    public float[] viewToWorld(double pixelX, double pixelY) {
        if (mapper == null) {
            return new float[]{(float) pixelX, (float) pixelY};
        }
        return mapper.toWorld((float) pixelX, (float) pixelY, imageWidth, imageHeight);
    }

    public List<Marker> markers() {
        return List.copyOf(markers);
    }

    public List<Marker> visibleMarkers() {
        List<Marker> out = new ArrayList<>();
        for (Marker m : markers) {
            if (!hidden.contains(m.kind)) {
                out.add(m);
            }
        }
        return out;
    }

    public void setKindVisible(MarkerKind kind, boolean visible) {
        if (visible) {
            hidden.remove(kind);
        } else {
            hidden.add(kind);
        }
    }

    public Marker selected() {
        return find(selectedId);
    }

    public Marker clickWorld(double x, double y) {
        if (tool == Tool.SELECT) {
            Marker nearest = nearest(x, y, 5);
            selectedId = nearest == null ? null : nearest.id;
            return nearest;
        }
        MarkerKind kind = kindFor(tool);
        if (kind == null) {
            return null;
        }
        pushUndo();
        Marker m = new Marker(UUID.randomUUID().toString(), kind, x, y);
        markers.add(m);
        selectedId = m.id;
        redo.clear();
        return m;
    }

    public void beginDrag(String id) {
        Marker m = find(id);
        if (m == null) {
            return;
        }
        pushUndo();
        selectedId = id;
        dragging = true;
        redo.clear();
    }

    public void dragTo(double x, double y) {
        Marker m = selected();
        if (!dragging || m == null) {
            return;
        }
        m.x = x;
        m.y = y;
    }

    public void endDrag() {
        dragging = false;
    }

    public void nudge(double dx, double dy) {
        Marker m = selected();
        if (m == null) {
            return;
        }
        pushUndo();
        m.x += dx;
        m.y += dy;
        redo.clear();
    }

    public void setNumericXy(double x, double y) {
        Marker m = selected();
        if (m == null) {
            return;
        }
        pushUndo();
        m.x = x;
        m.y = y;
        redo.clear();
    }

    public void setOrientation(float radians) {
        Marker m = selected();
        if (m == null) {
            return;
        }
        pushUndo();
        m.orientation = radians;
        redo.clear();
    }

    public void setOrientationFromPixelDelta(double dpx, double dpy) {
        if (calibration == null) {
            return;
        }
        setOrientation((float) calibration.worldOrientationFromPixelDelta(dpx, dpy));
    }

    public void undo() {
        if (undo.isEmpty()) {
            return;
        }
        redo.push(snapshot());
        restore(undo.pop());
    }

    public void redo() {
        if (redo.isEmpty()) {
            return;
        }
        undo.push(snapshot());
        restore(redo.pop());
    }

    public void deleteSelected() {
        Marker m = selected();
        if (m == null) {
            return;
        }
        pushUndo();
        markers.removeIf(x -> x.id.equals(m.id));
        selectedId = null;
        redo.clear();
    }

    public void pan(double dx, double dy) {
        panX += dx;
        panY += dy;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom <= 0 ? 1 : zoom;
    }

    public double zoom() {
        return zoom;
    }

    public double panX() {
        return panX;
    }

    public double panY() {
        return panY;
    }

    public void fitToMarkers() {
        if (markers.isEmpty()) {
            zoom = 1;
            return;
        }
        zoom = 1;
        panX = 0;
        panY = 0;
    }

    public void fitToRegion() {
        zoom = 1;
        panX = 0;
        panY = 0;
    }

    private MarkerKind kindFor(Tool t) {
        return switch (t) {
            case PLACE_GIVER -> MarkerKind.GIVER;
            case PLACE_TURN_IN -> MarkerKind.TURN_IN;
            case PLACE_KILL -> MarkerKind.KILL_TARGET;
            case PLACE_OBJECT -> MarkerKind.QUEST_OBJECT;
            case PLACE_AREA -> MarkerKind.AREA;
            case PLACE_ROUTE -> MarkerKind.ROUTE;
            case PLACE_NOTE -> MarkerKind.NOTE;
            default -> null;
        };
    }

    private Marker nearest(double x, double y, double max) {
        Marker best = null;
        double bestD = max;
        for (Marker m : visibleMarkers()) {
            double d = Math.hypot(m.x - x, m.y - y);
            if (d <= bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    private Marker find(String id) {
        if (id == null) {
            return null;
        }
        for (Marker m : markers) {
            if (id.equals(m.id)) {
                return m;
            }
        }
        return null;
    }

    private void pushUndo() {
        undo.push(snapshot());
    }

    private List<Marker> snapshot() {
        List<Marker> copy = new ArrayList<>();
        for (Marker m : markers) {
            copy.add(m.copy());
        }
        return copy;
    }

    private void restore(List<Marker> snap) {
        markers.clear();
        for (Marker m : snap) {
            markers.add(m.copy());
        }
        if (find(selectedId) == null) {
            selectedId = markers.isEmpty() ? null : markers.get(markers.size() - 1).id;
        }
    }
}
