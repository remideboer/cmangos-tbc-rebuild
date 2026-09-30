package org.tbc.content.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.content.dbc.DbcBinding;
import org.tbc.content.dbc.WdbcFile;
import org.tbc.content.domain.SpellDomain;
import org.tbc.content.util.Backup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentCompilerTest {
    @TempDir
    Path tmp;

    @Test
    void yamlWhenSpellDeltaShouldParseKindAndClientFields() throws Exception {
        Path y = tmp.resolve("s.yaml");
        Files.writeString(y, """
                kind: spell
                id: 20154
                client:
                  Spell:
                    AuraDescription_lang_enUS: "Melee ${1.2*($m1*1.2*1.03*$MWS/100)+0.03*($MW+$mw)/2+1} Holy"
                """);
        var deltas = YamlDeltaLoader.loadFile(y);
        assertEquals(1, deltas.size());
        assertEquals("spell", deltas.get(0).kind());
        assertEquals(20154, deltas.get(0).id());
        assertTrue(deltas.get(0).client().get("Spell").containsKey("AuraDescription_lang_enUS"));
        String tip = String.valueOf(deltas.get(0).client().get("Spell").get("AuraDescription_lang_enUS"));
        assertFalse(tip.contains("$HND"));
        assertTrue(tip.contains("1.2*($m1"));
    }

    @Test
    void spellDomainWhenServerFieldsShouldEmitUpdateSql() {
        String sql = SpellDomain.buildUpdate(20154, Map.of("EffectBasePoints1", 107), "test.yaml");
        assertTrue(sql.contains("UPDATE `spell_template`"));
        assertTrue(sql.contains("`EffectBasePoints1`=107"));
        assertTrue(sql.contains("WHERE `Id`=20154"));
    }

    @Test
    void mergeWhenYamlChangesStringShouldRewriteDbc() throws Exception {
        Path content = tmp.resolve("content");
        Path bindings = content.resolve("bindings/tbc243");
        Files.createDirectories(bindings);
        Files.writeString(bindings.resolve("Spell.txt"), "uint ID\nuint SpellToolTip0 string\n");
        Path spells = content.resolve("spells");
        Files.createDirectories(spells);
        Files.writeString(spells.resolve("x.yaml"), """
                kind: spell
                id: 9
                client:
                  Spell:
                    AuraDescription_lang_enUS: "one-formula holy"
                """);

        Path base = tmp.resolve("dbc");
        Files.createDirectories(base);
        WdbcFile baseDbc = WdbcFile.fixtureTwoFields(9, "old-cond-HND");
        // fixture uses field 1 as string; binding SpellToolTip0 is index 1 — good
        baseDbc.save(base.resolve("Spell.dbc"));

        Path out = tmp.resolve("out");
        var result = new ContentCompiler().compile(content, base, out, "patch-tbc-custom.MPQ");
        assertEquals(1, result.deltas());
        WdbcFile merged = WdbcFile.load(out.resolve("dbc/Spell.dbc"));
        DbcBinding b = DbcBinding.load(bindings.resolve("Spell.txt"));
        int tipOff = merged.records().get(0)[b.field("SpellToolTip0").orElseThrow().index()];
        String tip = merged.str(tipOff);
        assertEquals("one-formula holy", tip);
        assertFalse(tip.contains("HND"));
        assertTrue(Files.isRegularFile(result.mpqFile()));
        assertTrue(Files.size(result.mpqFile()) > 32);
    }

    @Test
    void backupWhenFileExistsShouldCreateBakSibling() throws Exception {
        Path f = tmp.resolve("x.dbc");
        Files.writeString(f, "hello");
        Path bak = Backup.bakIfExists(f);
        assertNotNull(bak);
        assertTrue(bak.getFileName().toString().startsWith("x.dbc.bak."));
        assertEquals("hello", Files.readString(bak));
    }
}
