package org.tbc.editor.quest;

import org.tbc.content.mpq.BlpImage;
import org.tbc.content.mpq.ClientMpq;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;
import org.tbc.world.map.WorldMapAreas;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Client world-map tiles: {@code Interface\WorldMap\<name>\<name>1.blp} … {@code 12},
 * four columns by three rows, the same layout the 8606 map frame uses.
 * Every WorldMapOverlay for the zone is stamped on that sheet. Sunstrider Isle
 * is the Eversong sheet cropped after those overlays, including SunstriderIsle.
 */
public final class WorldMapBlp {
    /** Highest-priority archive first. Locale files live under {@code Data/enUS}. */
    private static final String[] ARCHIVES = {
            "enUS/patch-enUS-3.MPQ",
            "enUS/patch-enUS-2.MPQ",
            "enUS/patch-enUS.MPQ",
            "patch-2.MPQ",
            "patch.MPQ",
            "enUS/expansion-locale-enUS.MPQ",
            "expansion.MPQ",
            "enUS/locale-enUS.MPQ",
            "enUS/base-enUS.MPQ",
            "common.MPQ"
    };

    /** WorldMapOverlay 1127 on the 1024×768 Eversong sheet, padded to the create point. */
    static final int SUNSTRIDER_OX = 195;
    static final int SUNSTRIDER_OY = 5;
    static final int CROP_X0 = 187;
    static final int CROP_Y0 = 0;
    static final int CROP_X1 = 715;
    static final int CROP_Y1 = 557;

    private WorldMapBlp() {}

    public static RegionMinimap.Raster load(Path dataDir, WorldMapArea area) {
        if (area == null) {
            return null;
        }
        if (area.areaId() == WorldMapAreas.SUNSTRIDER.areaId()) {
            return sunstrider(dataDir);
        }
        RegionMinimap.Raster sheet = assemble(dataDir, area.internalName(), area.internalName(), 0, 0);
        if (sheet != null) {
            paintOverlays(dataDir, sheet, area.internalName(), area.areaId());
        }
        return sheet;
    }

    /**
     * Eversong sheet cropped to the isle, with the Sunstrider overlay blitted on top.
     * Overlay pixels with alpha replace the sheet; the rest stays the zone map.
     */
    public static RegionMinimap.Raster sunstriderView(RegionMinimap.Raster parent, RegionMinimap.Raster overlay) {
        if (parent == null) {
            return overlay;
        }
        RegionMinimap.Raster crop = cropPixels(parent, CROP_X0, CROP_Y0, CROP_X1, CROP_Y1);
        if (overlay != null) {
            blitAlpha(crop, overlay, SUNSTRIDER_OX - CROP_X0, SUNSTRIDER_OY - CROP_Y0);
        }
        return crop;
    }

    public static void stamp(RegionMinimap.Raster sheet, RegionMinimap.Raster overlay, int x, int y) {
        if (sheet == null || overlay == null) {
            return;
        }
        blitAlpha(sheet, overlay, x, y);
    }

    public static RegionMinimap.Raster sunstriderCrop(RegionMinimap.Raster sheet) {
        if (sheet == null) {
            return null;
        }
        return cropPixels(sheet, CROP_X0, CROP_Y0, CROP_X1, CROP_Y1);
    }

    private static RegionMinimap.Raster sunstrider(Path dataDir) {
        String folder = WorldMapAreas.EVERSONG.internalName();
        RegionMinimap.Raster parent = assemble(dataDir, folder, folder, 4, 3);
        if (parent == null) {
            return null;
        }
        paintOverlays(dataDir, parent, folder, WorldMapAreas.EVERSONG.areaId());
        return sunstriderCrop(parent);
    }

    private static void paintOverlays(Path dataDir, RegionMinimap.Raster sheet, String folder, int zoneAreaId) {
        for (WorldMapOverlays.Overlay overlay : WorldMapOverlays.forZone(dataDir, zoneAreaId)) {
            int cols = (overlay.width() + 255) / 256;
            int rows = (overlay.height() + 255) / 256;
            if (cols <= 0 || rows <= 0) {
                continue;
            }
            RegionMinimap.Raster image = assemble(dataDir, folder, overlay.texture(), cols, rows);
            if (image == null) {
                continue;
            }
            int w = Math.min(overlay.width(), image.width());
            int h = Math.min(overlay.height(), image.height());
            RegionMinimap.Raster clipped = cropPixels(image, 0, 0, w, h);
            blitAlpha(sheet, clipped, overlay.offsetX(), overlay.offsetY());
        }
    }

