package org.tbc.content.compile;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SoR tips omit live {@code $MW}/{@code $MWS} formulas — those diverge from server combat on 8606.
 * Marker text proves the overlay Spell.dbc is loaded.
 */
class SorYamlExampleTest {
    @Test
    void sealOfRighteousnessYamlWhenLoadedShouldUsePlainTipWithoutWeaponTokenMath() throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"))
                .resolve("../content/spells/seal-of-righteousness.yaml")
                .normalize();
        if (!Files.isRegularFile(yaml)) {
            yaml = Path.of("d:/projecten/wow/tbc-server/content/spells/seal-of-righteousness.yaml");
        }
        assertTrue(Files.isRegularFile(yaml), "missing " + yaml.toAbsolutePath());
        List<ContentDelta> deltas = YamlDeltaLoader.loadFile(yaml);
        assertTrue(deltas.size() >= 10, "expected all SoR ranks, got " + deltas.size());
        for (ContentDelta d : deltas) {
            assertTrue("spell".equals(d.kind()));
            var spell = d.client().get("Spell");
            String tip = String.valueOf(spell.get("AuraDescription_lang_enUS"));
            String desc = String.valueOf(spell.get("Description_lang_enUS"));
            assertFalse(tip.contains("$HND"), "tip still has HND for " + d.id());
            assertFalse(desc.contains("$HND"), "desc still has HND for " + d.id());
            assertFalse(tip.contains("$MWS"), "no live weapon formula in tip for " + d.id());
            assertFalse(tip.contains("$MW"), "no live weapon formula in tip for " + d.id());
            assertTrue(tip.contains("see combat log"), "overlay marker missing for " + d.id());
            assertTrue(desc.contains("see combat log"), "overlay marker missing in desc for " + d.id());
        }
    }
}
