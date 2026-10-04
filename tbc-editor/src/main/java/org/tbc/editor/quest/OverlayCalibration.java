package org.tbc.editor.quest;

import java.util.List;

/** Similarity transform from overlay pixels to world XY. */
public final class OverlayCalibration {
    public static final double PIXEL_EPS = 1e-6;
    public static final double WORLD_EPS = 1e-4;
    public static final double MIN_SCALE = 1e-3;
    public static final double MAX_SCALE = 1e4;

    public record Anchor(double pixelX, double pixelY, double worldX, double worldY) {}

    private final double scale;
    private final double cos;
    private final double sin;
    private final double tx;
    private final double ty;
    private final double residual;

    private OverlayCalibration(double scale, double cos, double sin, double tx, double ty, double residual) {
        this.scale = scale;
        this.cos = cos;
        this.sin = sin;
        this.tx = tx;
        this.ty = ty;
        this.residual = residual;
    }

    public static OverlayCalibration fromAnchors(List<Anchor> anchors) {
        if (anchors == null || anchors.size() < 2) {
            throw new IllegalArgumentException("Calibration needs at least two anchors.");
        }
        Anchor a = anchors.get(0);
        Anchor b = anchors.get(1);
        double pdx = b.pixelX() - a.pixelX();
        double pdy = b.pixelY() - a.pixelY();
        double wdx = b.worldX() - a.worldX();
        double wdy = b.worldY() - a.worldY();
        double plen = Math.hypot(pdx, pdy);
        double wlen = Math.hypot(wdx, wdy);
        if (plen < PIXEL_EPS) {
            throw new IllegalArgumentException("Calibration anchors are coincident in pixel space.");
        }
        if (wlen < WORLD_EPS) {
            throw new IllegalArgumentException("Calibration anchors are coincident in world space.");
        }
        double scale = wlen / plen;
        if (!(scale >= MIN_SCALE && scale <= MAX_SCALE)) {
            throw new IllegalArgumentException("Calibration scale is implausible.");
        }
        double pang = Math.atan2(pdy, pdx);
        double wang = Math.atan2(wdy, wdx);
        double rot = wang - pang;
        double cos = Math.cos(rot);
        double sin = Math.sin(rot);
        double tx = a.worldX() - scale * (cos * a.pixelX() - sin * a.pixelY());
        double ty = a.worldY() - scale * (sin * a.pixelX() + cos * a.pixelY());
        OverlayCalibration cal = new OverlayCalibration(scale, cos, sin, tx, ty, 0);
        double maxErr = 0;
        for (int i = 2; i < anchors.size(); i++) {
            Anchor c = anchors.get(i);
            double[] w = cal.toWorld(c.pixelX(), c.pixelY());
            maxErr = Math.max(maxErr, Math.hypot(w[0] - c.worldX(), w[1] - c.worldY()));
        }
        return new OverlayCalibration(scale, cos, sin, tx, ty, maxErr);
    }

    public double residual() {
        return residual;
    }

    public double[] toWorld(double pixelX, double pixelY) {
        return new double[]{
                scale * (cos * pixelX - sin * pixelY) + tx,
                scale * (sin * pixelX + cos * pixelY) + ty
        };
    }

    public double[] toPixel(double worldX, double worldY) {
        double dx = worldX - tx;
        double dy = worldY - ty;
        double inv = 1.0 / scale;
        return new double[]{
                inv * (cos * dx + sin * dy),
                inv * (-sin * dx + cos * dy)
        };
    }

    public double worldOrientationFromPixelDelta(double dpx, double dpy) {
        double[] a = toWorld(0, 0);
        double[] b = toWorld(dpx, dpy);
        return Math.atan2(b[1] - a[1], b[0] - a[0]);
    }
}
