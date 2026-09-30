package org.tbc.content.compile;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Player seal auras use SpellDuration index 30 (30 min), not stock index 9 (30 s).
 */
class PaladinSealsDurationYamlTest {
    private static final Set<Integer> SEAL_AURA_IDS = Set.of(
            20154, 21084, 20287, 20288, 20289, 20290, 20291, 20292, 20293, 27155,
            21082, 20162, 20305, 20306, 20307, 20308, 27158,
            20164, 31895,
            20165, 20347, 20348, 20349, 27160,
            20166, 20356, 20357, 27166,
            20375, 20915, 20918, 20919, 20920, 27170,
            31892, 31801);

    @Test
    void paladinSealsDurationYamlWhenLoadedShouldSetDurationIndex30ForEverySealAura() throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"))
                .resolve("../content/spells/paladin-seals-duration.yaml")
                .normalize();
        if (!Files.isRegularFile(yaml)) {
            yaml = Path.of("d:/projecten/wow/tbc-server/content/spells/paladin-seals-duration.yaml");
        }
        assertTrue(Files.isRegularFile(yaml), "missing " + yaml.toAbsolutePath());
        List<ContentDelta> deltas = YamlDeltaLoader.loadFile(yaml);
        assertEquals(SEAL_AURA_IDS.size(), deltas.size(), "one delta per seal aura");
        for (ContentDelta d : deltas) {
            assertTrue(SEAL_AURA_IDS.contains(d.id()), "unexpected seal id " + d.id());
            assertEquals("spell", d.kind());
            assertEquals(30, ((Number) d.client().get("Spell").get("DurationIndex")).intValue());
            assertEquals(30, ((Number) d.server().get("DurationIndex")).intValue());
        }
    }
}
