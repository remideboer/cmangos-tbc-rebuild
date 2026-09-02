package org.tbc.matrix;

import org.tbc.common.Codes;
import org.tbc.common.Srp6;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(60)
class LiveAuthCommandMatrixTest {

    @Test
    void authNegativeAndCatalog() throws Exception {
        assumeTrue(WowLiveClient.authUp(), "auth not listening on " + WowLiveClient.authPort());
        List<MatrixRow> rows = new ArrayList<>();

        try (WowLiveClient c = new WowLiveClient()) {
            c.connectAuth();
            byte[] ch = c.logonChallenge("NOSUCHACCOUNTZZ", Srp6.BUILD_8606);
            int err = ch.length >= 3 ? ch[2] & 0xFF : -1;
            boolean pass = err == Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT;
            rows.add(new MatrixRow("auth-live", 0x00, "CMD_AUTH_LOGON_CHALLENGE unknown",
                    pass ? "pass" : "fail", "error=" + err));
        }

        try (WowLiveClient c = new WowLiveClient()) {
            c.connectAuth();
            byte[] ch = c.logonChallenge("PLAYER", 12340);
            int err = ch.length >= 3 ? ch[2] & 0xFF : -1;
            boolean pass = err == Codes.AUTH_LOGON_FAILED_VERSION_INVALID;
            rows.add(new MatrixRow("auth-live", 0x00, "CMD_AUTH_LOGON_CHALLENGE bad-build",
                    pass ? "pass" : "fail", "error=" + err));
        }

        for (ClientCommands.Auth xfer : ClientCommands.AUTH_XFER) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(WowLiveClient.authHost(), WowLiveClient.authPort()), 1000);
                OutputStream out = s.getOutputStream();
                out.write(xfer.id());
                out.flush();
                Thread.sleep(50);
            }
            boolean still = WowLiveClient.authUp();
            rows.add(new MatrixRow("auth-live", xfer.id(), xfer.name(),
                    still ? "pass" : "fail", still ? "process survived (unused XFER)" : "auth port down"));
        }

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(WowLiveClient.authHost(), WowLiveClient.authPort()), 1000);
            s.getOutputStream().write(0xFF);
            s.getOutputStream().flush();
            Thread.sleep(50);
        }
        rows.add(new MatrixRow("auth-live", 0xFF, "CMD_UNKNOWN",
                WowLiveClient.authUp() ? "pass" : "fail", "unexpected cmd must not kill process"));

        if (WowLiveClient.hasCredentials()) {
            try (WowLiveClient c = new WowLiveClient()) {
                int err = c.logon(WowLiveClient.account(), WowLiveClient.password());
                rows.add(new MatrixRow("auth-live", 0x01, "CMD_AUTH_LOGON_PROOF",
                        err == 0 ? "pass" : "fail", "error=" + err));
                if (err == 0) {
                    byte[] realms = c.realmList();
                    boolean ok = realms.length > 0 && (realms[0] & 0xFF) == Codes.CMD_REALM_LIST;
                    rows.add(new MatrixRow("auth-live", 0x10, "CMD_REALM_LIST",
                            ok ? "pass" : "fail", "bytes=" + realms.length));
                    int rec = c.reconnect(WowLiveClient.account());
                    rows.add(new MatrixRow("auth-live", 0x03, "CMD_AUTH_RECONNECT_PROOF",
                            rec == 0 ? "pass" : "fail", "error=" + rec));
                }
            }
        } else {
            for (ClientCommands.Auth a : ClientCommands.AUTH) {
                if (a.id() == 0x00) {
                    continue;
                }
                rows.add(new MatrixRow("auth-live", a.id(), a.name(), "skip", "TBC_ACCOUNT unset"));
            }
        }

        MatrixReport.write("live-auth", "Live auth TCP", rows);
        assertTrue(rows.stream().noneMatch(r -> "fail".equals(r.result())), () -> rows.toString());
        assertTrue(WowLiveClient.authUp());
    }
}
