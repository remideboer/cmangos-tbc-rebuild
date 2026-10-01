package org.tbc.content.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.content.dbc.WdbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharBaseInfoDeltaTest {
    @TempDir
    Path tmp;

    @Test
    void compileWhenClasslessCharBaseInfoYamlShouldAppendRaceClass6Rows() throws Exception {
        Path content = tmp.resolve("content");
        Path bindings = content.resolve("bindings/tbc243");
        Files.createDirectories(bindings);
        Files.writeString(bindings.resolve("CharBaseInfo.txt"), """
                uint RaceID
                uint ClassID
                """);
        // Minimal Spell binding so compiler can load directory; not used by this delta.
        Files.writeString(bindings.resolve("Spell.txt"), """
                uint ID
                """);
        Path classes = content.resolve("classes");
        Files.createDirectories(classes);
        Files.writeString(classes.resolve("classless-charbaseinfo.yaml"), """
                kind: charbaseinfo
                id: 6
                client:
                  CharBaseInfo:
                    races: [1, 2, 3, 4, 5, 6, 7, 8, 10, 11]
                """);

        Path base = tmp.resolve("dbc");
        Files.createDirectories(base);
        List<int[]> stock = new ArrayList<>();
        stock.add(new int[]{1, 1}); // human warrior
        stock.add(new int[]{1, 2}); // human paladin
        WdbcFile baseDbc = new WdbcFile(2, 2, stock, new byte[]{0});
        baseDbc.save(base.resolve("CharBaseInfo.dbc"));

        Path out = tmp.resolve("out");
        var result = new ContentCompiler().compile(content, base, out, "patch-tbc-custom.MPQ");
        assertEquals(1, result.deltas());

        WdbcFile merged = WdbcFile.load(out.resolve("dbc/CharBaseInfo.dbc"));
        assertTrue(merged.packedBytes());
        assertEquals(2, merged.recordSize());
        int[] races = {1, 2, 3, 4, 5, 6, 7, 8, 10, 11};
        for (int race : races) {
            assertTrue(merged.findRecordIndex(race, 6) >= 0,
                    "missing CharBaseInfo row (" + race + ",6)");
        }
        // stock rows preserved
        assertTrue(merged.findRecordIndex(1, 1) >= 0);
        assertEquals(2 + races.length, merged.records().size());
    }
}
