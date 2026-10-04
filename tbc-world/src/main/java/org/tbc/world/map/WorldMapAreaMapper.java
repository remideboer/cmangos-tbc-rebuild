package org.tbc.world.map;

/** Axis-aligned WorldMapArea loc* ↔ image pixels (north-up, origin top-left). */
public final class WorldMapAreaMapper {
    private final WorldMapArea area;

    public WorldMapAreaMapper(WorldMapArea area) {
        this.area = area;
    }

    public boolean contains(float worldX, float worldY) {
        return worldX >= area.locBottom() && worldX <= area.locTop()
                && worldY >= area.locRight() && worldY <= area.locLeft();
    }

    public float[] toWorld(float pixelX, float pixelY, float imgW, float imgH) {
        if (imgW <= 0 || imgH <= 0) {
            return new float[]{area.locTop(), area.locLeft()};
        }
        float worldX = area.locTop() + (pixelX / imgW) * (area.locBottom() - area.locTop());
        float worldY = area.locLeft() + (pixelY / imgH) * (area.locRight() - area.locLeft());
        return new float[]{worldX, worldY};
    }

    public float[] toPixel(float worldX, float worldY, float imgW, float imgH) {
        if (imgW <= 0 || imgH <= 0 || area.degenerate()) {
            return new float[]{0, 0};
        }
        float px = (worldX - area.locTop()) / (area.locBottom() - area.locTop()) * imgW;
        float py = (worldY - area.locLeft()) / (area.locRight() - area.locLeft()) * imgH;
        return new float[]{px, py};
    }
}
