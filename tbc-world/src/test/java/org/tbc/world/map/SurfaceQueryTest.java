package org.tbc.world.map;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tbc.world.vmap.VMapManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL08-034 — supporting surface selection for ground creatures. */
class SurfaceQueryTest {

    @Test
    void queryWhenFlatGroundShouldReturnMapZ() {
        SurfaceQuery q = SurfaceQuery.of((map, x, y) -> new float[]{12f});
        SurfaceResult r = q.query(0, 1, 2, 12.1f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.VALID, r.kind());
        assertEquals(12f, r.z(), 1e-4f);
    }

    @Test
    void queryWhenBuildingFloorAboveAdtShouldPreferFloorNearHint() {
        SurfaceQuery q = SurfaceQuery.of(
                (map, x, y) -> new float[]{90f},
                (map, x, y) -> new float[]{100f});
        SurfaceResult r = q.query(0, 1, 2, 100.2f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.VALID, r.kind());
        assertEquals(100f, r.z(), 1e-4f);
    }

    @Test
    void queryWhenMultipleFloorsShouldPickNearestToHint() {
        SurfaceQuery q = SurfaceQuery.of((map, x, y) -> new float[]{90f, 100f, 110f});
        SurfaceResult r = q.query(0, 0, 0, 99.5f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(100f, r.z(), 1e-4f);
    }

    @Test
    void queryWhenHoleShouldReturnNoFloor() {
        SurfaceQuery q = SurfaceQuery.of((map, x, y) -> new float[0]);
        SurfaceResult r = q.query(0, 0, 0, 50f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.NO_FLOOR, r.kind());
    }

    @Test
    void queryWhenFlyerShouldKeepHint() {
        SurfaceQuery q = SurfaceQuery.of((map, x, y) -> new float[]{0f});
        SurfaceResult r = q.query(0, 0, 0, 80f, CreatureGrounding.INHABIT_AIR, 10f);
        assertEquals(SurfaceResult.Kind.VALID, r.kind());
        assertEquals(80f, r.z(), 1e-4f);
    }

    @Test
    void queryWhenUnavailableShouldReportUnavailable() {
        SurfaceQuery q = SurfaceQuery.unavailable();
        SurfaceResult r = q.query(0, 0, 0, 12f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.UNAVAILABLE, r.kind());
        assertEquals(12f, r.z(), 1e-4f);
    }

    @Test
    void fromTerrainWhenAdtFarBelowHintShouldKeepHint() {
        Terrain.Height adt = (map, x, y, hint) -> hint - 2f;
        SurfaceQuery q = SurfaceQuery.fromHeight(adt);
        SurfaceResult r = q.query(0, 0, 0, 100f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.VALID, r.kind());
        assertEquals(100f, r.z(), 1e-4f);
    }

    @Test
    void fromTerrainWhenAdtNearHintShouldUseAdt() {
        Terrain.Height adt = (map, x, y, hint) -> 12.2f;
        SurfaceQuery q = SurfaceQuery.fromHeight(adt);
        SurfaceResult r = q.query(0, 0, 0, 12f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(12.2f, r.z(), 1e-4f);
    }

    @Test
    void combineWhenVmapFloorAboveAdtShouldPreferVmapFloor() {
        Terrain.Height adt = (map, x, y, hint) -> 90f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> 100f);
        SurfaceResult r = q.query(0, 0, 0, 100.2f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.VALID, r.kind());
        assertEquals(100f, r.z(), 1e-4f);
    }

    @Test
    void combineWhenStandingAtAdtAboveLowerVmapShouldUseAdt() {
        Terrain.Height adt = (map, x, y, hint) -> 50f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> 20f);
        SurfaceResult r = q.query(0, 0, 0, 50.3f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(50f, r.z(), 1e-4f);
    }

    @Test
    void combineWhenBelowAdtSurfaceShouldUseVmapFloor() {
        Terrain.Height adt = (map, x, y, hint) -> 50f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> 20f);
        SurfaceResult r = q.query(0, 0, 0, 25f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(20f, r.z(), 1e-4f);
    }

    @Test
    void combineWhenVmapMissesAndAdtFarBelowHintShouldKeepHint() {
        Terrain.Height adt = (map, x, y, hint) -> 98f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> Terrain.INVALID);
        SurfaceResult r = q.query(0, 0, 0, 100f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(SurfaceResult.Kind.VALID, r.kind());
        assertEquals(100f, r.z(), 1e-4f);
    }

    @Test
    void combineWhenVmapMissesAndAdtNearHintShouldUseAdt() {
        Terrain.Height adt = (map, x, y, hint) -> 12.2f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> Terrain.INVALID);
        assertEquals(12.2f, q.query(0, 0, 0, 12f, CreatureGrounding.DEFAULT_INHABIT, 10f).z(), 1e-4f);
    }

