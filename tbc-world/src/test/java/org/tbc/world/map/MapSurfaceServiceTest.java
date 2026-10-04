package org.tbc.world.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSurfaceServiceTest {
    @Test
    void candidateFloorsWhenOutdoorAdtUniqueShouldSuggestThatGround() {
        MapSurfaceService svc = MapSurfaceService.of(
                (map, x, y) -> new float[]{56.1f},
                (map, x, y) -> new float[0]);
        FloorCandidates c = svc.candidateFloors(0, -9465f, 62f);
        assertEquals(FloorCandidates.Status.UNIQUE, c.status());
        assertEquals(1, c.floors().size());
        assertEquals(FloorCandidate.Source.ADT, c.floors().get(0).source());
        assertEquals(56.1f, c.suggestedZ(), 1e-4f);
    }

    @Test
    void candidateFloorsWhenDataUnavailableShouldBeUnresolved() {
        FloorCandidates c = MapSurfaceService.unavailable().candidateFloors(0, 0, 0);
        assertEquals(FloorCandidates.Status.UNRESOLVED, c.status());
        assertTrue(c.floors().isEmpty());
    }

    @Test
    void candidateFloorsWhenProvidersReturnNoFloorShouldBeUnresolved() {
        MapSurfaceService svc = MapSurfaceService.of(
                (map, x, y) -> new float[0],
                (map, x, y) -> new float[0]);
        FloorCandidates c = svc.candidateFloors(0, 1, 1);
        assertEquals(FloorCandidates.Status.UNRESOLVED, c.status());
    }

    @Test
    void candidateFloorsWhenAdtAndVmapSeparatedShouldBeAmbiguous() {
        MapSurfaceService svc = MapSurfaceService.of(
                (map, x, y) -> new float[]{50f},
                (map, x, y) -> new float[]{70f});
        FloorCandidates c = svc.candidateFloors(0, 0, 0);
        assertEquals(FloorCandidates.Status.AMBIGUOUS, c.status());
        assertEquals(2, c.floors().size());
        assertEquals(50f, c.suggestedZ(), 1e-4f);
    }

    @Test
    void manualZWhenNearCandidateShouldStayValidAndFarShouldBeStale() {
        MapSurfaceService svc = MapSurfaceService.of(
                (map, x, y) -> new float[]{50f},
                (map, x, y) -> new float[]{70f});
        FloorCandidates c = svc.candidateFloors(0, 0, 0);
        assertTrue(svc.manualZStillValid(50.4f, c));
        assertTrue(svc.manualZStillValid(70.1f, c));
        assertFalse(svc.manualZStillValid(90f, c));
        assertFalse(svc.manualZStillValid(50f, FloorCandidates.unresolved()));
        assertFalse(svc.manualZStillValid(Float.NaN, c));
    }
}
