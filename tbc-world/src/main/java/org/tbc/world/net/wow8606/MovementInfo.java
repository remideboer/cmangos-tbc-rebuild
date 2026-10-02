package org.tbc.world.net.wow8606;

import org.tbc.common.WowBuffer;

/** spec/03-protocol/packets/movement.md — fallTime is always present. */
public final class MovementInfo {
    public static final int MOVEFLAG_FORWARD = 0x00000001;
    public static final int MOVEFLAG_BACKWARD = 0x00000002;
    public static final int MOVEFLAG_STRAFE_LEFT = 0x00000004;
    public static final int MOVEFLAG_STRAFE_RIGHT = 0x00000008;
    public static final int MOVEFLAG_TURN_LEFT = 0x00000010;
    public static final int MOVEFLAG_TURN_RIGHT = 0x00000020;
    public static final int MOVEFLAG_PITCH_UP = 0x00000040;
    public static final int MOVEFLAG_PITCH_DOWN = 0x00000080;
    public static final int MOVEFLAG_WALK_MODE = 0x00000100;
    public static final int MOVEFLAG_ONTRANSPORT = 0x00000200;
    public static final int MOVEFLAG_FALLING = 0x00001000;
    public static final int MOVEFLAG_JUMPING = 0x00002000;
    public static final int MOVEFLAG_FALLINGFAR = 0x00004000;
    public static final int MOVEFLAG_SWIMMING = 0x00200000;
    public static final int MOVEFLAG_FLYING2 = 0x02000000;
    public static final int MOVEFLAG_SPLINE_ELEVATION = 0x04000000;
    /** Object.h — stand from sit when moving/turning (food/drink cancel). */
    public static final int MOVEFLAG_MASK_MOVING_OR_TURN =
            MOVEFLAG_FORWARD | MOVEFLAG_BACKWARD | MOVEFLAG_STRAFE_LEFT | MOVEFLAG_STRAFE_RIGHT
                    | MOVEFLAG_PITCH_UP | MOVEFLAG_PITCH_DOWN | MOVEFLAG_JUMPING | MOVEFLAG_FALLINGFAR
                    | MOVEFLAG_SPLINE_ELEVATION | MOVEFLAG_TURN_LEFT | MOVEFLAG_TURN_RIGHT;

    public int moveFlags;
    public int moveFlags2;
    public int ctime;
    public int stime;
    public float x, y, z, o;
    public long transportGuid;
    public float tx, ty, tz, to;
    public int tTime;
    public float pitch;
    public int fallTime;
    public float jumpZ, jumpCos, jumpSin, jumpXy;
    public float splineElevation;

    public static MovementInfo readC2s(WowBuffer in) {
        MovementInfo m = new MovementInfo();
        if (in.remaining() < 29) {
            return m;
        }
        m.moveFlags = in.getU32();
        m.moveFlags2 = in.getU8();
        m.ctime = in.getU32();
        m.x = in.getFloat();
        m.y = in.getFloat();
        m.z = in.getFloat();
        m.o = in.getFloat();
        if ((m.moveFlags & MOVEFLAG_ONTRANSPORT) != 0) {
            if (!hasTransportBlock(in)) {
                m.moveFlags &= ~MOVEFLAG_ONTRANSPORT;
            } else {
                m.transportGuid = in.getPackedGuid();
                m.tx = in.getFloat();
                m.ty = in.getFloat();
                m.tz = in.getFloat();
                m.to = in.getFloat();
                m.tTime = in.getU32();
            }
        }
        if ((m.moveFlags & (MOVEFLAG_SWIMMING | MOVEFLAG_FLYING2)) != 0) {
            if (in.remaining() < 4) {
                m.moveFlags &= ~(MOVEFLAG_SWIMMING | MOVEFLAG_FLYING2);
            } else {
                m.pitch = in.getFloat();
            }
        }
        if (in.remaining() < 4) {
            return m;
        }
        m.fallTime = in.getU32();
        if ((m.moveFlags & MOVEFLAG_FALLING) != 0) {
            if (in.remaining() < 16) {
                m.moveFlags &= ~MOVEFLAG_FALLING;
            } else {
                m.jumpZ = in.getFloat();
                m.jumpCos = in.getFloat();
                m.jumpSin = in.getFloat();
                m.jumpXy = in.getFloat();
            }
        }
        if ((m.moveFlags & MOVEFLAG_SPLINE_ELEVATION) != 0) {
            if (in.remaining() < 4) {
                m.moveFlags &= ~MOVEFLAG_SPLINE_ELEVATION;
            } else {
                m.splineElevation = in.getFloat();
            }
        }
        return m;
    }

    /** Packed transport GUID (1+0..8) + xyz o + tTime — stop if the client truncated mid-block. */
    private static boolean hasTransportBlock(WowBuffer in) {
        if (in.remaining() < 1) {
            return false;
        }
        int mask = in.peekU8();
        int guidBytes = Integer.bitCount(mask & 0xFF);
        return in.remaining() >= 1 + guidBytes + 20;
    }

    public void write(WowBuffer out, boolean packedGuidPrefix, long guid, int serverTime) {
        if (packedGuidPrefix) {
            out.putPackedGuid(guid);
        }
        out.putU32(moveFlags);
        out.putU8(moveFlags2);
        out.putU32(serverTime);
        out.putFloat(x);
        out.putFloat(y);
        out.putFloat(z);
        out.putFloat(o);
        if ((moveFlags & MOVEFLAG_ONTRANSPORT) != 0) {
            out.putPackedGuid(transportGuid);
            out.putFloat(tx);
            out.putFloat(ty);
            out.putFloat(tz);
            out.putFloat(to);
            out.putU32(tTime);
        }
        if ((moveFlags & (MOVEFLAG_SWIMMING | MOVEFLAG_FLYING2)) != 0) {
            out.putFloat(pitch);
        }
        out.putU32(fallTime);
        if ((moveFlags & MOVEFLAG_FALLING) != 0) {
            out.putFloat(jumpZ);
            out.putFloat(jumpCos);
            out.putFloat(jumpSin);
            out.putFloat(jumpXy);
        }
        if ((moveFlags & MOVEFLAG_SPLINE_ELEVATION) != 0) {
            out.putFloat(splineElevation);
        }
    }
}