    @Test
    void combineWhenOnlyVmapHasFloorShouldUseVmap() {
        SurfaceQuery q = SurfaceQuery.combineHeights(Terrain.NONE, (map, x, y, z, search) -> 7f);
        assertEquals(7f, q.query(0, 0, 0, 8f, CreatureGrounding.DEFAULT_INHABIT, 10f).z(), 1e-4f);
    }

    @Test
    void combineWhenAdtFarBelowShouldWidenVmapSearchFromTwoAboveHint() {
        List<float[]> calls = new ArrayList<>();
        Terrain.Height adt = (map, x, y, hint) -> 0f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> {
            calls.add(new float[]{z, search});
            return Terrain.INVALID;
        });
        q.query(0, 0, 0, 100f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(102f, calls.get(0)[0], 1e-4f);
        assertEquals(103f, calls.get(0)[1], 1e-4f);
        assertEquals(10000f, calls.get(1)[1], 1e-4f);
    }

    @Test
    void combineWhenAdtAboveHintShouldRetryVmapNearTerrain() {
        List<float[]> calls = new ArrayList<>();
        Terrain.Height adt = (map, x, y, hint) -> 200f;
        SurfaceQuery q = SurfaceQuery.combineHeights(adt, (map, x, y, z, search) -> {
            calls.add(new float[]{z, search});
            return z == 202f ? 195f : Terrain.INVALID;
        });
        SurfaceResult r = q.query(0, 0, 0, 100f, CreatureGrounding.DEFAULT_INHABIT, 10f);
        assertEquals(195f, r.z(), 1e-4f);
        assertEquals(-10f, calls.get(2)[1], 1e-4f);
        assertEquals(SurfaceQuery.DEFAULT_HEIGHT_SEARCH, calls.get(3)[1], 1e-4f);
    }

    @Test
    void combineWhenFlyerShouldKeepHint() {
        SurfaceQuery q = SurfaceQuery.combineHeights(Terrain.NONE, (map, x, y, z, search) -> 0f);
        assertEquals(80f, q.query(0, 0, 0, 80f, CreatureGrounding.INHABIT_AIR, 10f).z(), 1e-4f);
    }

    @Test
    void combineWhenNoVmapManagerShouldActLikeAdtOnly() {
        Terrain.Height adt = (map, x, y, hint) -> hint - 2f;
        SurfaceQuery q = SurfaceQuery.combine(adt, (VMapManager) null);
        assertEquals(100f, q.query(0, 0, 0, 100f, CreatureGrounding.DEFAULT_INHABIT, 10f).z(), 1e-4f);
    }

    @Test
    void combineWhenVmapDirUnavailableShouldFallBackToAdt() {
        Terrain.Height adt = (map, x, y, hint) -> 12.2f;
        SurfaceQuery q = SurfaceQuery.combine(adt, VMapManager.fromDataDir(null));
        assertEquals(12.2f, q.query(0, 0, 0, 12f, CreatureGrounding.DEFAULT_INHABIT, 10f).z(), 1e-4f);
    }

    @Test
    void resolveOrHintWhenValidShouldUseFloor() {
        assertEquals(5f, SurfaceQuery.resolveOrHint(
                SurfaceResult.valid(5f), 9f), 1e-4f);
    }

    @Test
    void resolveOrHintWhenNoFloorOrUnavailableShouldKeepHint() {
        assertEquals(9f, SurfaceQuery.resolveOrHint(SurfaceResult.noFloor(), 9f), 1e-4f);
        assertEquals(9f, SurfaceQuery.resolveOrHint(SurfaceResult.unavailable(9f), 9f), 1e-4f);
    }

    @Test
    void isBlockingWhenNoFloorShouldBeTrue() {
        assertTrue(SurfaceResult.noFloor().blocksGroundMove());
    }
}
