package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;
import org.tbc.world.map.WorldMapAreas;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapClientAreasTest {
    @Test
    void mergeClientAreasWhenNoArchiveShouldLeaveTheSeed() {
        WorldMapAreas areas = WorldMapAreas.seeded();
        assertEquals(0, WorldMapBlp.mergeClientAreas(Path.of("missing-data-dir"), areas));
        assertEquals(null, areas.byDisplayName("Darkshore"));
    }

    @Test
    void mergeClientAreasWhenTbcClientPresentShouldLoadDarkshoreAndFelwood() {
        Path data = clientData();
        Assumptions.assumeTrue(data != null, "8606 client Data folder is not in this checkout");
        WorldMapAreas areas = WorldMapAreas.seeded();
        int added = WorldMapBlp.mergeClientAreas(data, areas);
        assertTrue(added > 20, "WorldMapArea rows from the client");
        WorldMapArea darkshore = areas.byDisplayName("Darkshore");
        WorldMapArea felwood = areas.byDisplayName("Felwood");
        assertNotNull(darkshore);
        assertNotNull(felwood);
        assertEquals("Darkshore", darkshore.internalName());
        assertEquals("Felwood", felwood.internalName());
        assertEquals(1, darkshore.mapId());
        assertEquals(1, felwood.mapId());
        RegionMinimap.Raster darkMap = WorldMapBlp.load(data, darkshore);
        RegionMinimap.Raster felMap = WorldMapBlp.load(data, felwood);
        assertNotNull(darkMap);
        assertNotNull(felMap);
        assertTrue(darkMap.width() >= 256 && darkMap.height() >= 256);
        assertTrue(felMap.width() >= 256 && felMap.height() >= 256);
        assertTrue(painted(darkMap));
        assertTrue(painted(felMap));
        assertTrue(new WorldMapAreaMapper(WorldMapAreas.SUNSTRIDER).contains(10349.6f, -6357.29f));
    }

    private static boolean painted(RegionMinimap.Raster image) {
        int dark = 0xFF202428;
        for (int pixel : image.argb()) {
            if (pixel != dark && pixel != 0) {
                return true;
            }
        }
        return false;
    }

    private static Path clientData() {
        Path[] candidates = {
                Path.of("..", "..", "WoW-2.4.3-client", "Data"),
                Path.of("..", "WoW-2.4.3-client", "Data")
        };
        for (Path candidate : candidates) {
            Path normal = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normal)) {
                return normal;
            }
        }
        return null;
    }
}
