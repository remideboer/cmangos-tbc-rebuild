package org.tbc.matrix;

import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(180)
class LiveWorldCommandMatrixTest {

    @Test
    void everyClientOpcodePingAfter() throws Exception {
        if (!WowLiveClient.authUp() || !WowLiveClient.worldUp() || !WowLiveClient.hasCredentials()) {
            String why;
            if (!WowLiveClient.authUp() || !WowLiveClient.worldUp()) {
                why = "auth/world not listening";
            } else {
                why = "set TBC_ACCOUNT and TBC_PASSWORD";
            }
            MatrixReport.write("live-world", "Live world TCP",
                    List.of(new MatrixRow("world-live", 0, "(all CMSG/MSG)", "skip", why)));
        }
        assumeTrue(WowLiveClient.authUp(), "auth down");
        assumeTrue(WowLiveClient.worldUp(), "world down");
        assumeTrue(WowLiveClient.hasCredentials(), "set TBC_ACCOUNT and TBC_PASSWORD");

        List<MatrixRow> rows = new ArrayList<>();
        WowLiveClient c = new WowLiveClient();
        try {
            int err = c.logon(WowLiveClient.account(), WowLiveClient.password());
            assumeTrue(err == 0, "auth logon failed error=" + err);
            c.realmList();
            c.connectWorld();
            c.authSession();
            c.enterWorld();
            assertTrue(c.ping(1), "initial ping");

            int seq = 2;
            for (ClientCommands.Op op : ClientCommands.WORLD) {
                if (skipLive(op.id())) {
                    rows.add(new MatrixRow("world-live", op.id(), op.name(), "skip", "destructive or probe"));
                    continue;
                }
                String result;
                String note = "";
                try {
                    c.sendOpcode(op.id(), payload(op.id(), c.playerGuid));
                    Thread.sleep(30);
                    c.drain(50);
                    boolean pong = c.ping(seq++);
                    if (!pong) {
                        c.close();
                        c = new WowLiveClient();
                        c.logon(WowLiveClient.account(), WowLiveClient.password());
                        c.realmList();
                        c.connectWorld();
                        c.authSession();
                        c.enterWorld();
                        result = "fail";
                        note = "lost pong; reconnected";
                    } else if (HandledOpcodes.handled(op.id()) || "STATUS_NEVER".equals(op.sessionStatus())) {
                        result = "pass";
                        note = HandledOpcodes.handled(op.id()) ? "handler" : "STATUS_NEVER stub";
                    } else {
                        result = "unimplemented";
                    }
                    Thread.sleep(25);
                } catch (Exception e) {
                    result = "fail";
                    note = e.getClass().getSimpleName();
                    try {
                        c.close();
                        c = new WowLiveClient();
                        c.logon(WowLiveClient.account(), WowLiveClient.password());
                        c.realmList();
                        c.connectWorld();
                        c.authSession();
                        c.enterWorld();
                    } catch (Exception ignored) {
                        note += "/reconnect-failed";
                    }
                }
                rows.add(new MatrixRow("world-live", op.id(), op.name(), result, note));
            }
        } finally {
            c.close();
        }
        MatrixReport.write("live-world", "Live world TCP", rows);
        assertTrue(rows.stream().noneMatch(r -> "fail".equals(r.result())),
                () -> rows.stream().filter(r -> "fail".equals(r.result())).limit(20).toList().toString());
        assertTrue(WowLiveClient.worldUp(), "world process died");
    }

    private static boolean skipLive(int id) {
        return id == Opcodes.CMSG_AUTH_SESSION
                || id == Opcodes.CMSG_LOGOUT_REQUEST
                || id == Opcodes.CMSG_LOGOUT_CANCEL
                || id == Opcodes.CMSG_PLAYER_LOGOUT
                || id == Opcodes.CMSG_CHAR_DELETE
                || id == Opcodes.CMSG_CHAR_CREATE
                || id == Opcodes.CMSG_PLAYER_LOGIN
                || id == Opcodes.CMSG_CHAR_RENAME
                || id == Opcodes.CMSG_PING;
    }

    private static byte[] payload(int opcode, long guid) {
        if (opcode >= Opcodes.MSG_MOVE_START_FORWARD && opcode <= Opcodes.MSG_MOVE_HEARTBEAT
                || opcode == Opcodes.CMSG_FORCE_MOVE_ROOT_ACK
                || opcode == Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK) {
            WowBuffer b = new WowBuffer(48);
            if (opcode == Opcodes.CMSG_FORCE_MOVE_ROOT_ACK || opcode == Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK) {
                b.putPackedGuid(guid);
                b.putU32(0);
            }
            b.putU32(0);
            b.putU8(0);
            b.putU32(0);
            b.putFloat(-8949);
            b.putFloat(-132);
            b.putFloat(83);
            b.putFloat(0);
            b.putU32(0);
            return b.array();
        }
        return new byte[64];
    }
}
