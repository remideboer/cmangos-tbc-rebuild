package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.map.MapCoords;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** CMaNGOS MovementHandler.cpp: living moves, movement ACKs, spline done, time skipped (movement.md). */
public final class MovementHandler {
    private MovementHandler() {}

    /** Singleton movement opcodes; the two opcode classes go through the predicates below. */
    public static void register(OpcodeTable t) {
        // HandleMoveTeleportAckOpcode: raw guid + counter + time; position is the teleport dest.
        t.register(Opcodes.MSG_MOVE_TELEPORT_ACK, (s, w, in) -> { })
                .register(Opcodes.CMSG_FORCE_MOVE_ROOT_ACK, (s, w, in) -> move(s, w, Opcodes.CMSG_FORCE_MOVE_ROOT_ACK, in, true))
                .register(Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK, (s, w, in) -> move(s, w, Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK, in, true))
                .register(Opcodes.CMSG_MOVE_SPLINE_DONE, (s, w, in) -> splineDone(s, in))
                .register(Opcodes.CMSG_MOVE_TIME_SKIPPED, MovementHandler::timeSkipped)
                .register(Opcodes.CMSG_MOVE_FALL_RESET, MovementHandler::fallReset)
                .register(Opcodes.CMSG_MOVE_SET_FLY, (s, w, in) -> move(s, w, Opcodes.CMSG_MOVE_SET_FLY, in, false))
                .register(Opcodes.CMSG_MOVE_CHNG_TRANSPORT, (s, w, in) -> move(s, w, Opcodes.CMSG_MOVE_CHNG_TRANSPORT, in, false))
                .register(Opcodes.CMSG_MOVE_KNOCK_BACK_ACK, MovementHandler::knockBackAck)
                .register(Opcodes.CMSG_MOVE_HOVER_ACK, (s, w, in) -> flagChangeAck(s, w, in, Opcodes.MSG_MOVE_HOVER))
                .register(Opcodes.CMSG_MOVE_WATER_WALK_ACK, (s, w, in) -> flagChangeAck(s, w, in, Opcodes.MSG_MOVE_WATER_WALK))
                .register(Opcodes.CMSG_MOVE_FEATHER_FALL_ACK, (s, w, in) -> flagChangeAck(s, w, in, Opcodes.MSG_MOVE_FEATHER_FALL))
                .register(Opcodes.CMSG_MOVE_NOT_ACTIVE_MOVER, MovementHandler::notActiveMover);
    }

    public static boolean isForceSpeedChangeAck(int opcode) {
        return opcode == Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_RUN_BACK_SPEED_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_SWIM_SPEED_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_WALK_SPEED_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_SWIM_BACK_SPEED_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_TURN_RATE_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_FLIGHT_SPEED_CHANGE_ACK
                || opcode == Opcodes.CMSG_FORCE_FLIGHT_BACK_SPEED_CHANGE_ACK;
    }

    /**
     * CMaNGOS Opcodes.cpp handlers that call HandleMovementOpcodes with a bare MovementInfo
     * (no GUID/counter prefix). Do not use a numeric range — it includes SMSG and cheat opcodes.
     */
    public static boolean isLivingMoveOpcode(int opcode) {
        return switch (opcode) {
            case Opcodes.MSG_MOVE_START_FORWARD,
                    Opcodes.MSG_MOVE_START_BACKWARD,
                    Opcodes.MSG_MOVE_STOP,
                    Opcodes.MSG_MOVE_START_STRAFE_LEFT,
                    Opcodes.MSG_MOVE_START_STRAFE_RIGHT,
                    Opcodes.MSG_MOVE_STOP_STRAFE,
                    Opcodes.MSG_MOVE_JUMP,
                    Opcodes.MSG_MOVE_START_TURN_LEFT,
                    Opcodes.MSG_MOVE_START_TURN_RIGHT,
                    Opcodes.MSG_MOVE_STOP_TURN,
                    Opcodes.MSG_MOVE_START_PITCH_UP,
                    Opcodes.MSG_MOVE_START_PITCH_DOWN,
                    Opcodes.MSG_MOVE_STOP_PITCH,
                    Opcodes.MSG_MOVE_SET_RUN_MODE,
                    Opcodes.MSG_MOVE_SET_WALK_MODE,
                    Opcodes.MSG_MOVE_FALL_LAND,
                    Opcodes.MSG_MOVE_START_SWIM,
                    Opcodes.MSG_MOVE_STOP_SWIM,
                    Opcodes.MSG_MOVE_SET_FACING,
                    Opcodes.MSG_MOVE_SET_PITCH,
                    Opcodes.MSG_MOVE_HEARTBEAT,
                    Opcodes.MSG_MOVE_START_ASCEND,
                    Opcodes.MSG_MOVE_STOP_ASCEND,
                    Opcodes.MSG_MOVE_START_DESCEND -> true;
            default -> false;
        };
    }

