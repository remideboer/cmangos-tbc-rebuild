package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayCalibrationTest {
    @Test
    void fromAnchorsWhenTwoNonCoincidentShouldRoundTripWorldThroughPixel() {
        OverlayCalibration cal = OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(10, 20, -9465.5, 62.25),
                new OverlayCalibration.Anchor(210, 20, -9265.5, 62.25)));
        double[] pixel = cal.toPixel(-9365.5, 62.25);
        double[] world = cal.toWorld(pixel[0], pixel[1]);
        assertEquals(-9365.5, world[0], 1e-6);
        assertEquals(62.25, world[1], 1e-6);
        assertEquals(0, cal.residual(), 1e-9);
    }

    @Test
    void fromAnchorsWhenThirdAnchorShouldReportResidual() {
        OverlayCalibration cal = OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(0, 0, 0, 0),
                new OverlayCalibration.Anchor(100, 0, 100, 0),
                new OverlayCalibration.Anchor(0, 100, 0, 102)));
        assertTrue(cal.residual() > 0.5);
        assertTrue(cal.residual() < 4);
    }

    @Test
    void fromAnchorsWhenCoincidentOrDegenerateShouldReject() {
        assertThrows(IllegalArgumentException.class, () -> OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(1, 1, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(5, 5, 0, 0),
                new OverlayCalibration.Anchor(5, 5, 10, 10))));
        assertThrows(IllegalArgumentException.class, () -> OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(0, 0, 1, 1),
                new OverlayCalibration.Anchor(10, 10, 1, 1))));
        assertThrows(IllegalArgumentException.class, () -> OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(0, 0, 0, 0),
                new OverlayCalibration.Anchor(1e-9, 0, 1e8, 0))));
    }

    @Test
    void worldOrientationFromPixelDeltaShouldUseCalibrationRotation() {
        OverlayCalibration cal = OverlayCalibration.fromAnchors(List.of(
                new OverlayCalibration.Anchor(0, 0, 0, 0),
                new OverlayCalibration.Anchor(10, 0, 0, 10)));
        double o = cal.worldOrientationFromPixelDelta(10, 0);
        assertEquals(Math.atan2(10, 0), o, 1e-6);
    }
}
