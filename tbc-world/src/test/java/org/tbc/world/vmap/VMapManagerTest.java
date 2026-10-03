package org.tbc.world.vmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.world.map.Terrain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VMAP_7.0 height reader against VMapManager2::getHeight / MapTree / BIH control flow. */
class VMapManagerTest {
    private static final float X = -100f;
    private static final float Y = 200f;
    private static final float[] LO = {-1e5f, -1e5f, -1e5f};
    private static final float[] HI = {1e5f, 1e5f, 1e5f};

    @TempDir
    Path dataDir;

    private Path vmaps() throws IOException {
        return Files.createDirectories(dataDir.resolve("vmaps"));
    }

    /** Horizontal triangle around the internal-rep image of (X, Y), at z = 10. */
    private static float[] floorTriangleAtQuery(float z) {
        float[] c = VMapManager.convertPosition(X, Y, z);
        return new float[]{c[0] - 5, c[1] - 5, z, c[0] + 5, c[1] - 5, z, c[0], c[1] + 5, z};
    }

    private void writeNonTiledFloor(int mapId, float[] verts) throws IOException {
        Path dir = vmaps();
        Files.write(dir.resolve("floor.wmo.vmo"), VMapFixture.model(verts, new int[]{0, 1, 2}));
        VMapFixture.tree(false, 1)
                .spawn(1, new float[3], 1f, LO, HI, "floor.wmo").u32(0)
                .write(dir.resolve(String.format("%03d.vmtree", mapId)));
    }

    @Test
    void convertPositionWhenOriginShouldMirrorAroundMapCenter() {
        float mid = 0.5f * 64f * 533.33333333f;
        assertArrayEquals(new float[]{mid, mid, 5f}, VMapManager.convertPosition(0f, 0f, 5f), 1e-3f);
        assertArrayEquals(new float[]{mid - 100f, mid + 50f, -3f}, VMapManager.convertPosition(100f, -50f, -3f), 1e-3f);
    }

