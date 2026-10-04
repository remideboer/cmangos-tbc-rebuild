package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.content.dbc.WdbcFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapOverlaysTest {
    @Test
    void matchingWhenChildAreaShouldKeepThatZonesOverlaysOnly() {
        byte[] strings = "\0SunstriderIsle\0Goldshire\0Falthrien\0".getBytes(StandardCharsets.UTF_8);
        int[] isle = row(3431, 0, 0, 0, strings, "SunstriderIsle", 512, 512, 195, 5);
        int[] gold = row(87, 0, 0, 0, strings, "Goldshire", 240, 220, 250, 270);
        int[] nested = row(3432, 0, 0, 0, strings, "Falthrien", 128, 128, 400, 80);
        WdbcFile dbc = new WdbcFile(17, 68, List.of(isle, gold, nested), strings);
        Map<Integer, Integer> parents = Map.of(3431, 3430, 3432, 3431, 87, 12);
        List<WorldMapOverlays.Overlay> eversong = WorldMapOverlays.matching(dbc, parents, 3430);
        assertEquals(List.of("SunstriderIsle", "Falthrien"),
                eversong.stream().map(WorldMapOverlays.Overlay::texture).toList());
        assertEquals(512, eversong.get(0).width());
        assertEquals(195, eversong.get(0).offsetX());
        assertEquals(5, eversong.get(0).offsetY());
        assertEquals(List.of("Goldshire"),
                WorldMapOverlays.matching(dbc, parents, 12).stream()
                        .map(WorldMapOverlays.Overlay::texture).toList());
        assertTrue(WorldMapOverlays.matching(dbc, parents, 0).isEmpty());
    }

    private static int[] row(int area, int a2, int a3, int a4, byte[] strings, String texture,
                              int width, int height, int ox, int oy) {
        int[] row = new int[17];
        row[2] = area;
        row[3] = a2;
        row[4] = a3;
        row[5] = a4;
        row[8] = indexOf(strings, texture);
        row[9] = width;
        row[10] = height;
        row[11] = ox;
        row[12] = oy;
        return row;
    }

    private static int indexOf(byte[] strings, String texture) {
        byte[] needle = texture.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i < strings.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (strings[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        throw new IllegalStateException(texture);
    }
}
