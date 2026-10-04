package org.tbc.editor.quest;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Which spawn entries belong to the quest open in the editor. */
public final class QuestRoles {
    public enum Role { GIVER, OBJECTIVE, TURN_IN }

    public record Marks(Map<Integer, EnumSet<Role>> creatures, Map<Integer, EnumSet<Role>> objects) {
        public Marks {
            creatures = copy(creatures);
            objects = copy(objects);
        }

        private static Map<Integer, EnumSet<Role>> copy(Map<Integer, EnumSet<Role>> source) {
            Map<Integer, EnumSet<Role>> out = new LinkedHashMap<>();
            if (source == null) {
                return Map.copyOf(out);
            }
            for (Map.Entry<Integer, EnumSet<Role>> e : source.entrySet()) {
                if (e.getValue() != null && !e.getValue().isEmpty()) {
                    out.put(e.getKey(), EnumSet.copyOf(e.getValue()));
                }
            }
            return Map.copyOf(out);
        }
    }

    private QuestRoles() {}

    public static Marks of(QuestDocument doc) {
        Map<Integer, EnumSet<Role>> creatures = new LinkedHashMap<>();
        Map<Integer, EnumSet<Role>> objects = new LinkedHashMap<>();
        if (doc == null) {
            return new Marks(creatures, objects);
        }
        if (doc.giverNpc() != 0) {
            add(creatures, doc.giverNpc(), Role.GIVER);
        }
        int turnIn = doc.turnInNpc() == 0 ? doc.giverNpc() : doc.turnInNpc();
        if (turnIn != 0) {
            add(creatures, turnIn, Role.TURN_IN);
        }
        for (int i = 0; i < 4; i++) {
            int id = doc.reqCreatureId(i);
            if (id > 0) {
                add(creatures, id, Role.OBJECTIVE);
            } else if (id < 0) {
                add(objects, -id, Role.OBJECTIVE);
            }
        }
        return new Marks(creatures, objects);
    }

    private static void add(Map<Integer, EnumSet<Role>> marks, int entry, Role role) {
        marks.computeIfAbsent(entry, k -> EnumSet.noneOf(Role.class)).add(role);
    }
}
