package org.tbc.world.vmap;

/** BIH.h/.cpp read + intersectRay (stop at first hit, no M2 filter: height queries only). */
final class Bih {
    private static final int MAX_STACK_SIZE = 64;

    @FunctionalInterface
    interface RayCallback {
        /** True when the primitive was hit; shrinks maxDist[0] to the hit distance. */
        boolean hit(Ray ray, int entry, float[] maxDist);
    }

    private final float[] lo;
    private final float[] hi;
    private final int[] tree;
    private final int[] objects;

    Bih(float[] lo, float[] hi, int[] tree, int[] objects) {
        this.lo = lo;
        this.hi = hi;
        this.tree = tree;
        this.objects = objects;
    }

    static Bih empty() {
        return new Bih(new float[3], new float[3], new int[]{3 << 30, 0, 0}, new int[0]);
    }

    static Bih read(VMapReader r) {
        float[] lo = r.floats(3);
        float[] hi = r.floats(3);
        int[] tree = r.ints(r.count(4));
        if (tree.length < 3) {
            throw new IllegalArgumentException("bih tree too small");
        }
        int[] objects = r.ints(r.count(4));
        return new Bih(lo, hi, tree, objects);
    }

    int primCount() {
        return objects.length;
    }

    void intersectRay(Ray r, RayCallback cb, float[] maxDist) {
        float intervalMin = -1f;
        float intervalMax = -1f;
        float[] org = r.org();
        float[] dir = r.dir();
        float[] invDir = new float[3];
        for (int i = 0; i < 3; i++) {
            invDir[i] = 1f / dir[i];
            if (Math.abs(dir[i]) > 1e-5f) {
                float t1 = (lo[i] - org[i]) * invDir[i];
                float t2 = (hi[i] - org[i]) * invDir[i];
                if (t1 > t2) {
                    float t = t1;
                    t1 = t2;
                    t2 = t;
                }
                if (t1 > intervalMin) {
                    intervalMin = t1;
                }
                if (t2 < intervalMax || intervalMax < 0f) {
                    intervalMax = t2;
                }
                if (intervalMax <= 0 || intervalMin >= maxDist[0]) {
                    return;
                }
            }
        }
        if (intervalMin > intervalMax) {
            return;
        }
        intervalMin = Math.max(intervalMin, 0f);
        intervalMax = Math.min(intervalMax, maxDist[0]);

        int[] offsetFront = new int[3];
        int[] offsetBack = new int[3];
        int[] offsetFront3 = new int[3];
        int[] offsetBack3 = new int[3];
        for (int i = 0; i < 3; i++) {
            offsetFront[i] = Float.floatToRawIntBits(dir[i]) >>> 31;
            offsetBack[i] = offsetFront[i] ^ 1;
            offsetFront3[i] = offsetFront[i] * 3;
            offsetBack3[i] = offsetBack[i] * 3;
            offsetFront[i]++;
            offsetBack[i]++;
        }

        int[] stackNode = new int[MAX_STACK_SIZE];
        float[] stackNear = new float[MAX_STACK_SIZE];
        float[] stackFar = new float[MAX_STACK_SIZE];
        int stackPos = 0;
        int node = 0;

        while (true) {
            while (true) {
                int tn = tree[node];
                int axis = tn >>> 30;
                boolean bvh2 = (tn & (1 << 29)) != 0;
                int offset = tn & ~(7 << 29);
                if (!bvh2) {
                    if (axis < 3) {
                        float tf = (Float.intBitsToFloat(tree[node + offsetFront[axis]]) - org[axis]) * invDir[axis];
                        float tb = (Float.intBitsToFloat(tree[node + offsetBack[axis]]) - org[axis]) * invDir[axis];
                        if (tf < intervalMin && tb > intervalMax) {
                            break;
                        }
                        int back = offset + offsetBack3[axis];
                        node = back;
                        if (tf < intervalMin) {
                            intervalMin = tb >= intervalMin ? tb : intervalMin;
                            continue;
                        }
                        node = offset + offsetFront3[axis];
                        if (tb > intervalMax) {
                            intervalMax = tf <= intervalMax ? tf : intervalMax;
                            continue;
                        }
                        stackNode[stackPos] = back;
                        stackNear[stackPos] = tb >= intervalMin ? tb : intervalMin;
                        stackFar[stackPos] = intervalMax;
                        stackPos++;
                        intervalMax = tf <= intervalMax ? tf : intervalMax;
                    } else {
                        int n = tree[node + 1];
                        while (n > 0) {
                            if (cb.hit(r, objects[offset], maxDist)) {
                                return;
                            }
                            n--;
                            offset++;
                        }
                        break;
                    }
                } else {
                    if (axis > 2) {
                        return;
                    }
                    float tf = (Float.intBitsToFloat(tree[node + offsetFront[axis]]) - org[axis]) * invDir[axis];
                    float tb = (Float.intBitsToFloat(tree[node + offsetBack[axis]]) - org[axis]) * invDir[axis];
                    node = offset;
                    intervalMin = tf >= intervalMin ? tf : intervalMin;
                    intervalMax = tb <= intervalMax ? tb : intervalMax;
                    if (intervalMin > intervalMax) {
                        break;
                    }
                }
            }
            while (true) {
                if (stackPos == 0) {
                    return;
                }
                stackPos--;
                intervalMin = stackNear[stackPos];
                if (maxDist[0] < intervalMin) {
                    continue;
                }
                node = stackNode[stackPos];
                intervalMax = stackFar[stackPos];
                break;
            }
        }
    }
}
