package org.tbc.editor.quest;

import org.tbc.content.dbc.WdbcFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FactionTemplate id to the English name in Faction.dbc.
 * Template field 1 is the Faction.dbc id. Faction field 22 is the enUS name.
 */
public final class NpcFactions {
    public record Choice(int templateId, String name) {
        @Override
        public String toString() {
            return label(templateId, name);
        }
    }

    private final Map<Integer, String> byTemplate;

    private NpcFactions(Map<Integer, String> byTemplate) {
        this.byTemplate = byTemplate;
    }

    public static NpcFactions empty() {
        return new NpcFactions(Map.of());
    }

    public static String label(int templateId, String name) {
        if (name == null || name.isBlank()) {
            return Integer.toString(templateId);
        }
        return templateId + " — " + name;
    }

    public String name(int templateId) {
        return byTemplate.getOrDefault(templateId, "");
    }

    public List<Choice> choices() {
        List<Choice> out = new ArrayList<>();
        for (Map.Entry<Integer, String> e : byTemplate.entrySet()) {
            out.add(new Choice(e.getKey(), e.getValue()));
        }
        out.sort(Comparator.comparingInt(Choice::templateId));
        return out;
    }

    public static NpcFactions load(Path dataDir) {
        byte[] templates = WorldMapBlp.readNamed(dataDir, "DBFilesClient\\FactionTemplate.dbc");
        byte[] factions = WorldMapBlp.readNamed(dataDir, "DBFilesClient\\Faction.dbc");
        if (templates == null || factions == null) {
            return empty();
        }
        try {
            return join(WdbcFile.read(templates), WdbcFile.read(factions));
        } catch (Exception e) {
            return empty();
        }
    }

    public static NpcFactions join(WdbcFile templates, WdbcFile factions) {
        Map<Integer, String> names = new LinkedHashMap<>();
        if (factions != null) {
            for (int[] row : factions.records()) {
                if (row.length > 22 && row[0] != 0) {
                    names.put(row[0], factions.str(row[22]));
                }
            }
        }
        Map<Integer, String> byTemplate = new LinkedHashMap<>();
        if (templates != null) {
            for (int[] row : templates.records()) {
                if (row.length < 2 || row[0] == 0) {
                    continue;
                }
                byTemplate.put(row[0], names.getOrDefault(row[1], ""));
            }
        }
        return new NpcFactions(byTemplate);
    }
}
