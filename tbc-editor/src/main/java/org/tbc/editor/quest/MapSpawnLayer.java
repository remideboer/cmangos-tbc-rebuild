package org.tbc.editor.quest;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;

import java.util.ArrayList;
import java.util.List;

/** Creatures and gameobjects already spawned inside a quest-editor zone. */
public final class MapSpawnLayer {
    public enum Kind { CREATURE, OBJECT }

    public record Pin(Kind kind, int guid, int entry, String name, String typeName, float x, float y, float z, float o) {
        public Pin(Kind kind, int guid, int entry, String name, String typeName, float x, float y, float z) {
            this(kind, guid, entry, name, typeName, x, y, z, 0f);
        }

        public Pin moved(float x, float y, float z) {
            return new Pin(kind, guid, entry, name, typeName, x, y, z, o);
        }

        public Pin faced(float o) {
            return new Pin(kind, guid, entry, name, typeName, x, y, z, o);
        }

        public Pin named(String name) {
            return new Pin(kind, guid, entry, name == null ? "" : name, typeName, x, y, z, o);
        }
    }

    private static final int MAX_PINS = 8000;

    private MapSpawnLayer() {}

    public static List<Pin> inArea(ObjectMgr mgr, WorldMapArea area) {
        if (mgr == null || area == null) {
            return List.of();
        }
        WorldMapAreaMapper mapper = new WorldMapAreaMapper(area);
        List<Pin> pins = new ArrayList<>();
        add(pins, Kind.CREATURE, mgr.creatureSpawns(), mgr, mapper, area);
        add(pins, Kind.OBJECT, mgr.gameObjectSpawns(), mgr, mapper, area);
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
            pins.add(new Pin(kind, s.guid(), s.entry(), name(kind, mgr, s.entry()), typeName(kind, mgr, s.entry()),
                    s.x(), s.y(), s.z(), s.o()));
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

    private static String typeName(Kind kind, ObjectMgr mgr, int entry) {
        if (kind == Kind.CREATURE) {
            ObjectMgr.CreatureTemplate t = mgr.creatures.get(entry);
            return QuestService.creatureTypeName(t == null ? -1 : t.type());
        }
        ObjectMgr.GameObjectTemplate t = mgr.gameObjects.get(entry);
        return gameObjectTypeName(t == null ? -1 : t.type);
    }

    static String gameObjectTypeName(int type) {
        return switch (type) {
            case 0 -> "Door";
            case 1 -> "Button";
            case 2 -> "Questgiver";
            case 3 -> "Chest";
            case 4 -> "Binder";
            case 5 -> "Generic";
            case 6 -> "Trap";
            case 7 -> "Chair";
            case 8 -> "Spell focus";
            case 9 -> "Text";
            case 10 -> "Goober";
            case 11 -> "Transport";
            case 12 -> "Area damage";
            case 13 -> "Camera";
            case 14 -> "Map object";
            case 15 -> "Mo transport";
            case 16 -> "Duel arbiter";
            case 17 -> "Fishing node";
            case 18 -> "Ritual";
            case 19 -> "Mailbox";
            case 20 -> "Auction house";
            case 21 -> "Guard post";
            case 22 -> "Spellcaster";
            case 23 -> "Meeting stone";
            case 24 -> "Flag stand";
            case 25 -> "Fishing hole";
            case 26 -> "Flag drop";
            case 27 -> "Mini game";
            case 28 -> "Lottery kiosk";
            case 29 -> "Capture point";
            case 30 -> "Aura generator";
            case 31 -> "Dungeon difficulty";
            case 32 -> "Barber chair";
            case 33 -> "Destructible building";
            case 34 -> "Guild bank";
            case 35 -> "Trapdoor";
            default -> "Type " + type;
        };
    }
}
