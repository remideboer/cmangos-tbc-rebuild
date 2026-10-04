package org.tbc.world.content;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runtime quest overlay YAML (published). Ignores authoring-only {@code editor:} keys. */
public final class QuestOverlayYaml {
    private QuestOverlayYaml() {}

    public static Overlay read(Path file) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(file)) {
            Object doc = yaml.load(in);
            if (!(doc instanceof Map<?, ?> map)) {
                throw new IOException(file + ": expected mapping");
            }
            return parse(map);
        }
    }

    @SuppressWarnings("unchecked")
    public static Overlay parse(Map<?, ?> map) {
        Overlay o = new Overlay();
        o.id = intVal(map.get("id"));
        o.title = str(map.get("title"));
        o.details = str(map.get("details"));
        o.objectives = str(map.get("objectives"));
        o.minLevel = intVal(map.get("minLevel"));
        o.questLevel = intVal(map.get("questLevel"));
        o.type = intVal(map.get("type"));
        o.rewMoney = intVal(map.get("rewMoney"));
        o.prevQuestId = intVal(map.get("prevQuestId"));
        o.requiredRaces = intVal(map.get("requiredRaces"));
        o.zoneOrSort = intVal(map.get("zoneOrSort"));
        o.giverNpc = intVal(map.get("giverNpc"));
        o.turnInNpc = intVal(map.get("turnInNpc"));
        o.rewSpell = intVal(map.get("rewSpell"));
        o.rewItemId = intVal(nested(map.get("rewItem"), "id"));
        o.rewItemCount = intVal(nested(map.get("rewItem"), "count"));
        fillReqs(map.get("reqCreature"), o.reqCreatureId, o.reqCreatureCount);
        fillReqs(map.get("reqItem"), o.reqItemId, o.reqItemCount);
        Object creatures = map.get("creatures");
        if (creatures instanceof List<?> list) {
            for (Object row : list) {
                if (row instanceof Map<?, ?> m) {
                    o.creatures.add(new CreatureOverlay(
                            intVal(m.get("entry")),
                            str(m.get("name")),
                            intVal(m.get("display")),
                            intVal(m.get("faction")),
                            intVal(m.get("npcFlags")),
                            intVal(m.get("movementType"))));
                }
            }
        }
        Object spawns = map.get("spawns");
        if (spawns instanceof List<?> list) {
            for (Object row : list) {
                if (row instanceof Map<?, ?> m) {
                    o.spawns.add(new SpawnOverlay(
                            intVal(m.get("guid")),
                            intVal(m.get("entry")),
                            intVal(m.get("map")),
                            floatVal(first(m, "worldX", "x")),
                            floatVal(first(m, "worldY", "y")),
                            floatVal(first(m, "worldZ", "z")),
                            floatVal(first(m, "orientation", "o")),
                            intVal(m.get("movementType"))));
                }
            }
        }
        return o;
    }

    public static Map<String, Object> toRuntimeMap(Overlay o) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", "quest");
        map.put("id", o.id);
        map.put("title", o.title);
        map.put("details", o.details);
        map.put("objectives", o.objectives);
        map.put("minLevel", o.minLevel);
        map.put("questLevel", o.questLevel);
        map.put("type", o.type);
        map.put("rewMoney", o.rewMoney);
        map.put("prevQuestId", o.prevQuestId);
        map.put("requiredRaces", o.requiredRaces);
        map.put("zoneOrSort", o.zoneOrSort);
        map.put("giverNpc", o.giverNpc);
        map.put("turnInNpc", o.turnInNpc);
        if (o.rewSpell != 0) {
            map.put("rewSpell", o.rewSpell);
        }
        if (o.rewItemId != 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", o.rewItemId);
            item.put("count", o.rewItemCount);
            map.put("rewItem", item);
        }
        List<Map<String, Object>> creatures = new ArrayList<>();
        for (CreatureOverlay c : o.creatures) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("entry", c.entry());
            row.put("name", c.name());
            row.put("display", c.display());
            row.put("faction", c.faction());
            row.put("npcFlags", c.npcFlags());
            row.put("movementType", c.movementType());
            creatures.add(row);
        }
        if (!creatures.isEmpty()) {
            map.put("creatures", creatures);
        }
        List<Map<String, Object>> spawns = new ArrayList<>();
        for (SpawnOverlay s : o.spawns) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("guid", s.guid());
            row.put("entry", s.entry());
            row.put("map", s.map());
            row.put("worldX", s.x());
            row.put("worldY", s.y());
            row.put("worldZ", s.z());
            row.put("orientation", s.o());
            row.put("movementType", s.movementType());
            spawns.add(row);
        }
        if (!spawns.isEmpty()) {
            map.put("spawns", spawns);
        }
        List<Map<String, Object>> reqC = reqList(o.reqCreatureId, o.reqCreatureCount);
        if (!reqC.isEmpty()) {
            map.put("reqCreature", reqC);
        }
        List<Map<String, Object>> reqI = reqList(o.reqItemId, o.reqItemCount);
        if (!reqI.isEmpty()) {
            map.put("reqItem", reqI);
        }
        return map;
    }

    private static List<Map<String, Object>> reqList(int[] ids, int[] counts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", ids[i]);
                row.put("count", counts[i]);
                out.add(row);
            }
        }
        return out;
    }

    private static void fillReqs(Object raw, int[] ids, int[] counts) {
        if (!(raw instanceof List<?> list)) {
            return;
        }
        int i = 0;
        for (Object row : list) {
            if (i >= ids.length) {
                break;
            }
            if (row instanceof Map<?, ?> m) {
                ids[i] = intVal(m.get("id"));
                counts[i] = intVal(m.get("count"));
                i++;
            }
        }
    }

    private static Object first(Map<?, ?> m, String... keys) {
        for (String key : keys) {
            if (m.containsKey(key)) {
                return m.get(key);
            }
        }
        return null;
    }

    private static Object nested(Object raw, String key) {
        if (raw instanceof Map<?, ?> m) {
            return m.get(key);
        }
        return null;
    }

    private static int intVal(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o == null || o instanceof Boolean) {
            return 0;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return (int) Double.parseDouble(s);
        }
    }

    private static float floatVal(Object o) {
        if (o instanceof Number n) {
            return n.floatValue();
        }
        if (o == null || o instanceof Boolean) {
            return 0f;
        }
        try {
            return Float.parseFloat(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public static final class Overlay {
        public int id;
        public String title = "";
        public String details = "";
        public String objectives = "";
        public int minLevel;
        public int questLevel;
        public int type;
        public int rewMoney;
        public int prevQuestId;
        public int requiredRaces;
        public int zoneOrSort;
        public int giverNpc;
        public int turnInNpc;
        public int rewSpell;
        public int rewItemId;
        public int rewItemCount;
        public final int[] reqCreatureId = new int[4];
        public final int[] reqCreatureCount = new int[4];
        public final int[] reqItemId = new int[4];
        public final int[] reqItemCount = new int[4];
        public final List<CreatureOverlay> creatures = new ArrayList<>();
        public final List<SpawnOverlay> spawns = new ArrayList<>();
    }

    public record CreatureOverlay(int entry, String name, int display, int faction, int npcFlags, int movementType) {}

    public record SpawnOverlay(int guid, int entry, int map, float x, float y, float z, float o, int movementType) {}
}
