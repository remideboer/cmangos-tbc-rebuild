package org.tbc.world.ai;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.TaxiHandler;
import org.tbc.world.session.WorldSession;

/** Straight-line chase. spec/05-domain/movement-and-pathfinding.md PathFinder fallback. */
public final class MotionMaster {
    public static final int IDLE = 0;
    public static final int CHASE = 1;

    private int type = IDLE;
    private Unit target;

    public int type() {
        return type;
    }

    public void moveChase(Unit victim) {
        type = CHASE;
        target = victim;
    }

    public void moveIdle() {
        type = IDLE;
        target = null;
    }

    public byte[] update(Creature c, int diffMs) {
        if (type != CHASE || target == null || c == null || diffMs <= 0) {
            return null;
        }
        double dist = c.distance2d(target);
        float stop = WorldSession.MELEE_RANGE;
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
        return monsterMove(c, target.x, target.y, target.z);
    }

    static byte[] monsterMove(Creature c, float destX, float destY, float destZ) {
        float dx = destX - c.x;
        float dy = destY - c.y;
        float dz = destZ - c.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        int duration = Math.max(1, (int) (dist / UpdateBuilder.RUN * 1000f));
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
