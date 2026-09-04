package org.tbc.world.ai;

import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.TaxiHandler;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/** Straight-line chase and OOC random. spec/05-domain/movement-and-pathfinding.md PathFinder fallback. */
public final class MotionMaster {
    public static final int IDLE = 0;
    /** DB `creature.MovementType` random. */
    public static final int RANDOM = 1;
    /** Combat chase — not a DB spawn type (waypoint is 2). */
    public static final int CHASE = 3;

    private int type = IDLE;
    private Unit target;
    private float wanderRadius;
    private float destX;
    private float destY;
    private float destZ;
    private boolean hasDest;
    private DoubleSupplier rng = ThreadLocalRandom.current()::nextDouble;

    public int type() {
        return type;
    }

    public void rng(DoubleSupplier rng) {
        this.rng = rng == null ? ThreadLocalRandom.current()::nextDouble : rng;
    }

    public void moveChase(Unit victim) {
        type = CHASE;
        target = victim;
        hasDest = false;
    }

    public void moveRandom(float spawnDist) {
        type = RANDOM;
        target = null;
        wanderRadius = spawnDist;
        hasDest = false;
    }

    public void moveIdle() {
        type = IDLE;
        target = null;
        hasDest = false;
    }

    public byte[] update(Creature c, int diffMs) {
        if (c == null || diffMs <= 0) {
            return null;
        }
        if (type == RANDOM) {
            return updateRandom(c, diffMs);
        }
        if (type != CHASE || target == null) {
            return null;
        }
        double dist = c.distance2d(target);
        float stop = Combat.meleeRange(c);
        if (dist <= stop) {
            return null;
        }
        float step = UpdateBuilder.RUN * (diffMs / 1000f);
        double nx = target.x - c.x;
        double ny = target.y - c.y;
        double len = Math.hypot(nx, ny);
        if (len < 1e-6) {
            return null;
        }
        double close = dist - stop;
        if (step > close) {
            step = (float) close;
        }
        c.relocate(c.x + (float) (nx / len * step), c.y + (float) (ny / len * step), c.z, c.o);
        return monsterMove(c, target.x, target.y, target.z, UpdateBuilder.RUN);
    }

    private byte[] updateRandom(Creature c, int diffMs) {
        if (wanderRadius <= 0) {
            return null;
        }
        if (!hasDest || arrived(c)) {
            pickDest(c);
        }
        double dx = destX - c.x;
        double dy = destY - c.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-3) {
            return null;
        }
        float step = UpdateBuilder.WALK * (diffMs / 1000f);
        if (step > len) {
            step = (float) len;
        }
        byte[] spline = monsterMove(c, destX, destY, destZ, UpdateBuilder.WALK);
        c.relocate(c.x + (float) (dx / len * step), c.y + (float) (dy / len * step), c.z, c.o);
        return spline;
    }

    private boolean arrived(Creature c) {
        return Math.hypot(destX - c.x, destY - c.y) < 0.2;
    }

    private void pickDest(Creature c) {
        double angle = rng.getAsDouble() * Math.PI * 2;
        double radius = rng.getAsDouble() * wanderRadius;
        destX = c.spawnX + (float) (Math.cos(angle) * radius);
        destY = c.spawnY + (float) (Math.sin(angle) * radius);
        destZ = c.spawnZ;
        hasDest = true;
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ) {
        return monsterMove(c, destX, destY, destZ, UpdateBuilder.RUN);
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ, float speed) {
        float dx = destX - c.x;
        float dy = destY - c.y;
        float dz = destZ - c.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        int duration = Math.max(1, (int) (dist / speed * 1000f));
        WowBuffer b = new WowBuffer(64);
        b.putPackedGuid(c.guid);
        b.putFloat(c.x);
        b.putFloat(c.y);
        b.putFloat(c.z);
        b.putU32(1);
        b.putU8(TaxiHandler.MONSTER_MOVE_NORMAL);
        b.putU32(TaxiHandler.SPLINE_FLAG_RUNMODE);
        b.putU32(duration);
        b.putU32(0);
        b.putFloat(destX);
        b.putFloat(destY);
        b.putFloat(destZ);
        return b.array();
    }
}
