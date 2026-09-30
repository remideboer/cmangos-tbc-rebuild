package org.tbc.content.compile;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Player blessing auras use SpellDuration index 30 (30 min), not stock 10 min.
 * Same lab approach as {@link PaladinSealsDurationYamlTest} / SoR.
 */
class PaladinBlessingsDurationYamlTest {
    /** CMaNGOS spell group 7 — 10-minute blessings only (not Hands / Greater). */
    private static final Set<Integer> BLESSING_AURA_IDS = Set.of(
            // Blessing of Might
            19740, 19834, 19835, 19836, 19837, 19838, 25291, 27140,
            // Blessing of Wisdom
            19742, 19850, 19852, 19853, 19854, 25290, 27142,
            // Blessing of Light
            19977, 19978, 19979, 27144,
            // Blessing of Kings
            20217,
            // Blessing of Sanctuary
            20911, 20912, 20913, 20914, 27168);

    @Test
    void paladinBlessingsDurationYamlWhenLoadedShouldSetDurationIndex30ForEveryTenMinuteBlessing()
            throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"))
                .resolve("../content/spells/paladin-blessings-duration.yaml")
                .normalize();
        if (!Files.isRegularFile(yaml)) {
            yaml = Path.of("d:/projecten/wow/tbc-server/content/spells/paladin-blessings-duration.yaml");
        }
        assertTrue(Files.isRegularFile(yaml), "missing " + yaml.toAbsolutePath());
        List<ContentDelta> deltas = YamlDeltaLoader.loadFile(yaml);
        assertEquals(BLESSING_AURA_IDS.size(), deltas.size(), "one delta per 10-min blessing");
        for (ContentDelta d : deltas) {
            assertTrue(BLESSING_AURA_IDS.contains(d.id()), "unexpected blessing id " + d.id());
            assertEquals("spell", d.kind());
            assertEquals(30, ((Number) d.client().get("Spell").get("DurationIndex")).intValue());
            assertEquals(30, ((Number) d.server().get("DurationIndex")).intValue());
        }
    }
}