    /** Speed-change ACK: movement + trailing float speed; malformed packets are ignored. */
    public static void forceSpeedChangeAck(WorldSession s, World world, int opcode, WowBuffer in) {
        try {
            move(s, world, opcode, in, true);
            if (in.remaining() >= 4) {
                s.player().lastAckSpeed = in.getFloat();
            }
        } catch (RuntimeException ignored) {
        }
    }

    /** HandleMovementOpcodes — apply, echo to nearby (same opcode), duel bounds, explore, reveal. */
    public static void move(WorldSession s, World world, int opcode, WowBuffer in, boolean ack) {
        if (ack) {
            if (!skipAckGuid(in)) {
                return;
            }
            in.getU32();
        }
        Player player = s.player();
        MovementInfo m = MovementInfo.readC2s(in);
        if (!acceptsMovement(player, m)) {
            return;
        }
        // CMaNGOS MovementHandler: MOVEFLAG_MASK_MOVING_OR_TURN while sitting → SetStandState(STAND)
        // (removes STANDING_CANCELS food/drink via leaveSeatedAuras).
        if ((m.moveFlags & MovementInfo.MOVEFLAG_MASK_MOVING_OR_TURN) != 0 && player.isSitState()) {
            player.stand();
        }
        apply(world, player, m);
        m.stime = (int) world.nowMs();
        WowBuffer echo = new WowBuffer(64);
        m.write(echo, true, player.guid, m.stime);
        for (Player o : world.map(player.mapId, player.instanceId).nearbyPlayers(player, GameMap.VISIBILITY)) {
            o.session.send(opcode, echo.array());
        }
        if (player.duelOpponent != null && player.distance2d(player.duelOpponent) > 50) {
            s.send(Opcodes.SMSG_DUEL_OUTOFBOUNDS, new byte[0]);
        }
        s.maybeExplore(world);
        s.revealNearby(world);
    }

