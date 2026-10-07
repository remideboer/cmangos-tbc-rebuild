package org.tbc.world.session;

import org.junit.jupiter.api.Test;
import org.tbc.world.net.wow8606.Opcodes;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Movement opcode classes extracted from WorldSession (refactoring plan cycle 2.2). */
class MovementHandlerTest {
    @Test
    void isLivingMoveOpcodeWhenBareMovementInfoShouldBeTrue() {
        assertTrue(MovementHandler.isLivingMoveOpcode(Opcodes.MSG_MOVE_HEARTBEAT));
        assertTrue(MovementHandler.isLivingMoveOpcode(Opcodes.MSG_MOVE_START_FORWARD));
        assertTrue(MovementHandler.isLivingMoveOpcode(Opcodes.MSG_MOVE_START_DESCEND));
    }

    @Test
    void isLivingMoveOpcodeWhenAckOrNonMovementShouldBeFalse() {
        assertFalse(MovementHandler.isLivingMoveOpcode(Opcodes.CMSG_FORCE_MOVE_ROOT_ACK));
        assertFalse(MovementHandler.isLivingMoveOpcode(Opcodes.CMSG_CAST_SPELL));
    }

    @Test
    void isForceSpeedChangeAckShouldCoverAllEightSpeedAcks() {
        assertTrue(MovementHandler.isForceSpeedChangeAck(Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK));
        assertTrue(MovementHandler.isForceSpeedChangeAck(Opcodes.CMSG_FORCE_FLIGHT_BACK_SPEED_CHANGE_ACK));
        assertFalse(MovementHandler.isForceSpeedChangeAck(Opcodes.CMSG_FORCE_MOVE_ROOT_ACK));
    }

    @Test
    void registerShouldCoverExactlyTheSingletonMovementOpcodes() {
        OpcodeTable t = new OpcodeTable();
        MovementHandler.register(t);
        assertEquals(Set.of(
                Opcodes.MSG_MOVE_TELEPORT_ACK, Opcodes.CMSG_FORCE_MOVE_ROOT_ACK, Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK,
                Opcodes.CMSG_MOVE_SPLINE_DONE, Opcodes.CMSG_MOVE_TIME_SKIPPED, Opcodes.CMSG_MOVE_FALL_RESET,
                Opcodes.CMSG_MOVE_SET_FLY, Opcodes.CMSG_MOVE_CHNG_TRANSPORT, Opcodes.CMSG_MOVE_KNOCK_BACK_ACK,
                Opcodes.CMSG_MOVE_HOVER_ACK, Opcodes.CMSG_MOVE_WATER_WALK_ACK, Opcodes.CMSG_MOVE_FEATHER_FALL_ACK,
                Opcodes.CMSG_MOVE_NOT_ACTIVE_MOVER), t.opcodes());
    }
}