    @Test
    void getHeightWhenNoDataDirShouldReturnInvalid() {
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(null).getHeight(0, X, Y, 12f, 10f));
    }

    @Test
    void getHeightWhenVmapsDirectoryMissingShouldReturnInvalid() {
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 12f, 10f));
    }

    @Test
    void getHeightWhenVmtreeMissingShouldReturnInvalid() throws IOException {
        vmaps();
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 12f, 10f));
    }

    @Test
    void getHeightWhenVmtreeMagicWrongShouldReturnInvalid() throws IOException {
        Files.write(vmaps().resolve("000.vmtree"), "VMAP_9.9\0NODE".getBytes());
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 12f, 10f));
    }

    @Test
    void getHeightWhenVmtreeTruncatedShouldReturnInvalid() throws IOException {
        Files.write(vmaps().resolve("000.vmtree"), "VMAP_7.0\0NODE".getBytes());
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 12f, 10f));
    }

    @Test
    void getHeightWhenFloorBelowPositionShouldReturnFloorZ() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        VMapManager vmap = VMapManager.fromDataDir(dataDir);
        assertEquals(10f, vmap.getHeight(0, X, Y, 14f, 10f), 1e-2f);
    }

    @Test
    void getHeightWhenFloorBeyondSearchDistanceShouldReturnInvalid() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        VMapManager vmap = VMapManager.fromDataDir(dataDir);
        assertEquals(Terrain.INVALID, vmap.getHeight(0, X, Y, 14f, 2f));
    }

    @Test
    void getHeightWhenSearchNegativeShouldLookUpward() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        VMapManager vmap = VMapManager.fromDataDir(dataDir);
        assertEquals(10f, vmap.getHeight(0, X, Y, 4f, -10f), 1e-2f);
        assertEquals(Terrain.INVALID, vmap.getHeight(0, X, Y, 4f, 10f));
    }

    @Test
    void getHeightWhenOutsideTriangleShouldReturnInvalid() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        VMapManager vmap = VMapManager.fromDataDir(dataDir);
        assertEquals(Terrain.INVALID, vmap.getHeight(0, X + 50f, Y, 14f, 10f));
    }

    @Test
    void getHeightWhenOtherMapShouldReturnInvalid() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(1, X, Y, 14f, 10f));
    }

    @Test
    void getHeightWhenModelFileMissingShouldReturnInvalid() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        Files.delete(vmaps().resolve("floor.wmo.vmo"));
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 14f, 10f));
    }

    @Test
    void getHeightWhenSpawnScaledAndMovedShouldApplyInstanceTransform() throws IOException {
        float[] c = VMapManager.convertPosition(X, Y, 0f);
        float[] local = {-2.5f, -2.5f, 5f, 2.5f, -2.5f, 5f, 0f, 2.5f, 5f};
        Path dir = vmaps();
        Files.write(dir.resolve("small.m2.vmo"), VMapFixture.model(local, new int[]{0, 1, 2}));
        VMapFixture.tree(false, 1)
                .spawn(1, new float[]{c[0], c[1], 3f}, 2f, LO, HI, "small.m2").u32(0)
                .write(dir.resolve("000.vmtree"));
        assertEquals(13f, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 20f, 10f), 1e-2f);
    }

    @Test
    void getHeightWhenTiledShouldLoadTileFileWithYBeforeX() throws IOException {
        Path dir = vmaps();
        Files.write(dir.resolve("floor.wmo.vmo"),
                VMapFixture.model(floorTriangleAtQuery(10f), new int[]{0, 1, 2}));
        VMapFixture.tree(true, 1).write(dir.resolve("000.vmtree"));
        int tileX = (int) (32 - X / Terrain.SIZE_OF_GRIDS);
        int tileY = (int) (32 - Y / Terrain.SIZE_OF_GRIDS);
        assertTrue(tileX != tileY);
        VMapFixture.create().raw("VMAP_7.0").u32(1)
                .spawn(1, new float[3], 1f, LO, HI, "floor.wmo").u32(0)
                .write(dir.resolve(String.format("000_%02d_%02d.vmtile", tileY, tileX)));
        assertEquals(10f, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 14f, 10f), 1e-2f);
    }

    @Test
    void getHeightWhenTileFileNamedXBeforeYShouldReturnInvalid() throws IOException {
        Path dir = vmaps();
        Files.write(dir.resolve("floor.wmo.vmo"),
                VMapFixture.model(floorTriangleAtQuery(10f), new int[]{0, 1, 2}));
        VMapFixture.tree(true, 1).write(dir.resolve("000.vmtree"));
        int tileX = (int) (32 - X / Terrain.SIZE_OF_GRIDS);
        int tileY = (int) (32 - Y / Terrain.SIZE_OF_GRIDS);
        VMapFixture.create().raw("VMAP_7.0").u32(1)
                .spawn(1, new float[3], 1f, LO, HI, "floor.wmo").u32(0)
                .write(dir.resolve(String.format("000_%02d_%02d.vmtile", tileX, tileY)));
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, X, Y, 14f, 10f));
    }

    @Test
    void tileFileNameWhenTileCoordsShouldSwapXAndY() {
        assertEquals("000_31_32.vmtile", StaticMapTree.tileFileName(0, 32, 31));
        assertEquals("530_05_40.vmtile", StaticMapTree.tileFileName(530, 40, 5));
    }

    @Test
    void getHeightWhenTileOutsideWorldShouldReturnInvalid() throws IOException {
        writeNonTiledFloor(0, floorTriangleAtQuery(10f));
        assertEquals(Terrain.INVALID, VMapManager.fromDataDir(dataDir).getHeight(0, 40000f, Y, 14f, 10f));
    }

    @Test
    void eulerZyxWhenYawQuarterTurnShouldRotateXTowardY() {
        float[] m = ModelInstance.eulerZyx((float) (Math.PI / 2), 0f, 0f);
        assertArrayEquals(new float[]{0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f}, m, 1e-6f);
    }

    @Test
    void intersectTriangleWhenRayFacesTriangleShouldReportDistance() {
        GroupModel g = new GroupModel(new float[]{0, 0, 0, 10, 0, 0, 0, 10, 0}, new int[]{0, 1, 2}, Bih.empty());
        float[] dist = {100f};
        assertTrue(g.intersectTriangle(0, Ray.of(2f, 2f, 4f, 0f, 0f, -1f), dist));
        assertEquals(4f, dist[0], 1e-5f);
        assertFalse(g.intersectTriangle(0, Ray.of(2f, 2f, 4f, 0f, 0f, 1f), new float[]{100f}));
        assertFalse(g.intersectTriangle(0, Ray.of(2f, 2f, 4f, 1f, 0f, 0f), new float[]{100f}));
        assertFalse(g.intersectTriangle(0, Ray.of(9f, 9f, 4f, 0f, 0f, -1f), new float[]{100f}));
        assertFalse(g.intersectTriangle(0, Ray.of(2f, 2f, 4f, 0f, 0f, -1f), new float[]{3f}));
    }

    @Test
    void intersectRayWhenSplitNodeShouldVisitOnlyChildUnderRay() {
        int[] tree = {3, Float.floatToRawIntBits(1f), Float.floatToRawIntBits(2f),
                3 << 30, 1, 0, (3 << 30) | 1, 1, 0};
        Bih bih = new Bih(new float[]{0, 0, -1}, new float[]{3, 1, 1}, tree, new int[]{0, 1});
        assertEquals(List.of(1), visited(bih, 2.5f));
        assertEquals(List.of(0), visited(bih, 0.5f));
        assertEquals(List.of(), visited(bih, 1.5f));
    }

    private static List<Integer> visited(Bih bih, float x) {
        List<Integer> seen = new ArrayList<>();
        bih.intersectRay(Ray.of(x, 0.5f, 5f, 0f, 0f, -1f), (r, entry, d) -> {
            seen.add(entry);
            return false;
        }, new float[]{100f});
        return seen;
    }
}
