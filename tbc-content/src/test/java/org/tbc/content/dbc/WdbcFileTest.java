package org.tbc.content.dbc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WdbcFileTest {
    @TempDir
    Path tmp;

    @Test
    void saveLoadWhenTwoFieldFixtureShouldRoundTripString() throws Exception {
        WdbcFile f = WdbcFile.fixtureTwoFields(42, "hello");
        Path p = tmp.resolve("Tiny.dbc");
        f.save(p);
        WdbcFile loaded = WdbcFile.load(p);
        assertEquals(1, loaded.records().size());
        assertEquals(42, loaded.records().get(0)[0]);
        assertEquals("hello", loaded.str(loaded.records().get(0)[1]));
    }

    @Test
    void setFieldWhenStringShouldUpdateRecord() throws Exception {
        Path binding = writeMiniBinding();
        DbcBinding b = DbcBinding.load(binding);
        WdbcFile f = WdbcFile.fixtureTwoFields(7, "old");
        f.setField(b, 7, "Name", "new");
        f.rebuildStringBlock(b);
        assertEquals("new", f.str(f.records().get(0)[1]));
        Path out = tmp.resolve("out.dbc");
        f.save(out);
        assertTrue(Files.size(out) > 20);
    }

    private Path writeMiniBinding() throws Exception {
        Path p = tmp.resolve("Tiny.txt");
        Files.writeString(p, "uint ID\nuint Name string\n");
        return p;
    }
}