    public static RegionMinimap.Raster crop(RegionMinimap.Raster full, WorldMapArea parent, WorldMapArea child) {
        if (full == null || parent == null || child == null || parent.degenerate()) {
            return full;
        }
        WorldMapAreaMapper mapper = new WorldMapAreaMapper(parent);
        float[] a = mapper.toPixel(child.locTop(), child.locLeft(), full.width(), full.height());
        float[] b = mapper.toPixel(child.locBottom(), child.locRight(), full.width(), full.height());
        int x0 = clamp((int) Math.floor(Math.min(a[0], b[0])), 0, full.width() - 1);
        int y0 = clamp((int) Math.floor(Math.min(a[1], b[1])), 0, full.height() - 1);
        int x1 = clamp((int) Math.ceil(Math.max(a[0], b[0])), x0 + 1, full.width());
        int y1 = clamp((int) Math.ceil(Math.max(a[1], b[1])), y0 + 1, full.height());
        int w = x1 - x0;
        int h = y1 - y0;
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(full.argb(), (y0 + y) * full.width() + x0, argb, y * w, w);
        }
        return new RegionMinimap.Raster(w, h, argb);
    }

    private static RegionMinimap.Raster cropPixels(RegionMinimap.Raster full, int x0, int y0, int x1, int y1) {
        x0 = clamp(x0, 0, Math.max(0, full.width() - 1));
        y0 = clamp(y0, 0, Math.max(0, full.height() - 1));
        x1 = clamp(x1, x0 + 1, full.width());
        y1 = clamp(y1, y0 + 1, full.height());
        int w = x1 - x0;
        int h = y1 - y0;
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(full.argb(), (y0 + y) * full.width() + x0, argb, y * w, w);
        }
        return new RegionMinimap.Raster(w, h, argb);
    }

    private static void blitAlpha(RegionMinimap.Raster dest, RegionMinimap.Raster src, int ox, int oy) {
        for (int y = 0; y < src.height(); y++) {
            int dy = oy + y;
            if (dy < 0 || dy >= dest.height()) {
                continue;
            }
            for (int x = 0; x < src.width(); x++) {
                int dx = ox + x;
                if (dx < 0 || dx >= dest.width()) {
                    continue;
                }
                int pixel = src.argb()[y * src.width() + x];
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                int at = dy * dest.width() + dx;
                if (alpha == 255) {
                    dest.argb()[at] = pixel;
                } else {
                    dest.argb()[at] = blend(dest.argb()[at], pixel, alpha);
                }
            }
        }
    }

    private static int blend(int dst, int src, int alpha) {
        int inv = 255 - alpha;
        int r = ((((src >> 16) & 0xFF) * alpha) + (((dst >> 16) & 0xFF) * inv)) / 255;
        int g = ((((src >> 8) & 0xFF) * alpha) + (((dst >> 8) & 0xFF) * inv)) / 255;
        int b = (((src & 0xFF) * alpha) + ((dst & 0xFF) * inv)) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static RegionMinimap.Raster assemble(Path dataDir, String folder, String prefix, int cols, int rows) {
        if (folder == null || folder.isBlank() || prefix == null || prefix.isBlank()) {
            return null;
        }
        BlpImage.Image[] tiles = new BlpImage.Image[12];
        int found = 0;
        int tileW = 0;
        int tileH = 0;
        int highest = 0;
        for (int n = 1; n <= 12; n++) {
            byte[] raw = readTile(dataDir, folder, prefix, n);
            BlpImage.Image image = raw == null ? null : BlpImage.decode(raw);
            if (image == null) {
                continue;
            }
            tiles[n - 1] = image;
            tileW = image.width();
            tileH = image.height();
            found++;
            highest = n;
        }
        if (found == 0 || tileW <= 0 || tileH <= 0) {
            return null;
        }
        if (cols <= 0 || rows <= 0) {
            if (highest <= 1) {
                cols = 1;
                rows = 1;
            } else if (highest <= 4) {
                cols = highest;
                rows = 1;
            } else {
                cols = 4;
                rows = 3;
            }
        }
        int[] argb = new int[cols * tileW * rows * tileH];
        for (int i = 0; i < tiles.length; i++) {
            BlpImage.Image tile = tiles[i];
            if (tile == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            if (row >= rows) {
                continue;
            }
            blit(argb, cols * tileW, tile, col * tileW, row * tileH);
        }
        return new RegionMinimap.Raster(cols * tileW, rows * tileH, argb);
    }

    private static void blit(int[] dest, int destW, BlpImage.Image tile, int ox, int oy) {
        for (int y = 0; y < tile.height(); y++) {
            System.arraycopy(tile.argb(), y * tile.width(), dest, (oy + y) * destW + ox, tile.width());
        }
    }

    private static byte[] readTile(Path dataDir, String folder, String prefix, int index) {
        return readNamed(dataDir, "Interface\\WorldMap\\" + folder + "\\" + prefix + index + ".blp");
    }

    static byte[] readNamed(Path dataDir, String relative) {
        for (Path root : roots(dataDir)) {
            Path loose = root.resolve(relative.replace('\\', java.io.File.separatorChar));
            if (Files.isRegularFile(loose)) {
                try {
                    return Files.readAllBytes(loose);
                } catch (Exception ignored) {
                    return null;
                }
            }
            for (String archive : ARCHIVES) {
                Path mpq = root.resolve(archive);
                if (!Files.isRegularFile(mpq)) {
                    continue;
                }
                try {
                    return ClientMpq.read(mpq, relative);
                } catch (Exception ignored) {
                    // next archive
                }
            }
        }
        return null;
    }

    static List<Path> roots(Path dataDir) {
        List<Path> roots = new ArrayList<>();
        add(roots, dataDir);
        if (dataDir != null) {
            add(roots, dataDir.resolve("Data"));
            add(roots, dataDir.getParent());
        }
        Path cwd = Path.of(System.getProperty("user.dir", "."));
        add(roots, cwd.resolve("..").resolve("WoW-2.4.3-client").resolve("Data").normalize());
        add(roots, cwd.resolve("WoW-2.4.3-client").resolve("Data"));
        return roots;
    }

    private static void add(List<Path> roots, Path path) {
        if (path == null) {
            return;
        }
        Path normal = path.normalize();
        if (!roots.contains(normal)) {
            roots.add(normal);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
