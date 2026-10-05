package org.tbc.editor.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unsaved NPC edits. Position is one creature guid. Name, display, type, faction, and gear are the shared template.
 * Clear copies the snapshot back. Save reads {@link #moved()} and {@link #changedLooks()}.
 */
public final class NpcEditSession {
    public record Pose(int guid, int entry, float x, float y, float z, float o) {}

    public record Look(int entry, String name, int displayId, int equipmentId, int creatureType, int faction) {}

    public record Change(Kind kind, int guid, int entry, String text) {
        public enum Kind { MOVED, TEMPLATE }

        @Override
        public String toString() {
            return text;
        }
    }

    private final Map<Integer, Pose> originalPose = new LinkedHashMap<>();
    private final Map<Integer, Pose> pose = new LinkedHashMap<>();
    private final Map<Integer, Look> originalLook = new LinkedHashMap<>();
    private final Map<Integer, Look> look = new LinkedHashMap<>();

    public void remember(MapSpawnLayer.Pin pin, int displayId, int equipmentId, int creatureType, int faction) {
        if (pin == null || pin.kind() != MapSpawnLayer.Kind.CREATURE || pin.guid() == 0) {
            return;
        }
        String name = pin.name() == null ? "" : pin.name();
        originalPose.putIfAbsent(pin.guid(), new Pose(pin.guid(), pin.entry(), pin.x(), pin.y(), pin.z(), pin.o()));
        pose.putIfAbsent(pin.guid(), originalPose.get(pin.guid()));
        originalLook.putIfAbsent(pin.entry(), new Look(pin.entry(), name, displayId, equipmentId, creatureType, faction));
        look.putIfAbsent(pin.entry(), originalLook.get(pin.entry()));
    }

    public void move(int guid, float x, float y, float z) {
        Pose cur = pose.get(guid);
        if (cur == null) {
            return;
        }
        pose.put(guid, new Pose(guid, cur.entry(), x, y, z, cur.o()));
    }

    public void face(int guid, float o) {
        Pose cur = pose.get(guid);
        if (cur == null) {
            return;
        }
        pose.put(guid, new Pose(guid, cur.entry(), cur.x(), cur.y(), cur.z(), o));
    }

    public void rename(int entry, String name) {
        Look cur = look.get(entry);
        if (cur == null) {
            return;
        }
        look.put(entry, new Look(entry, name == null ? "" : name, cur.displayId(), cur.equipmentId(),
                cur.creatureType(), cur.faction()));
    }

    public void setDisplay(int entry, int displayId) {
        Look cur = look.get(entry);
        if (cur == null) {
            return;
        }
        look.put(entry, new Look(entry, cur.name(), displayId, cur.equipmentId(), cur.creatureType(), cur.faction()));
    }

    public void setCreatureType(int entry, int creatureType) {
        Look cur = look.get(entry);
        if (cur == null) {
            return;
        }
        look.put(entry, new Look(entry, cur.name(), cur.displayId(), cur.equipmentId(), creatureType, cur.faction()));
    }

    public void setFaction(int entry, int faction) {
        Look cur = look.get(entry);
        if (cur == null) {
            return;
        }
        look.put(entry, new Look(entry, cur.name(), cur.displayId(), cur.equipmentId(), cur.creatureType(), faction));
    }

    public void setEquipment(int entry, int equipmentId) {
        Look cur = look.get(entry);
        if (cur == null) {
            return;
        }
        look.put(entry, new Look(entry, cur.name(), cur.displayId(), equipmentId, cur.creatureType(), cur.faction()));
    }

    public Look look(int entry) {
        return look.get(entry);
    }

    public boolean dirty() {
        return !moved().isEmpty() || !changedLooks().isEmpty();
    }

    public List<Pose> moved() {
        List<Pose> out = new ArrayList<>();
        for (Map.Entry<Integer, Pose> e : pose.entrySet()) {
            Pose was = originalPose.get(e.getKey());
            Pose now = e.getValue();
            if (was != null && shifted(was, now)) {
                out.add(now);
            }
        }
        return out;
    }

    public List<Look> changedLooks() {
        List<Look> out = new ArrayList<>();
        for (Map.Entry<Integer, Look> e : look.entrySet()) {
            Look was = originalLook.get(e.getKey());
            Look now = e.getValue();
            if (was != null && !sameLook(was, now)) {
                out.add(now);
            }
        }
        return out;
    }

    public List<Change> changes() {
        return changes(guid -> true);
    }

    public List<Change> changes(java.util.function.IntPredicate dbCreature) {
        java.util.function.IntPredicate inDb = dbCreature == null ? guid -> true : dbCreature;
        List<Change> out = new ArrayList<>();
        for (Pose now : moved()) {
            Look named = look.get(now.entry());
            String name = named == null ? "" : named.name();
            String text = "guid " + now.guid() + " (" + name + ") moved";
            if (Math.abs(now.o() - originalPose.get(now.guid()).o()) > POSITION_EPS) {
                text += " facing " + NpcFacing.tick(now.o()) + "/16";
            }
            if (!inDb.test(now.guid())) {
                text += " — will insert";
            }
            out.add(new Change(Change.Kind.MOVED, now.guid(), now.entry(), text));
        }
        for (Look now : changedLooks()) {
            Look was = originalLook.get(now.entry());
            List<String> fields = new ArrayList<>();
            if (was != null && !was.name().equals(now.name())) {
                fields.add("name");
            }
            if (was != null && was.displayId() != now.displayId()) {
                fields.add("race");
            }
            if (was != null && was.creatureType() != now.creatureType()) {
                fields.add("type");
            }
            if (was != null && was.equipmentId() != now.equipmentId()) {
                fields.add("gear");
            }
            if (was != null && was.faction() != now.faction()) {
                fields.add("faction");
            }
            out.add(new Change(Change.Kind.TEMPLATE, 0, now.entry(),
                    "entry " + now.entry() + " (" + now.name() + "): " + String.join(", ", fields)));
        }
        return out;
    }

    /** Drop unsaved edits. Does not touch the database. */
    public void revert() {
        pose.clear();
        pose.putAll(originalPose);
        look.clear();
        look.putAll(originalLook);
    }

    /** After a successful save, the current values are the new snapshot. */
    public void accept() {
        originalPose.clear();
        originalPose.putAll(pose);
        originalLook.clear();
        originalLook.putAll(look);
    }

    /** Paint unsaved positions and names onto pins rebuilt from the loaded spawns. */
    public List<MapSpawnLayer.Pin> overlay(List<MapSpawnLayer.Pin> pins) {
        List<MapSpawnLayer.Pin> out = new ArrayList<>();
        if (pins == null) {
            return out;
        }
        for (MapSpawnLayer.Pin pin : pins) {
            MapSpawnLayer.Pin next = pin;
            Pose at = pose.get(pin.guid());
            if (pin.kind() == MapSpawnLayer.Kind.CREATURE && at != null) {
                next = next.moved(at.x(), at.y(), at.z()).faced(at.o());
            }
            Look named = look.get(pin.entry());
            if (pin.kind() == MapSpawnLayer.Kind.CREATURE && named != null) {
                next = next.named(named.name());
            }
            out.add(next);
        }
        return out;
    }

    static final float POSITION_EPS = 0.05f;

    private static boolean shifted(Pose was, Pose now) {
        return Math.abs(was.x() - now.x()) > POSITION_EPS
                || Math.abs(was.y() - now.y()) > POSITION_EPS
                || Math.abs(was.z() - now.z()) > POSITION_EPS
                || Math.abs(was.o() - now.o()) > POSITION_EPS;
    }

    private static boolean sameLook(Look was, Look now) {
        return was.displayId() == now.displayId()
                && was.equipmentId() == now.equipmentId()
                && was.creatureType() == now.creatureType()
                && was.faction() == now.faction()
                && was.name().equals(now.name());
    }
}
