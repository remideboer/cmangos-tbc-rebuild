package org.tbc.editor.quest;

/** Sixteen compass ticks for creature.orientation (radians, 0 = +X / north on the minimap). */
public final class NpcFacing {
    public static final int TICKS = 16;
    public static final float STEP = (float) (Math.PI / 8);

    private NpcFacing() {}

    public static float wrap(float o) {
        float tau = TICKS * STEP;
        float r = o % tau;
        if (r < 0f) {
            r += tau;
        }
        if (r >= tau) {
            r = 0f;
        }
        return r;
    }

    public static int tick(float o) {
        int t = Math.round(wrap(o) / STEP) % TICKS;
        return t < 0 ? t + TICKS : t;
    }

    public static float snap(float o) {
        return tick(o) * STEP;
    }

    /** Clockwise one tick (Up). */
    public static float right(float o) {
        return wrap(snap(o) - STEP);
    }

    /** Counterclockwise one tick (Down). */
    public static float left(float o) {
        return wrap(snap(o) + STEP);
    }

    /** Pixel direction: world (+X, +Y) facing maps to screen (up, left). */
    public static float[] screenDir(float o) {
        return new float[] {(float) -Math.sin(o), (float) -Math.cos(o)};
    }
}
