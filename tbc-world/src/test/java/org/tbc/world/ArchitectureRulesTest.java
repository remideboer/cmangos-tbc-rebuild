package org.tbc.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-scan architecture gate for the modular refactoring
 * ({@code tbc-server/docs/maintainability-refactoring-plan.md}).
 *
 * <p>Hard rules fail on any violation. Ratchets pin the current size of a hotspot; a refactor
 * cycle that shrinks a hotspot must lower the constant to the new value. Never raise a ratchet.
 */
class ArchitectureRulesTest {
    /** Ratchets — lower only. Baseline measured 2026-10-07. */
    static final int WORLD_SESSION_MAX_LINES = 1797;
    static final int WORLD_SESSION_MAX_CASE_LABELS = 63;
    static final int LATER_OPCODES_MAX_LINES = 0;
    static final int WORLD_MAX_LINES = 1678;
    static final int WORLD_MAX_SESSION_REFS = 99;
    static final int OBJECT_MGR_MAX_LINES = 2067;
    static final int SPELL_ENGINE_MAX_LINES = 3765;
    /** Content is a facade over QuestGiver/Gossip/Vendor/Banker; new NPC behavior goes into those, not here. */
    static final int CONTENT_MAX_LINES = 511;
    /** Table SQL lives in persist/{Inventory,QuestStatus,Aura,Spell}Persist; CharacterStore orchestrates save/load. */
    static final int CHARACTER_STORE_MAX_LINES = 1320;
    /** Lines in entity/spell/combat/content/map that name {@code org.tbc.world.session.}. */
    static final int DOMAIN_SESSION_DEPENDENCIES_MAX = 20;
    /**
     * Raw {@code objectMgr.items.get(...)} map reads and {@code objectMgr.spawns} list touches outside
     * ObjectMgr and its seeds/loaders; consumers go through the content/catalog interfaces.
     */
    static final int RAW_CATALOG_MAP_READS_MAX = 0;

    private static final Path MAIN = mainRoot();

    private static Path mainRoot() {
        Path p = Path.of("src", "main", "java", "org", "tbc", "world");
        if (!Files.isDirectory(p)) {
            p = Path.of("tbc-world").resolve(p);
        }
        assertTrue(Files.isDirectory(p), "tbc-world main sources not found from " + Path.of("").toAbsolutePath());
        return p;
    }

    @Test
    void productionWhenImportingTestDoublesShouldHaveNone() {
        List<String> hits = linesMatching(MAIN, l -> l.startsWith("import org.tbc.bdd"));
        assertEquals(List.of(), hits, "production code must not import tbc-tests types");
    }

    @Test
    void nettyWhenImportedOutsideTransportShouldHaveNone() {
        List<String> hits = linesMatching(MAIN, l -> l.startsWith("import io.netty")).stream()
                .filter(h -> !h.contains("/net/") && !h.contains("WorldMain.java"))
                .toList();
        assertEquals(List.of(), hits, "io.netty is transport-only (net/ and WorldMain)");
    }

    @Test
    void domainWhenDependingOnSessionShouldNotGrow() {
        int count = 0;
        for (String dir : List.of("entity", "spell", "combat", "content", "map")) {
            count += linesMatching(MAIN.resolve(dir), l -> l.contains("org.tbc.world.session.")).size();
        }
        ratchet("domain → session dependencies", count, DOMAIN_SESSION_DEPENDENCIES_MAX);
    }

    @Test
    void worldSessionShouldNotGrow() {
        List<String> lines = read(MAIN.resolve("session/WorldSession.java"));
        ratchet("WorldSession lines", lines.size(), WORLD_SESSION_MAX_LINES);
        long cases = lines.stream().filter(l -> l.contains("case Opcodes.")).count();
        ratchet("WorldSession case labels", (int) cases, WORLD_SESSION_MAX_CASE_LABELS);
    }

    @Test
    void laterOpcodesShouldNotGrow() {
        Path p = MAIN.resolve("session/LaterOpcodes.java");
        int lines = Files.exists(p) ? read(p).size() : 0;
        ratchet("LaterOpcodes lines", lines, LATER_OPCODES_MAX_LINES);
    }

    @Test
    void worldShouldNotGrow() {
        List<String> lines = read(MAIN.resolve("world/World.java"));
        ratchet("World lines", lines.size(), WORLD_MAX_LINES);
        int refs = 0;
        for (String l : lines) {
            refs += occurrences(l, ".session");
        }
        ratchet("World .session references", refs, WORLD_MAX_SESSION_REFS);
    }

    @Test
    void objectMgrShouldNotGrow() {
        ratchet("ObjectMgr lines", read(MAIN.resolve("content/ObjectMgr.java")).size(), OBJECT_MGR_MAX_LINES);
    }

    @Test
    void rawCatalogMapReadsShouldNotGrow() {
        java.util.regex.Pattern raw = java.util.regex.Pattern.compile(
                "\\b(objectMgr|mgr)\\.((items|creatures|quests|gameObjects)\\.(get|containsKey|getOrDefault)\\(|(spawns|goSpawns)\\b)");
        List<String> hits = linesMatching(MAIN, l -> raw.matcher(l).find()).stream()
                .filter(h -> !h.contains("/content/ObjectMgr.java")
                        && !h.matches(".*/content/\\w+(Seed|Loader)\\.java: .*"))
                .toList();
        ratchet("raw catalog map reads", hits.size(), RAW_CATALOG_MAP_READS_MAX);
    }

    @Test
    void contentFacadeShouldNotGrow() {
        ratchet("Content lines", read(MAIN.resolve("content/Content.java")).size(), CONTENT_MAX_LINES);
    }

    @Test
    void characterStoreShouldNotGrow() {
        ratchet("CharacterStore lines", read(MAIN.resolve("persist/CharacterStore.java")).size(),
                CHARACTER_STORE_MAX_LINES);
    }

    @Test
    void spellEngineShouldNotGrow() {
        ratchet("SpellEngine lines", read(MAIN.resolve("spell/SpellEngine.java")).size(), SPELL_ENGINE_MAX_LINES);
    }

    private static void ratchet(String what, int measured, int max) {
        assertTrue(measured <= max, what + " = " + measured + " exceeds ratchet " + max
                + " (shrink the hotspot; never raise the constant)");
    }

    private static int occurrences(String line, String needle) {
        int n = 0;
        for (int i = line.indexOf(needle); i >= 0; i = line.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    private static List<String> read(Path p) {
        try {
            return Files.readAllLines(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> linesMatching(Path root, java.util.function.Predicate<String> match) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(f -> f.toString().endsWith(".java"))
                    .flatMap(f -> read(f).stream().filter(match)
                            .map(l -> f.toString().replace('\\', '/') + ": " + l.trim()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
