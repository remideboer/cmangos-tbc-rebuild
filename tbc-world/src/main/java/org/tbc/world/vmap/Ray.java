package org.tbc.world.vmap;

/** G3D::Ray: origin + unit direction. */
record Ray(float[] org, float[] dir) {
    static Ray of(float ox, float oy, float oz, float dx, float dy, float dz) {
        return new Ray(new float[]{ox, oy, oz}, new float[]{dx, dy, dz});
    }

    /** G3D Ray::intersectionTime(AABox) != inf (VMapTools collisionLocationForMovingPointFixedAABox). */
    boolean hitsBox(float[] lo, float[] hi) {
        boolean inside = true;
        float[] maxT = {-1f, -1f, -1f};
        float[] loc = new float[3];
        for (int i = 0; i < 3; i++) {
            if (org[i] < lo[i]) {
                loc[i] = lo[i];
                inside = false;
                if (Float.floatToRawIntBits(dir[i]) != 0) {
                    maxT[i] = (lo[i] - org[i]) / dir[i];
                }
            } else if (org[i] > hi[i]) {
                loc[i] = hi[i];
                inside = false;
                if (Float.floatToRawIntBits(dir[i]) != 0) {
                    maxT[i] = (hi[i] - org[i]) / dir[i];
                }
            }
        }
        if (inside) {
            return true;
        }
        int plane = 0;
        if (maxT[1] > maxT[plane]) {
            plane = 1;
        }
        if (maxT[2] > maxT[plane]) {
            plane = 2;
        }
        if (Float.floatToRawIntBits(maxT[plane]) < 0) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (i != plane) {
                loc[i] = org[i] + maxT[plane] * dir[i];
                if (loc[i] < lo[i] || loc[i] > hi[i]) {
                    return false;
                }
            }
        }
        return true;
    }
}
