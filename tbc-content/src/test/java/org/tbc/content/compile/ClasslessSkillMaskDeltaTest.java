package org.tbc.content.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.content.dbc.WdbcFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasslessSkillMaskDeltaTest {
    @TempDir
    Path tmp;

    @Test
    void compileWhenClasslessSkillMaskShouldOrBitIntoNonZeroClassMasks() throws Exception {
        Path content = tmp.resolve("content");
        Path bindings = content.resolve("bindings/tbc243");
        Files.createDirectories(bindings);
        Files.writeString(bindings.resolve("SkillLineAbility.txt"), """
                uint ID
                uint SkillLine
                uint Spell
                uint RaceMask
                uint ClassMask
                uint ExcludeRace
                uint ExcludeClass
                uint MinSkillLineRank
                uint SupercededBySpell
                uint AcquireMethod
                uint TrivialSkillLineRankHigh
                uint TrivialSkillLineRankLow
                uint CharacterPoints0
                uint CharacterPoints1
                uint NumSkillUps
                """);
        Files.writeString(bindings.resolve("SkillRaceClassInfo.txt"), """
                uint ID
                uint SkillID
                uint RaceMask
                uint ClassMask
                uint Flags
                uint MinLevel
                uint SkillTierID
                uint SkillCostIndex
                """);
        // Minimal Spell binding so other tests' assumptions aren't required here.
        Files.writeString(bindings.resolve("Spell.txt"), """
                uint ID
                uint Unused1
                """);
        Path classes = content.resolve("classes");
        Files.createDirectories(classes);
        Files.writeString(classes.resolve("classless-skill-mask.yaml"), """
                kind: classless-skill-mask
                id: 6
                client:
                  SkillLineAbility:
                    orClassMask: 32
                  SkillRaceClassInfo:
                    orClassMask: 32
                """);

        Path base = tmp.resolve("dbc");
        Files.createDirectories(base);
        List<int[]> sla = new ArrayList<>();
        // warrior Battle Shout classMask=1; open racial classMask=0
        sla.add(new int[]{1, 256, 6673, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        sla.add(new int[]{2, 8, 133, 0, 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        sla.add(new int[]{3, 95, 668, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        new WdbcFile(15, 60, sla, new byte[]{0}).save(base.resolve("SkillLineAbility.dbc"));
        List<int[]> src = new ArrayList<>();
        src.add(new int[]{1, 26, 0, 1, 0, 0, 0, 0});
        src.add(new int[]{2, 95, 0, 0, 0, 0, 0, 0});
        new WdbcFile(8, 32, src, new byte[]{0}).save(base.resolve("SkillRaceClassInfo.dbc"));
        new WdbcFile(2, 8, List.of(new int[]{1, 0}), new byte[]{0}).save(base.resolve("Spell.dbc"));

        Path out = tmp.resolve("out");
        var result = new ContentCompiler().compile(content, base, out, "patch-tbc-custom.MPQ");
        assertEquals(1, result.deltas());
        assertTrue(Files.isRegularFile(out.resolve("dbc/SkillLineAbility.dbc")));
        assertTrue(Files.isRegularFile(out.resolve("dbc/SkillRaceClassInfo.dbc")));

        WdbcFile mergedSla = WdbcFile.load(out.resolve("dbc/SkillLineAbility.dbc"));
        assertEquals(1 | 32, mergedSla.records().get(0)[4]);
        assertEquals(128 | 32, mergedSla.records().get(1)[4]);
        assertEquals(0, mergedSla.records().get(2)[4], "zero ClassMask stays any-class");

        WdbcFile mergedSrc = WdbcFile.load(out.resolve("dbc/SkillRaceClassInfo.dbc"));
        assertEquals(1 | 32, mergedSrc.records().get(0)[3]);
        assertEquals(0, mergedSrc.records().get(1)[3]);
    }
}