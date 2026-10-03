package org.tbc.world.vmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** WorldModel.cpp readFile (.vmo): group meshes + group BIH. */
final class WorldModel {
    private final List<GroupModel> groups;
    private final Bih groupTree;

    WorldModel(List<GroupModel> groups, Bih groupTree) {
        this.groups = groups;
        this.groupTree = groupTree;
    }

    /** Null when the file is missing or not a VMAP_7.0 model. */
    static WorldModel read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            VMapReader r = VMapReader.open(file);
            if (!r.chunk(VMapManager.MAGIC) || !r.chunk("WMOD")) {
                return null;
            }
            r.u32();
            r.u32();
            List<GroupModel> groups = new ArrayList<>();
            Bih groupTree = Bih.empty();
            if (r.chunk("GMOD")) {
                int count = r.count(1);
                for (int i = 0; i < count; i++) {
                    GroupModel g = GroupModel.read(r);
                    if (g == null) {
                        return null;
                    }
                    groups.add(g);
                }
                if (!r.chunk("GBIH")) {
                    return null;
                }
                groupTree = Bih.read(r);
            }
            return new WorldModel(groups, groupTree);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    boolean intersectRay(Ray ray, float[] distance) {
        if (groups.size() == 1) {
            return groups.get(0).intersectRay(ray, distance);
        }
        boolean[] hit = {false};
        groupTree.intersectRay(ray, (rr, entry, d) -> {
            if (groups.get(entry).intersectRay(rr, d)) {
                hit[0] = true;
            }
            return hit[0];
        }, distance);
        return hit[0];
    }
}
