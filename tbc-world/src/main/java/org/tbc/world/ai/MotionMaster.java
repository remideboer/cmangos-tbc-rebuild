package org.tbc.world.ai;

import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.Terrain;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.TaxiHandler;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Straight-line chase and OOC random.
 * CMaNGOS: RandomMovementGenerator one spline until Finalized;
 * packet_builder WriteLinearPath last_idx = pointCount-3;
 * ChaseMovementGenerator GetNearPoint + SetInFront.
 */
public final class MotionMaster {
    public static final int IDLE = 0;
    /** DB `creature.MovementType` random. */
    public static final int RANDOM = 1;
    /** Combat chase — not a DB spawn type (waypoint is 2). */
    public static final int CHASE = 3;
    /** CMaNGOS HOME_MOTION_TYPE / MoveTargetedHome. */
    public static final int HOME = 4;
    /** Creature chase/face poll. CMaNGOS i_recheckDistance 250ms; animals ~150ms. */
    public static final int CHASE_REACTION_MS = 150;
    /** Any player move counts; CMaNGOS currentTargetPos != i_lastTargetPos. */
    private static final float TARGET_MOVE_YARDS = 0.01f;
    /** CMaNGOS HasInArc(0.01) on finalize. */
    private static final float FACE_RESEND_RAD = 0.01f;

