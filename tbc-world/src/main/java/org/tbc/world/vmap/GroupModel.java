package org.tbc.world.vmap;

/** WorldModel.cpp GroupModel: collision mesh + MBIH. Liquid chunk is skipped (height only). */
final class GroupModel {
    private static final float EPS = 1e-5f;

    private final float[] vertices;
    private final int[] triangles;
    private final Bih meshTree;

    GroupModel(float[] vertices, int[] triangles, Bih meshTree) {
        this.vertices = vertices;
        this.triangles = triangles;
        this.meshTree = meshTree;
    }

    /** Null on malformed chunk order. */
    static GroupModel read(VMapReader r) {
        r.skip(24);
        r.u32();
        r.u32();
        if (!r.chunk("VERT")) {
            return null;
        }
        r.u32();
        int vertCount = r.count(12);
        if (vertCount == 0) {
            return new GroupModel(new float[0], new int[0], Bih.empty());
        }
        float[] vertices = r.floats(vertCount * 3);
        if (!r.chunk("TRIM")) {
            return null;
        }
        r.u32();
        int triCount = r.count(12);
        int[] triangles = r.ints(triCount * 3);
        for (int idx : triangles) {
            if (idx < 0 || idx >= vertCount) {
                return null;
            }
        }
        if (!r.chunk("MBIH")) {
            return null;
        }
        Bih meshTree = Bih.read(r);
        if (!r.chunk("LIQU")) {
            return null;
        }
        int liquidSize = r.u32();
        if (liquidSize > 0) {
            r.skip(liquidSize);
        }
        return new GroupModel(vertices, triangles, meshTree);
    }

    boolean intersectRay(Ray ray, float[] distance) {
        if (triangles.length == 0) {
            return false;
        }
        boolean[] hit = {false};
        meshTree.intersectRay(ray, (rr, entry, d) -> {
            if (intersectTriangle(entry, rr, d)) {
                hit[0] = true;
            }
            return hit[0];
        }, distance);
        return hit[0];
    }

    /** WorldModel.cpp IntersectTriangle (RTR2 13.7). */
    boolean intersectTriangle(int tri, Ray ray, float[] distance) {
        int i0 = triangles[tri * 3] * 3;
        int i1 = triangles[tri * 3 + 1] * 3;
        int i2 = triangles[tri * 3 + 2] * 3;
        float e1x = vertices[i1] - vertices[i0];
        float e1y = vertices[i1 + 1] - vertices[i0 + 1];
        float e1z = vertices[i1 + 2] - vertices[i0 + 2];
        float e2x = vertices[i2] - vertices[i0];
        float e2y = vertices[i2 + 1] - vertices[i0 + 1];
        float e2z = vertices[i2 + 2] - vertices[i0 + 2];
        float[] d = ray.dir();
        float px = d[1] * e2z - d[2] * e2y;
        float py = d[2] * e2x - d[0] * e2z;
        float pz = d[0] * e2y - d[1] * e2x;
        float a = e1x * px + e1y * py + e1z * pz;
        if (Math.abs(a) < EPS) {
            return false;
        }
        float f = 1f / a;
        float sx = ray.org()[0] - vertices[i0];
        float sy = ray.org()[1] - vertices[i0 + 1];
        float sz = ray.org()[2] - vertices[i0 + 2];
        float u = f * (sx * px + sy * py + sz * pz);
        if (u < 0f || u > 1f) {
            return false;
        }
        float qx = sy * e1z - sz * e1y;
        float qy = sz * e1x - sx * e1z;
        float qz = sx * e1y - sy * e1x;
        float v = f * (d[0] * qx + d[1] * qy + d[2] * qz);
        if (v < 0f || u + v > 1f) {
            return false;
        }
        float t = f * (e2x * qx + e2y * qy + e2z * qz);
        if (t > 0f && t < distance[0]) {
            distance[0] = t;
            return true;
        }
        return false;
    }
}
