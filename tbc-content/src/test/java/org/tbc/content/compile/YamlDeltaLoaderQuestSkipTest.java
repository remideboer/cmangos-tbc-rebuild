package org.tbc.content.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlDeltaLoaderQuestSkipTest {
    @TempDir
    Path tmp;

    @Test
    void loadTreeWhenQuestYamlUnderContentQuestsShouldSkipAndStillLoadSpells() throws Exception {
        Path spells = tmp.resolve("spells");
        Files.createDirectories(spells);
        Files.writeString(spells.resolve("seal.yaml"), """
                kind: spell
                id: 20154
                client:
                  Spell:
                    Description_lang_enUS: "ok"
                """);
        Path drafts = tmp.resolve("quests").resolve("drafts");
        Files.createDirectories(drafts);
        Files.writeString(drafts.resolve("95001.yaml"), """
                kind: quest
                id: 95001
                title: Draft Only
                """);
        Path published = tmp.resolve("quests").resolve("published");
        Files.createDirectories(published);
        Files.writeString(published.resolve("95001.yaml"), """
                kind: quest
                id: 95001
                title: Published
                """);

        List<ContentDelta> deltas = YamlDeltaLoader.loadTree(tmp);
        assertEquals(1, deltas.size());
        assertEquals("spell", deltas.get(0).kind());
        assertEquals(20154, deltas.get(0).id());
        assertTrue(deltas.stream().noneMatch(d -> "quest".equals(d.kind())));
    }
}
