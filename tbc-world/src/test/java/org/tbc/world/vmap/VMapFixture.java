package org.tbc.world.vmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes tiny VMAP_7.0 files in the layout CMaNGOS reads (VMapManager2 / MapTree / WorldModel). */
final class VMapFixture {
    static final int HAS_BOUND = 4;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    static VMapFixture create() {
        return new VMapFixture();
    }

    VMapFixture raw(String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
        return this;
    }

    VMapFixture u8(int v) {
        out.write(v);
        return this;
    }

    VMapFixture u16(int v) {
        out.writeBytes(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) v).array());
        return this;
    }

    VMapFixture u32(int v) {
        out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array());
        return this;
    }

    VMapFixture f32(float... v) {
        for (float f : v) {
            u32(Float.floatToRawIntBits(f));
        }
        return this;
    }

    VMapFixture bih(float[] lo, float[] hi, int[] tree, int[] objects) {
        f32(lo).f32(hi).u32(tree.length);
        for (int t : tree) {
            u32(t);
        }
        u32(objects.length);
        for (int o : objects) {
            u32(o);
        }
        return this;
    }

    /** One leaf holding objects 0..n-1. */
    VMapFixture leafBih(float[] lo, float[] hi, int n) {
        int[] objects = new int[n];
        for (int i = 0; i < n; i++) {
            objects[i] = i;
        }
        return bih(lo, hi, new int[]{3 << 30, n, 0}, objects);
    }

    VMapFixture spawn(int id, float[] pos, float scale, float[] lo, float[] hi, String name) {
        return u32(HAS_BOUND).u16(0).u32(id).f32(pos).f32(0f, 0f, 0f).f32(scale).f32(lo).f32(hi)
                .u32(name.length()).raw(name);
    }

    byte[] bytes() {
        return out.toByteArray();
    }

    void write(Path file) throws IOException {
        Files.write(file, bytes());
    }

    /** .vmo with one group holding the triangles (vertex xyz triples, index triples). */
    static byte[] model(float[] verts, int[] tris) {
        float[] lo = {-1e6f, -1e6f, -1e6f};
        float[] hi = {1e6f, 1e6f, 1e6f};
        VMapFixture f = create().raw("VMAP_7.0").raw("WMOD").u32(8).u32(1).raw("GMOD").u32(1)
                .f32(lo).f32(hi).u32(0).u32(1).raw("VERT").u32(4 + verts.length * 4).u32(verts.length / 3).f32(verts)
                .raw("TRIM").u32(4 + tris.length * 4).u32(tris.length / 3);
        for (int t : tris) {
            f.u32(t);
        }
        return f.raw("MBIH").leafBih(lo, hi, tris.length / 3).raw("LIQU").u32(0)
                .raw("GBIH").leafBih(lo, hi, 1).bytes();
    }

    /** .vmtree: NODE BIH over {@code prims} instances, GOBJ block follows. */
    static VMapFixture tree(boolean tiled, int prims) {
        float[] lo = {-1e6f, -1e6f, -1e6f};
        float[] hi = {1e6f, 1e6f, 1e6f};
        return create().raw("VMAP_7.0").u8(tiled ? 1 : 0).raw("NODE").leafBih(lo, hi, prims).raw("GOBJ");
    }
}
