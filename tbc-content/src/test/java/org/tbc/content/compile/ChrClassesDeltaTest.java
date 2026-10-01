package org.tbc.content.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.content.dbc.DbcBinding;
import org.tbc.content.dbc.WdbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChrClassesDeltaTest {
    @TempDir
    Path tmp;

    @Test
    void compileWhenClasslessYamlShouldInsertId6WithWarriorFilename() throws Exception {
        Path content = tmp.resolve("content");
        Path bindings = content.resolve("bindings/tbc243");
        Files.createDirectories(bindings);
        Files.writeString(bindings.resolve("ChrClasses.txt"), """
                uint ClassID
                uint Unused1
                int powerType
                uint Unused3
                uint name_lang_enUS string
                uint filename string
                """);
        Path classes = content.resolve("classes");
        Files.createDirectories(classes);
        Files.writeString(classes.resolve("classless.yaml"), """
                kind: chrclasses
                id: 6
                client:
                  ChrClasses:
                    powerType: 0
                    name_lang_enUS: Classless
                    filename: WARRIOR
                server:
                  templateId: 1
                """);

        Path base = tmp.resolve("dbc");
        Files.createDirectories(base);
        List<int[]> rows = new ArrayList<>();
        rows.add(new int[]{1, 0, 1, 0, 0, 0}); // warrior rage
        WdbcFile baseDbc = new WdbcFile(6, 24, rows, new byte[]{0});
        DbcBinding binding = DbcBinding.load(bindings.resolve("ChrClasses.txt"));
        baseDbc.setField(binding, 1, "name_lang_enUS", "Warrior");
        baseDbc.setField(binding, 1, "filename", "WARRIOR");
        baseDbc.save(base.resolve("ChrClasses.dbc"));

        Path out = tmp.resolve("out");
        var result = new ContentCompiler().compile(content, base, out, "patch-tbc-custom.MPQ");
        assertEquals(1, result.deltas());
        WdbcFile merged = WdbcFile.load(out.resolve("dbc/ChrClasses.dbc"));
        assertTrue(merged.findRecordIndexById(6) >= 0);
        int row = merged.findRecordIndexById(6);
        assertEquals(0, merged.records().get(row)[binding.field("powerType").orElseThrow().index()]);
        int nameOff = merged.records().get(row)[binding.field("name_lang_enUS").orElseThrow().index()];
        assertEquals("Classless", merged.str(nameOff));
        int fileOff = merged.records().get(row)[binding.field("filename").orElseThrow().index()];
        assertEquals("WARRIOR", merged.str(fileOff));
    }
}