    private int type = IDLE;
    private Unit target;
    private float wanderRadius;
    private float destX;
    private float destY;
    private float destZ;
    private float lastTargetX;
    private float lastTargetY;
    private boolean hasDest;
    private boolean splineSent;
    private boolean faceSent;
    private boolean sentPacket;
    private int sincePacketMs;
    private int nextSplineId = 1;
    private float lastFaceO;
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
        splineSent = false;
        faceSent = false;
        sentPacket = false;
        sincePacketMs = CHASE_REACTION_MS;
    }

    public void moveRandom(float spawnDist) {
        type = RANDOM;
        target = null;
        wanderRadius = spawnDist;
        hasDest = false;
        splineSent = false;
        faceSent = false;
        sentPacket = false;
        sincePacketMs = CHASE_REACTION_MS;
    }

    public void moveIdle() {
        type = IDLE;
        target = null;
        hasDest = false;
        splineSent = false;
        faceSent = false;
        sentPacket = false;
        sincePacketMs = CHASE_REACTION_MS;
    }

    public void moveHome() {
        type = HOME;
        target = null;
        hasDest = false;
        splineSent = false;
        faceSent = false;
        sentPacket = false;
        sincePacketMs = CHASE_REACTION_MS;
    }

    public byte[] update(Creature c, int diffMs) {
        return update(c, diffMs, Terrain.NONE);
    }

    public byte[] update(Creature c, int diffMs, Terrain.Height ground) {
        if (c == null || diffMs <= 0) {
            return null;
        }
        Terrain.Height g = ground == null ? Terrain.NONE : ground;
        if (type == RANDOM) {
            return updateRandom(c, diffMs, g);
        }
        if (type == HOME) {
            return updateHome(c, diffMs, g);
        }
        if (type != CHASE || target == null) {
            return null;
        }
        return updateChase(c, diffMs, g);
    }

    private byte[] updateChase(Creature c, int diffMs, Terrain.Height g) {
        sincePacketMs += diffMs;
        double dist = c.distance2d(target);
        float stop = Combat.meleeRange(c, target);
        if (dist <= stop) {
            hasDest = false;
            splineSent = false;
            return faceOnWire(c, target);
        }
        double nx = target.x - c.x;
        double ny = target.y - c.y;
        double len = Math.hypot(nx, ny);
        if (len < 1e-6) {
            return null;
        }
        float mx = target.x - (float) (nx / len * stop);
        float my = target.y - (float) (ny / len * stop);
        float mz = g.at(c.mapId, mx, my, target.z);
        boolean targetMoved = hasDest && Math.hypot(target.x - lastTargetX, target.y - lastTargetY) > TARGET_MOVE_YARDS;
        if (!hasDest || targetMoved) {
            destX = mx;
            destY = my;
            destZ = mz;
            lastTargetX = target.x;
            lastTargetY = target.y;
            hasDest = true;
            splineSent = false;
        }
        byte[] spline = null;
        if (!splineSent && readyToSend()) {
            spline = emit(c, destX, destY, destZ, UpdateBuilder.RUN, target.guid);
            splineSent = true;
            faceSent = true;
            lastFaceO = angleTo(c, target);
        }
        advance(c, destX, destY, destZ, UpdateBuilder.RUN, diffMs, g, target);
        return spline;
    }

    private byte[] updateHome(Creature c, int diffMs, Terrain.Height g) {
        sincePacketMs += diffMs;
        destX = c.spawnX;
        destY = c.spawnY;
        destZ = c.spawnZ;
        hasDest = true;
        byte[] spline = null;
        if (!splineSent && readyToSend()) {
            spline = emit(c, destX, destY, destZ, UpdateBuilder.RUN, 0);
            splineSent = true;
        }
        advance(c, destX, destY, destZ, UpdateBuilder.RUN, diffMs, g, null);
        if (arrived(c)) {
            c.relocate(c.spawnX, c.spawnY, c.spawnZ, c.spawnO);
        }
        return spline;
    }

    public boolean homeArrived(Creature c) {
        return type == HOME && c != null && arrived(c);
    }

    private byte[] updateRandom(Creature c, int diffMs, Terrain.Height g) {
        if (wanderRadius <= 0) {
            return null;
        }
        byte[] spline = null;
        if (!hasDest || arrived(c)) {
            pickDest(c, g);
            spline = monsterMove(c, destX, destY, destZ, UpdateBuilder.WALK);
            splineSent = true;
        }
        advance(c, destX, destY, destZ, UpdateBuilder.WALK, diffMs, g, null);
        return spline;
    }

    private void advance(Creature c, float toX, float toY, float toZ, float speed, int diffMs,
            Terrain.Height g, Unit faceToward) {
        double dx = toX - c.x;
        double dy = toY - c.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-3) {
            if (faceToward != null) {
                face(c, faceToward);
            }
            return;
        }
        float step = speed * (diffMs / 1000f);
        if (step > len) {
            step = (float) len;
        }
        float nx = c.x + (float) (dx / len * step);
        float ny = c.y + (float) (dy / len * step);
        float hintZ = c.z + (toZ - c.z) * (step / (float) len);
        float o = faceToward == null ? c.o : angleTo(c, faceToward);
        c.relocate(nx, ny, g.at(c.mapId, nx, ny, hintZ), o);
    }

    private byte[] faceOnWire(Creature c, Unit t) {
        float o = angleTo(c, t);
        c.relocate(c.x, c.y, c.z, o);
        if (faceSent && (!readyToSend() || angleDelta(lastFaceO, o) < FACE_RESEND_RAD)) {
            return null;
        }
        lastFaceO = o;
        faceSent = true;
        return emit(c, c.x, c.y, c.z, UpdateBuilder.RUN, t.guid);
    }

    private boolean readyToSend() {
        return !sentPacket || sincePacketMs >= CHASE_REACTION_MS;
    }

    private byte[] emit(Creature c, float x, float y, float z, float speed, long faceGuid) {
        sincePacketMs = 0;
        sentPacket = true;
        return monsterMove(c, x, y, z, speed, faceGuid, nextSplineId++);
    }

    private static float angleDelta(float a, float b) {
        float d = Math.abs(a - b);
        if (d > Math.PI) {
            d = (float) (Math.PI * 2 - d);
        }
        return d;
    }

    private static void face(Creature c, Unit t) {
        c.relocate(c.x, c.y, c.z, angleTo(c, t));
    }

    private static float angleTo(Creature c, Unit t) {
        float ang = (float) Math.atan2(t.y - c.y, t.x - c.x);
        return ang >= 0 ? ang : (float) (Math.PI * 2 + ang);
    }

    private boolean arrived(Creature c) {
        return Math.hypot(destX - c.x, destY - c.y) < 0.2;
    }

    private void pickDest(Creature c, Terrain.Height g) {
        double angle = rng.getAsDouble() * Math.PI * 2;
        double radius = rng.getAsDouble() * wanderRadius;
        destX = c.spawnX + (float) (Math.cos(angle) * radius);
        destY = c.spawnY + (float) (Math.sin(angle) * radius);
        destZ = g.at(c.mapId, destX, destY, c.spawnZ);
        hasDest = true;
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ) {
        return monsterMove(c, destX, destY, destZ, UpdateBuilder.RUN, 0, 1);
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ, float speed) {
        return monsterMove(c, destX, destY, destZ, speed, 0, 1);
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ, float speed, long faceGuid) {
        return monsterMove(c, destX, destY, destZ, speed, faceGuid, 1);
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ, float speed, long faceGuid,
            int splineId) {
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
        b.putU32(splineId);
        if (faceGuid != 0) {
            b.putU8(TaxiHandler.MONSTER_MOVE_FACING_TARGET);
            b.putU64(faceGuid);
        } else {
            b.putU8(TaxiHandler.MONSTER_MOVE_NORMAL);
        }
        b.putU32(TaxiHandler.SPLINE_FLAG_RUNMODE);
        b.putU32(duration);
        b.putU32(1);
        b.putFloat(destX);
        b.putFloat(destY);
        b.putFloat(destZ);
        return b.array();
    }
}
