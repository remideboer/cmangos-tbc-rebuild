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

    public long getGuid(int field) {
        return (values[field] & 0xFFFFFFFFL) | ((long) values[field + 1] << 32);
    }

    public float getFloat(int field) {
        return Float.intBitsToFloat(values[field]);
    }

    /**
     * CMaNGOS Object::ApplyPercentModFloatValue — multiply by (100+val)/100 on apply,
     * by 100/(100+val) on remove. val == -100 clamped to -99.9.
     */
    public void applyPercentModFloatValue(int field, float val, boolean apply) {
        float v = val == -100.0f ? -99.9f : val;
        float cur = getFloat(field);
        setFloat(field, cur * (apply ? (100.0f + v) / 100.0f : 100.0f / (100.0f + v)));
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
        if (owner && ((vis & UpdateFields.PRIVATE) != 0 || (vis & UpdateFields.OWNER_ONLY) != 0
                || (vis & UpdateFields.GROUP_ONLY) != 0)) {
            return true;
        }
        return false;
    }
}
