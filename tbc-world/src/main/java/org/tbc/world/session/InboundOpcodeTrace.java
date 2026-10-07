package org.tbc.world.session;

import org.tbc.world.net.wow8606.Opcodes;

/**
 * Lab inbound C2S opcode lines for {@code logs/world.log}. Skips ping/movement so gossip and
 * query packets stay visible when {@code LogInboundOpcodes = 1}.
 */
public final class InboundOpcodeTrace {
    private InboundOpcodeTrace() {}

    public static boolean shouldLog(int opcode) {
        if (opcode == Opcodes.CMSG_PING
                || opcode == Opcodes.CMSG_KEEP_ALIVE
                || opcode == Opcodes.CMSG_WARDEN_DATA
                || opcode == Opcodes.CMSG_TIME_SYNC_RESP) {
            return false;
        }
        if (MovementHandler.isLivingMoveOpcode(opcode)) {
            return false;
        }
        return switch (opcode) {
            case Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_RUN_BACK_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_SWIM_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_WALK_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_SWIM_BACK_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_TURN_RATE_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_FLIGHT_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_FLIGHT_BACK_SPEED_CHANGE_ACK,
                    Opcodes.CMSG_FORCE_MOVE_ROOT_ACK,
                    Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK,
                    Opcodes.CMSG_MOVE_WATER_WALK_ACK,
                    Opcodes.CMSG_MOVE_HOVER_ACK,
                    Opcodes.CMSG_MOVE_FEATHER_FALL_ACK,
                    Opcodes.CMSG_MOVE_KNOCK_BACK_ACK,
                    Opcodes.MSG_MOVE_TELEPORT_ACK,
                    Opcodes.MSG_MOVE_WORLDPORT_ACK,
                    Opcodes.CMSG_MOVE_NOT_ACTIVE_MOVER,
                    Opcodes.CMSG_MOVE_SPLINE_DONE,
                    Opcodes.CMSG_MOVE_TIME_SKIPPED,
                    Opcodes.CMSG_MOVE_FALL_RESET,
                    Opcodes.CMSG_MOVE_SET_FLY,
                    Opcodes.CMSG_MOVE_CHNG_TRANSPORT -> false;
            default -> true;
        };
    }

    public static String format(String playerName, int opcode, int size) {
        String who = playerName == null || playerName.isEmpty() ? "-" : playerName;
        return String.format("C2S %s (0x%X) size=%d player=%s",
                Opcodes.name(opcode), opcode, size, who);
    }
}
