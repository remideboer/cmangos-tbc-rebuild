package org.tbc.world.entity;

import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.ArrayList;
import java.util.List;

public class Unit extends Entity {
    public static final int TYPEID_UNIT = 3;
    public static final int TYPEID_PLAYER = 4;
    public static final int UNIT_FLAG_PLAYER_CONTROLLED = 0x8;
    public static final int UPDATEFLAG_SELF = 0x01;
    public static final int UPDATEFLAG_LOWGUID = 0x08;
    public static final int UPDATEFLAG_HIGHGUID = 0x10;
    public static final int UPDATEFLAG_LIVING = 0x20;
    public static final int UPDATEFLAG_HAS_POSITION = 0x40;
    public static final int PLAYER_CREATE_FLAGS = UPDATEFLAG_SELF | UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;

    public MovementInfo movement = new MovementInfo();
    public long victim;
    public boolean inCombat;
    public int level = 1;
    public String name = "";
    public int faction;
    public int entry;
    public String scriptName = "";
    public final List<Aura> auras = new ArrayList<>();
    public long lastMeleeMs;
    public int threat;

    public Unit(int valueCount, int typeId) {
        super(valueCount, typeId);
        updateFlags = UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;
        setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, 1.0f);
    }

    @Override
    public void relocate(float x, float y, float z, float o) {
        super.relocate(x, y, z, o);
        movement.x = x;
        movement.y = y;
        movement.z = z;
        movement.o = o;
    }

    public int health() {
        return getInt(UpdateFields.UNIT_FIELD_HEALTH);
    }

    public int maxHealth() {
        return getInt(UpdateFields.UNIT_FIELD_MAXHEALTH);
    }

    public void setHealth(int h) {
        setInt(UpdateFields.UNIT_FIELD_HEALTH, Math.max(0, Math.min(h, maxHealth() == 0 ? h : maxHealth())));
    }

    public int power() {
        return getInt(UpdateFields.UNIT_FIELD_POWER1);
    }

    public int maxPower() {
        return getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
    }

    public void setPower(int v) {
        setInt(UpdateFields.UNIT_FIELD_POWER1, Math.max(0, Math.min(v, maxPower() == 0 ? v : maxPower())));
    }

    public boolean alive() {
        return health() > 0;
    }

    public record Aura(int spellId, int durationMs, int stacks) {}
}
