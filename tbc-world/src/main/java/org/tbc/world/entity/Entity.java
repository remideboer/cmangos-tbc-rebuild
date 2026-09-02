package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;

public class Entity {
    public long guid;
    public int mapId;
    public int zoneId;
    public int areaId;
    public float x, y, z, o;
    public final int[] values;
    public final int typeId;
    public int updateFlags;

    public Entity(int valueCount, int typeId) {
        this.values = new int[valueCount];
        this.typeId = typeId;
    }

    public void setInt(int field, int v) {
        values[field] = v;
    }

    public void setFloat(int field, float v) {
        values[field] = Float.floatToIntBits(v);
    }

    public void setGuid(int field, long g) {
        values[field] = (int) g;
        values[field + 1] = (int) (g >>> 32);
    }

    public int getInt(int field) {
        return values[field];
    }

    public float getFloat(int field) {
        return Float.intBitsToFloat(values[field]);
    }

    public void relocate(float x, float y, float z, float o) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.o = o;
    }

    public double distance2d(Entity o) {
        double dx = x - o.x;
        double dy = y - o.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean visibleToOwner(int field, boolean owner) {
        int vis = UpdateFields.visibility(field);
        if ((vis & UpdateFields.PUBLIC) != 0 || (vis & UpdateFields.DYNAMIC) != 0) {
            return true;
        }
        if (owner && ((vis & UpdateFields.PRIVATE) != 0 || (vis & UpdateFields.OWNER_ONLY) != 0)) {
            return true;
        }
        return false;
    }
}