    public static void splineDone(WorldSession s, WowBuffer in) {
        try {
            MovementInfo.readC2s(in);
            if (in.remaining() >= 4) {
                s.player().lastSplineDoneCounter = in.getU32();
            }
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * HandleMoveTimeSkippedOpcode — raw guid + uint32. Observers get MSG_MOVE_TIME_SKIPPED
     * packed guid + skipped. Sender is excluded. Wrong guid is ignored.
     */
    public static void timeSkipped(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 12) {
            return;
        }
        Player player = s.player();
        long guid = in.getU64();
        int skipped = in.getU32();
        if (guid != player.guid) {
            return;
        }
        WowBuffer data = new WowBuffer(16);
        data.putPackedGuid(player.guid);
        data.putU32(skipped);
        broadcastToOthers(world, player, Opcodes.MSG_MOVE_TIME_SKIPPED, data.array());
    }

    /**
     * HandleMovementOpcodes for CMSG_MOVE_FALL_RESET — apply MovementInfo, do not echo.
     * The 8606 client has no handler for this CMSG.
     */
    public static void fallReset(WorldSession s, World world, WowBuffer in) {
        Player player = s.player();
        MovementInfo m = MovementInfo.readC2s(in);
        if (!acceptsMovement(player, m)) {
            return;
        }
        apply(world, player, m);
    }

    /**
     * HandleMoveKnockBackAck — packed guid + counter + MovementInfo.
     * Observers get MSG_MOVE_KNOCK_BACK: packed guid + MovementInfo + jump cos/sin/xy/zspeed.
     * Sender excluded (SendMessageToAllWhoSeeMeMove).
     */
    public static void knockBackAck(WorldSession s, World world, WowBuffer in) {
        if (!skipAckGuid(in) || in.remaining() < 4) {
            return;
        }
        in.getU32();
        Player player = s.player();
        MovementInfo m = MovementInfo.readC2s(in);
        if (!acceptsMovement(player, m)) {
            return;
        }
        apply(world, player, m);
        m.stime = (int) world.nowMs();
        WowBuffer echo = new WowBuffer(80);
        m.write(echo, true, player.guid, m.stime);
        echo.putFloat(m.jumpCos);
        echo.putFloat(m.jumpSin);
        echo.putFloat(m.jumpXy);
        echo.putFloat(m.jumpZ);
        broadcastToOthers(world, player, Opcodes.MSG_MOVE_KNOCK_BACK, echo.array());
    }

    /**
     * HandleMoveFlagChangeOpcode — packed guid + counter + MovementInfo + isApplied u32.
     * Observers get response MSG (HOVER / WATER_WALK / FEATHER_FALL) with packed guid + MovementInfo.
     */
    public static void flagChangeAck(WorldSession s, World world, WowBuffer in, int responseOpcode) {
        if (!skipAckGuid(in) || in.remaining() < 4) {
            return;
        }
        in.getU32();
        Player player = s.player();
        MovementInfo m = MovementInfo.readC2s(in);
        if (in.remaining() >= 4) {
            in.getU32();
        }
        if (!acceptsMovement(player, m)) {
            return;
        }
        apply(world, player, m);
        m.stime = (int) world.nowMs();
        WowBuffer echo = new WowBuffer(64);
        m.write(echo, true, player.guid, m.stime);
        broadcastToOthers(world, player, responseOpcode, echo.array());
    }

    /** HandleMoveNotActiveMoverOpcode — packed guid + MovementInfo. Apply only; no echo. */
    public static void notActiveMover(WorldSession s, World world, WowBuffer in) {
        if (!skipAckGuid(in)) {
            return;
        }
        Player player = s.player();
        MovementInfo m = MovementInfo.readC2s(in);
        if (!acceptsMovement(player, m)) {
            return;
        }
        apply(world, player, m);
    }

    /** CMaNGOS-tbc movement ACKs: {@code >> ObjectGuid} is a raw u64 (movement.md). */
    private static boolean skipAckGuid(WowBuffer in) {
        if (in.remaining() < 8) {
            return false;
        }
        in.getU64();
        return true;
    }

    /**
     * ProcessMovementInfo: ignore while IsBeingTeleported; VerifyMovementInfo drops
     * !IsValidMapCoord so a bad packet cannot strand the player in the void.
     */
    private static boolean acceptsMovement(Player player, MovementInfo m) {
        return !player.teleportPending && MapCoords.valid(m.x, m.y, m.z, m.o);
    }

    /** Relocate on the map grid and keep the last MovementInfo. */
    private static void apply(World world, Player player, MovementInfo m) {
        float ox = player.x;
        float oy = player.y;
        player.relocate(m.x, m.y, m.z, m.o);
        world.map(player.mapId, player.instanceId).reindex(player, ox, oy);
        player.movement = m;
    }

    private static void broadcastToOthers(World world, Player player, int opcode, byte[] pkt) {
        for (Player o : world.map(player.mapId, player.instanceId).nearbyPlayers(player, GameMap.VISIBILITY)) {
            if (o != player && o.session != null) {
                o.session.send(opcode, pkt);
            }
        }
    }
}
