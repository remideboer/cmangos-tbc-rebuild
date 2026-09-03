package org.tbc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecFilesP0Test {
    @Test
    void tpSl31TbcRaidFiles() throws Exception {
        Path scripts = specScripts();
        assertFileHas(scripts.resolve("karazhan.md"), "532", "boss_curator", "30383");
        assertFileHas(scripts.resolve("gruuls_lair.md"), "565", "boss_gruul", "36300");
        assertFileHas(scripts.resolve("magtheridons_lair.md"), "544", "boss_magtheridon", "30616");
        assertFileHas(scripts.resolve("serpentshrine.md"), "548", "boss_fathomlord_karathress", "38441");
        assertFileHas(scripts.resolve("the_eye.md"), "550", "boss_alar", "34121");
        assertFileHas(scripts.resolve("hyjal.md"), "534", "boss_anetheron", "31306");
        assertFileHas(scripts.resolve("black_temple.md"), "564", "boss_najentus", "39837");
        assertFileHas(scripts.resolve("sunwell.md"), "580", "boss_brutallus", "45150");
        assertFileHas(scripts.resolve("zulaman.md"), "568", "boss_akilzon", "43622");
    }

    @Test
    void tpSl32TbcFiveManFiles() throws Exception {
        Path scripts = specScripts();
        assertFileHas(scripts.resolve("hfc_ramparts.md"), "543");
        assertFileHas(scripts.resolve("hfc_bf.md"), "542");
        assertFileHas(scripts.resolve("hfc_sh.md"), "540");
        assertFileHas(scripts.resolve("cf_slaves.md"), "547");
        assertFileHas(scripts.resolve("cf_underbog.md"), "546");
        assertFileHas(scripts.resolve("cf_steam.md"), "545");
        assertFileHas(scripts.resolve("auch_mt.md"), "557");
        assertFileHas(scripts.resolve("auch_ac.md"), "558");
        assertFileHas(scripts.resolve("auch_seth.md"), "556");
        assertFileHas(scripts.resolve("auch_sl.md"), "555");
        assertFileHas(scripts.resolve("tk_bot.md"), "553");
        assertFileHas(scripts.resolve("tk_mech.md"), "554");
        assertFileHas(scripts.resolve("tk_arc.md"), "552");
        assertFileHas(scripts.resolve("magisters.md"), "585");
        assertFileHas(scripts.resolve("cot_hillsbrad.md"), "560");
        assertFileHas(scripts.resolve("cot_blackmorass.md"), "269");
    }

    @Test
    void tpSl33ClassicRaidFiles() throws Exception {
        Path scripts = specScripts();
        assertFileHas(scripts.resolve("molten_core.md"), "409");
        assertFileHas(scripts.resolve("blackwing_lair.md"), "469");
        assertFileHas(scripts.resolve("aq20.md"), "509");
        assertFileHas(scripts.resolve("aq40.md"), "531");
        assertFileHas(scripts.resolve("naxxramas.md"), "533");
        assertFileHas(scripts.resolve("zulgurub.md"), "309");
        assertFileHas(scripts.resolve("onyxia.md"), "249");
        assertFileHas(scripts.resolve("world-remaining.md"), "boss_azuregos");
    }

    @Test
    void tpSl34ClassSpellScripts() throws Exception {
        Path root = specRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(root != null, "spec/ not in this checkout");
        Path file = root.resolve("05-domain").resolve("class-spell-scripts.md");
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("spell_warrior_execute"));
        assertTrue(text.contains("5308"));
        assertTrue(text.contains("20647"));
        assertTrue(text.contains("spell_unstable_affliction"));
        assertTrue(text.contains("30108"));
    }

    private static void assertFileHas(Path file, String... needles) throws Exception {
        assertTrue(Files.isRegularFile(file), "missing " + file);
        String text = Files.readString(file, StandardCharsets.UTF_8);
        for (String n : needles) {
            assertTrue(text.contains(n), file.getFileName() + " missing " + n);
        }
    }

    private static Path specRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        List<Path> candidates = List.of(
                cwd.resolve("../../spec"),
                cwd.resolve("../spec"),
                cwd.resolve("spec"),
                cwd.getParent() == null ? cwd : cwd.getParent().resolve("spec"));
        for (Path p : candidates) {
            Path n = p.normalize();
            if (Files.isDirectory(n.resolve("05-domain").resolve("scripts"))) {
                return n;
            }
        }
        return null;
    }

    private static Path specScripts() {
        Path root = specRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(root != null, "spec/ not in this checkout");
        return root.resolve("05-domain").resolve("scripts");
    }
}
