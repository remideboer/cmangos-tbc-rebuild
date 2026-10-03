package org.tbc.world.vmap;

/** ModelInstance.cpp: placed WorldModel; ray is moved into model space before the BIH walk. */
final class ModelInstance {
    private static final int MOD_HAS_BOUND = 1 << 2;
    private static final int MAX_NAME = 500;

    /** ModelSpawn::readFromFile; null on EOF, truncation or oversized name. */
    record Spawn(int flags, int adtId, int id, float[] pos, float[] rot, float scale, float[] lo, float[] hi,
            String name) {
        static Spawn read(VMapReader r) {
            if (!r.hasRemaining()) {
                return null;
            }
            try {
                int flags = r.u32();
                int adtId = r.u16();
                int id = r.u32();
                float[] pos = r.floats(3);
                float[] rot = r.floats(3);
                float scale = r.f32();
                float[] lo = new float[3];
                float[] hi = new float[3];
                if ((flags & MOD_HAS_BOUND) != 0) {
                    lo = r.floats(3);
                    hi = r.floats(3);
                }
                int nameLen = r.u32();
                if (nameLen < 0 || nameLen > MAX_NAME) {
                    return null;
                }
                return new Spawn(flags, adtId, id, pos, rot, scale, lo, hi, r.ascii(nameLen));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private final Spawn spawn;
    private final WorldModel model;
    private final float[] invRot;
    private final float invScale;

    ModelInstance(Spawn spawn, WorldModel model) {
        this.spawn = spawn;
        this.model = model;
        float pi = (float) Math.PI;
        this.invRot = transpose(eulerZyx(pi * spawn.rot()[1] / 180f, pi * spawn.rot()[0] / 180f,
                pi * spawn.rot()[2] / 180f));
        this.invScale = 1f / spawn.scale();
    }

    boolean intersectRay(Ray ray, float[] maxDist) {
        if (model == null || !ray.hitsBox(spawn.lo(), spawn.hi())) {
            return false;
        }
        float[] o = ray.org();
        float[] p = rotate(invRot, o[0] - spawn.pos()[0], o[1] - spawn.pos()[1], o[2] - spawn.pos()[2]);
        float[] d = ray.dir();
        float[] md = rotate(invRot, d[0], d[1], d[2]);
        Ray modelRay = new Ray(new float[]{p[0] * invScale, p[1] * invScale, p[2] * invScale}, md);
        float[] distance = {maxDist[0] * invScale};
        if (!model.intersectRay(modelRay, distance)) {
            return false;
        }
        maxDist[0] = distance[0] * spawn.scale();
        return true;
    }

    /** G3D Matrix3::fromEulerAnglesZYX, row-major. */
    static float[] eulerZyx(float yaw, float pitch, float roll) {
        float c = (float) Math.cos(yaw);
        float s = (float) Math.sin(yaw);
        float[] z = {c, -s, 0f, s, c, 0f, 0f, 0f, 1f};
        c = (float) Math.cos(pitch);
        s = (float) Math.sin(pitch);
        float[] y = {c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
        c = (float) Math.cos(roll);
        s = (float) Math.sin(roll);
        float[] x = {1f, 0f, 0f, 0f, c, -s, 0f, s, c};
        return mul(z, mul(y, x));
    }

    private static float[] mul(float[] a, float[] b) {
        float[] out = new float[9];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
            }
        }
        return out;
    }

    private static float[] transpose(float[] m) {
        return new float[]{m[0], m[3], m[6], m[1], m[4], m[7], m[2], m[5], m[8]};
    }

    private static float[] rotate(float[] m, float x, float y, float z) {
        return new float[]{
                m[0] * x + m[1] * y + m[2] * z,
                m[3] * x + m[4] * y + m[5] * z,
                m[6] * x + m[7] * y + m[8] * z};
    }
}
