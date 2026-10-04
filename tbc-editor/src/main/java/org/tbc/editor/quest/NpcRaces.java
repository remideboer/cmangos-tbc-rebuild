package org.tbc.editor.quest;

import org.tbc.content.dbc.WdbcFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Player races from ChrRaces.dbc. Field 4 is the male display id, field 5 the female, field 14 the name. */
public final class NpcRaces {
    public record Race(String name, int displayId) {
        @Override
        public String toString() {
            return name;
        }
    }

    private NpcRaces() {}

    public static List<Race> load(Path dataDir) {
        byte[] bytes = WorldMapBlp.readNamed(dataDir, "DBFilesClient\\ChrRaces.dbc");
        if (bytes == null) {
            return List.of();
        }
        try {
            return from(WdbcFile.read(bytes));
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<Race> from(WdbcFile file) {
        List<Race> races = new ArrayList<>();
        if (file == null) {
            return races;
        }
        for (int[] row : file.records()) {
            if (row.length < 15 || row[0] == 0) {
                continue;
            }
            String name = file.str(row[14]);
            if (name.isBlank()) {
                continue;
            }
            int male = row[4];
            int female = row[5];
            if (male > 0) {
                races.add(new Race(name, male));
            }
            if (female > 0 && female != male) {
                races.add(new Race(name + " (female)", female));
            }
        }
        races.sort(Comparator.comparing(Race::name));
        return races;
    }
}
