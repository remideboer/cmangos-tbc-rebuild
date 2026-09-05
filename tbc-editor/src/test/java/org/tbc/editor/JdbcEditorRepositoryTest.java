package org.tbc.editor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcEditorRepositoryTest {
    private DbPool login;
    private DbPool chars;

    @AfterEach
    void tearDown() {
        if (login != null) {
            login.close();
        }
        if (chars != null) {
            chars.close();
        }
    }

    @Test
    void accountLookupShouldListAndFindWithoutPasswords() throws Exception {
        login = memPool("login");
        try (Connection c = login.get(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE account (id INT PRIMARY KEY, username VARCHAR(32), expansion INT)");
            s.execute("INSERT INTO account VALUES (1, 'REMI', 1), (2, 'REMI2', 0)");
        }
        JdbcAccountLookup lookup = new JdbcAccountLookup(login);
        List<AccountRef> rows = lookup.list();
        assertEquals(2, rows.size());
        assertEquals("REMI", rows.get(0).username());
        assertEquals(1, lookup.findById(1).expansion());
        assertNull(lookup.findById(9));
    }

    @Test
    void characterRepositoryShouldListSearchFallbacksAndIgnorePutRemove() throws Exception {
        chars = memPool("chars");
        try (Connection c = chars.get(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE characters (
                      guid INT PRIMARY KEY,
                      account INT,
                      name VARCHAR(12),
                      race INT,
                      `class` INT,
                      gender INT,
                      level INT,
                      map INT,
                      online INT,
                      deleteDate INT
                    )
                    """);
            s.execute("INSERT INTO characters VALUES (1, 1, 'Piep', 7, 1, 0, 6, 0, 0, NULL)");
            s.execute("INSERT INTO characters VALUES (2, 1, 'Gone', 1, 1, 0, 1, 0, 0, 1)");
            s.execute("INSERT INTO characters VALUES (3, 2, 'OrcOne', 2, 1, 0, 1, 1, 1, NULL)");
        }
        JdbcCharacterRepository repo = new JdbcCharacterRepository(chars);
        List<CharacterSummary> all = repo.listAll();
        assertEquals(2, all.size());
        assertEquals("OrcOne", all.get(0).name());
        assertTrue(all.get(0).online());
        assertFalse(repo.find(1L).online());
        assertEquals(1, repo.listByAccount(1).size());
        assertNull(repo.find(2L));
        repo.put(new CharacterSummary(9L, 1, "", "Nope", 1, 1, 0, 1, 0, false));
        repo.remove(1L);
        assertEquals(2, repo.listAll().size());
        assertEquals("Piep", repo.find(1L).name());
    }

    @Test
    void characterRepositoryWhenOnlineColumnMissingShouldFallBack() throws Exception {
        chars = memPool("chars2");
        try (Connection c = chars.get(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE characters (
                      guid INT PRIMARY KEY,
                      account INT,
                      name VARCHAR(12),
                      race INT,
                      `class` INT,
                      gender INT,
                      level INT,
                      map INT,
                      deleteDate INT
                    )
                    """);
            s.execute("INSERT INTO characters VALUES (4, 1, 'NoOn', 1, 1, 0, 1, 0, NULL)");
        }
        JdbcCharacterRepository repo = new JdbcCharacterRepository(chars);
        CharacterSummary row = repo.find(4L);
        assertEquals("NoOn", row.name());
        assertFalse(row.online());
    }

    @Test
    void characterRepositoryWhenDeleteDateMissingShouldFallBack() throws Exception {
        chars = memPool("chars3");
        try (Connection c = chars.get(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE characters (
                      guid INT PRIMARY KEY,
                      account INT,
                      name VARCHAR(12),
                      race INT,
                      `class` INT,
                      gender INT,
                      level INT,
                      map INT
                    )
                    """);
            s.execute("INSERT INTO characters VALUES (5, 1, 'Bare', 1, 1, 0, 1, 0)");
        }
        JdbcCharacterRepository repo = new JdbcCharacterRepository(chars);
        assertEquals("Bare", repo.find(5L).name());
        assertEquals(1, repo.listByAccount(1).size());
    }

    @Test
    void characterRepositoryWhenTableMissingShouldThrow() {
        chars = memPool("empty");
        JdbcCharacterRepository repo = new JdbcCharacterRepository(chars);
        assertThrows(EditorException.class, repo::listAll);
        JdbcAccountLookup lookup = new JdbcAccountLookup(chars);
        assertThrows(EditorException.class, lookup::list);
        assertThrows(EditorException.class, () -> lookup.findById(1));
    }

    private static DbPool memPool(String name) {
        String url = "jdbc:h2:mem:ed_" + name + "_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        return new DbPool(url, "sa", "", "editor-" + name);
    }
}
