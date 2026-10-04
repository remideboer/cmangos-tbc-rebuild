package org.tbc.editor.quest;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;

import java.util.ArrayList;
import java.util.List;

/** Creatures and gameobjects already spawned inside a quest-editor zone. */
public final class MapSpawnLayer {
    public enum Kind { CREATURE, OBJECT }

    public record Pin(Kind kind, int entry, String name, float x, float y) {}

    private static final int MAX_PINS = 8000;

    private MapSpawnLayer() {}

    public static List<Pin> inArea(ObjectMgr mgr, WorldMapArea area) {
        if (mgr == null || area == null) {
            return List.of();
        }
        WorldMapAreaMapper mapper = new WorldMapAreaMapper(area);
        List<Pin> pins = new ArrayList<>();
        add(pins, Kind.CREATURE, mgr.spawns, mgr, mapper, area);
        add(pins, Kind.OBJECT, mgr.goSpawns, mgr, mapper, area);
        return List.copyOf(pins);
    }

    public static Pin nearest(List<Pin> pins, float worldX, float worldY, float yards) {
        if (pins == null || yards <= 0) {
            return null;
        }
        Pin best = null;
        double bestD = yards;
        for (Pin pin : pins) {
            double d = Math.hypot(pin.x() - worldX, pin.y() - worldY);
            if (d <= bestD) {
                bestD = d;
                best = pin;
            }
        }
        return best;
    }

    private static void add(List<Pin> pins, Kind kind, List<ObjectMgr.Spawn> spawns, ObjectMgr mgr,
                            WorldMapAreaMapper mapper, WorldMapArea area) {
        for (ObjectMgr.Spawn s : spawns) {
            if (pins.size() >= MAX_PINS) {
                return;
            }
            if (s.map() != area.mapId() || !mapper.contains(s.x(), s.y())) {
                continue;
            }
            pins.add(new Pin(kind, s.entry(), name(kind, mgr, s.entry()), s.x(), s.y()));
        }
    }

    private static String name(Kind kind, ObjectMgr mgr, int entry) {
        if (kind == Kind.CREATURE) {
            ObjectMgr.CreatureTemplate t = mgr.creatures.get(entry);
            if (t != null && t.name() != null && !t.name().isBlank()) {
                return t.name();
            }
        } else {
            ObjectMgr.GameObjectTemplate t = mgr.gameObjects.get(entry);
            if (t != null && t.name != null && !t.name.isBlank()) {
                return t.name;
            }
        }
        return (kind == Kind.CREATURE ? "Creature " : "Object ") + entry;
    }
}
