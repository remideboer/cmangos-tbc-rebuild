package org.tbc.world.mmap;

import org.junit.jupiter.api.Test;
import org.tbc.world.ai.PathFinder;
import org.tbc.world.ai.PathType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MMapManagerTest {

    @Test
    void fromDataDirWhenNullShouldBeUnavailable() {
        MMapManager m = MMapManager.fromDataDir(null, true);
        assertFalse(m.available(0));
        PathFinder pf = new PathFinder(m, true);
        assertTrue(pf.calculate(0, 0, 0, 0, 1, 0, 0, false, false).nopath());
    }

    @Test
    void validTileHeaderWhenMmapV8ShouldPass() {
        ByteBuffer b = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(MMapManager.MMAP_MAGIC);
        b.putInt(1);
        b.putInt(MMapManager.MMAP_VERSION);
        b.putInt(0);
        b.putInt(0);
        assertTrue(MMapManager.validTileHeader(b.array()));
    }

    @Test
    void validTileHeaderWhenWrongMagicShouldFail() {
        assertFalse(MMapManager.validTileHeader(new byte[20]));
    }

    @Test
    void hasMapFileWhenParamsPresentShouldBeTrue() throws Exception {
        Path dir = Files.createTempDirectory("mmaps-test");
        Path mmaps = dir.resolve("mmaps");
        Files.createDirectory(mmaps);
        Files.write(mmaps.resolve("000.mmap"), new byte[28]);
        MMapManager m = MMapManager.fromDataDir(dir, true);
        assertTrue(m.hasMapFile(0));
        assertFalse(m.available(0)); // no native in CI
        PathFinder ci = new PathFinder(m, false);
        assertEquals(PathType.NOT_USING_PATH | PathType.SHORTCUT,
                ci.calculate(0, 0, 0, 0, 2, 0, 0, false, false).type());
    }

    @Test
    void decodeWhenTripletsShouldBuildWaypoints() {
        assertEquals(2, MMapManager.decode(new float[]{1, 2, 3, 4, 5, 6}).size());
        assertTrue(MMapManager.decode(new float[]{1, 2}).isEmpty());
    }
}
