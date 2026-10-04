package org.tbc.world.map;

/** One WorldMapArea.dbc row: named zone or continent with loc* bounds. */
public record WorldMapArea(int areaId, int mapId, String internalName, String displayName,
                           float locLeft, float locRight, float locTop, float locBottom) {
    @Override
    public String toString() {
        return displayName == null || displayName.isBlank() ? internalName : displayName;
    }

    public boolean degenerate() {
        return locTop == locBottom || locLeft == locRight;
    }
}
