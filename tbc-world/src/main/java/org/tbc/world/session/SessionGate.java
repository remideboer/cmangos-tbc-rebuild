package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/**
 * Status-gated routing that runs before the logged-in switch in {@link WorldSession#handle}
 * (CMaNGOS WorldSession::Update opcode status checks).
 *
 * <ul>
 *   <li>{@code STATUS_NEVER}: only the handshake ({@code CMSG_PING}, {@code CMSG_KEEP_ALIVE},
 *       {@code CMSG_AUTH_SESSION}); anything else is dropped.</li>
 *   <li>{@code STATUS_AUTHED}: character-screen opcodes; otherwise only
 *       {@code CMSG_UPDATE_ACCOUNT_DATA} (STATUS_LOGGEDIN_OR_RECENTLY_LOGGEDOUT) is accepted.</li>
 *   <li>{@code STATUS_LOGGEDIN}: movement opcodes are consumed here; everything else is left to
 *       the caller's switch / {@link OpcodeTable}.</li>
 * </ul>
 */
final class SessionGate {
    private final OpcodeTable handshake = new OpcodeTable();
    private final OpcodeTable characterScreen = new OpcodeTable();
    private final OpcodeTable recentlyLoggedOut = new OpcodeTable();
    private final OpcodeTable movement = new OpcodeTable();

    SessionGate() {
        handshake.register(Opcodes.CMSG_PING, (s, w, in) -> s.handlePing(w, in))
                .register(Opcodes.CMSG_KEEP_ALIVE, (s, w, in) -> { })
                .register(Opcodes.CMSG_AUTH_SESSION, (s, w, in) -> s.handleAuthSession(w, in));
        characterScreen.register(Opcodes.CMSG_CHAR_ENUM, (s, w, in) -> s.handleCharEnum(w))
                .register(Opcodes.CMSG_CHAR_CREATE, (s, w, in) -> s.handleCharCreate(w, in))
                .register(Opcodes.CMSG_CHAR_DELETE, (s, w, in) -> s.handleCharDelete(w, in))
                .register(Opcodes.CMSG_CHAR_RENAME, (s, w, in) -> s.handleCharRename(w, in))
                .register(Opcodes.CMSG_SET_PLAYER_DECLINED_NAMES, (s, w, in) -> s.handleSetPlayerDeclinedNames(w, in))
                .register(Opcodes.CMSG_PLAYER_LOGIN, (s, w, in) -> s.handleLogin(w, in))
                .register(Opcodes.CMSG_GUILD_QUERY, QueryHandler::guild)
                .register(Opcodes.CMSG_REALM_SPLIT, (s, w, in) -> s.handleRealmSplit(in))
                .register(Opcodes.CMSG_OPT_OUT_OF_LOOT, (s, w, in) -> GroupHandler.optOutOfLoot(in));
        // Client flushes UI prefs on logout.
        recentlyLoggedOut.register(Opcodes.CMSG_UPDATE_ACCOUNT_DATA, (s, w, in) -> s.handleUpdateAccountData(w, in));
        movement.register(Opcodes.MSG_MOVE_WORLDPORT_ACK, (s, w, in) -> s.handleWorldportAck(w));
        MovementHandler.register(movement);
    }

    /** @return true when the packet was consumed (handled or dropped by status); false leaves it to the caller. */
    boolean dispatch(WorldSession s, World world, int opcode, WowBuffer in) {
        if (handshake.dispatch(s, world, opcode, in)) {
            return true;
        }
        if (s.status() < WorldSession.STATUS_AUTHED) {
            return true;
        }
        if (characterScreen.dispatch(s, world, opcode, in)) {
            return true;
        }
        if (s.status() < WorldSession.STATUS_LOGGEDIN) {
            recentlyLoggedOut.dispatch(s, world, opcode, in);
            return true;
        }
        if (MovementHandler.isForceSpeedChangeAck(opcode)) {
            MovementHandler.forceSpeedChangeAck(s, world, opcode, in);
            return true;
        }
        if (MovementHandler.isLivingMoveOpcode(opcode)) {
            MovementHandler.move(s, world, opcode, in, false);
            return true;
        }
        return movement.dispatch(s, world, opcode, in);
    }
}
