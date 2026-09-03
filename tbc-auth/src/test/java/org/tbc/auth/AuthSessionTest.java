package org.tbc.auth;

import org.tbc.common.Bn;
import org.tbc.common.Codes;
import org.tbc.common.DbPool;
import org.tbc.common.Srp6;
import org.tbc.common.WowBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionTest {
    private DbPool db;
    private CaptureIo io;
    private AuthSession session;

    @BeforeEach
    void open() throws Exception {
        String url = "jdbc:h2:mem:tbcauth_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        db = new DbPool(url, "sa", "", "auth-test");
        try (Connection c = db.get(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE account (
                      id INT PRIMARY KEY,
                      username VARCHAR(32),
                      sessionkey VARCHAR(80) DEFAULT '',
                      v VARCHAR(80),
                      s VARCHAR(80),
                      locked INT DEFAULT 0,
                      lockedIp VARCHAR(32),
                      gmlevel INT DEFAULT 0,
                      expansion INT DEFAULT 1,
                      token VARCHAR(32),
                      os VARCHAR(8),
                      platform VARCHAR(8)
                    )
                    """);
            st.execute("""
                    CREATE TABLE account_banned (
                      account_id INT,
                      active INT,
                      banned_at INT,
                      expires_at INT
                    )
                    """);
            st.execute("""
                    CREATE TABLE ip_banned (
                      ip VARCHAR(32),
                      banned_at INT,
                      expires_at INT
                    )
                    """);
            st.execute("""
                    CREATE TABLE realmlist (
                      id INT,
                      name VARCHAR(32),
                      address VARCHAR(32),
                      port INT,
                      icon INT,
                      realmflags INT,
                      timezone INT,
                      allowedSecurityLevel INT,
                      population FLOAT
                    )
                    """);
            st.execute("""
                    CREATE TABLE realmcharacters (
                      realmid INT,
                      acctid INT,
                      numchars INT
                    )
                    """);
            st.execute("""
                    CREATE TABLE account_logons (
                      accountId INT,
                      ip VARCHAR(32),
                      loginTime TIMESTAMP,
                      loginSource INT
                    )
                    """);
            st.execute("INSERT INTO realmlist VALUES (1,'TBC','127.0.0.1',8085,0,0,1,0,0)");
        }
        io = new CaptureIo();
        session = new AuthSession(db, io);
    }

    @AfterEach
    void close() {
        db.close();
    }

    @Test
    void tpInv001WrongBuildRejected() {
        session.onBytes(new WowBuffer(challenge("PLAYER", 12340).array()));
        assertEquals(Codes.AUTH_LOGON_FAILED_VERSION_INVALID, io.lastError());
        assertTrue(io.closed);
        assertFalse(io.sawRealmList());
    }

    @Test
    void tpSl01WrongPasswordNoRealmList() throws Exception {
        insertAccount("PLAYER", "PLAYER", 0, "127.0.0.1", false);
        session.onBytes(new WowBuffer(challenge("PLAYER", Srp6.BUILD_8606).array()));
        assertEquals(0, io.lastError());
        Srp6.Client c = clientFromChallenge("PLAYER", "WRONG");
        session.onBytes(new WowBuffer(proof(c).array()));
        assertEquals(Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT, io.lastError());
        int sent = io.sent.size();
        session.onBytes(new WowBuffer(realmList().array()));
        assertEquals(sent, io.sent.size());
        assertFalse(io.sawRealmList());
        assertTrue(io.closed);
    }

    @Test
    void tpSl01SuccessRealmAndSessionkey() throws Exception {
        insertAccount("PLAYER", "PLAYER", 0, "127.0.0.1", false);
        logonOk("PLAYER", "PLAYER");
        session.onBytes(new WowBuffer(realmList().array()));
        assertTrue(io.sawRealmList());
        byte[] realm = io.last();
        String body = new String(realm, StandardCharsets.US_ASCII);
        assertTrue(body.contains("TBC"));
        try (Connection c = db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT sessionkey, v, s FROM account WHERE username='PLAYER'")) {
            assertTrue(rs.next());
            assertTrue(rs.getString("sessionkey").length() >= 40);
            assertFalse(rs.getString("v").isEmpty());
            assertFalse(rs.getString("s").isEmpty());
        }
        try (Connection c = db.get(); ResultSet cols = c.getMetaData().getColumns(null, null, "ACCOUNT", null)) {
            while (cols.next()) {
                assertNotEquals("PASSWORD", cols.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
            }
        }
    }

    @Test
    void tpSl01BannedAccount() throws Exception {
        insertAccount("PLAYER", "PLAYER", 0, "127.0.0.1", true);
        session.onBytes(new WowBuffer(challenge("PLAYER", Srp6.BUILD_8606).array()));
        int err = io.lastError();
        assertTrue(err == Codes.AUTH_LOGON_FAILED_BANNED || err == Codes.AUTH_LOGON_FAILED_SUSPENDED);
        assertFalse(io.sawRealmList());
    }

    @Test
    void tpSl01IpBanned() throws Exception {
        insertAccount("PLAYER", "PLAYER", 0, "127.0.0.1", false);
        io.ip = "10.0.0.9";
        try (Connection c = db.get(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO ip_banned VALUES ('10.0.0.9', 1, 1)");
        }
        session.onBytes(new WowBuffer(challenge("PLAYER", Srp6.BUILD_8606).array()));
        assertEquals(Codes.AUTH_LOGON_FAILED_BANNED, io.lastError());
        assertFalse(io.sawRealmList());
    }

    @Test
    void tpSl01IpLock() throws Exception {
        insertAccount("PLAYER", "PLAYER", 1, "1.2.3.4", false);
        io.ip = "10.0.0.9";
        session.onBytes(new WowBuffer(challenge("PLAYER", Srp6.BUILD_8606).array()));
        assertEquals(Codes.AUTH_LOGON_FAILED_FAIL_NOACCESS, io.lastError());
        assertFalse(io.sawRealmList());
    }

    @Test
    void tpSl02ReconnectWithoutPassword() throws Exception {
        insertAccount("PLAYER", "PLAYER", 0, "127.0.0.1", false);
        logonOk("PLAYER", "PLAYER");
        byte[] k;
        try (Connection c = db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT sessionkey FROM account WHERE username='PLAYER'")) {
            assertTrue(rs.next());
            k = Bn.beHexToLe(rs.getString(1), 40);
        }
        io.sent.clear();
        io.closed = false;
        session = new AuthSession(db, io);
        session.onBytes(new WowBuffer(reconnectChallenge("PLAYER").array()));
        byte[] ch = io.last();
        assertEquals(Codes.CMD_AUTH_RECONNECT_CHALLENGE, ch[0] & 0xFF);
        assertEquals(0, ch[1] & 0xFF);
        byte[] seed = Arrays.copyOfRange(ch, 2, 18);
        byte[] r1 = new byte[16];
        Arrays.fill(r1, (byte) 1);
        byte[] r2 = Srp6.reconnectClientR2("PLAYER", r1, seed, k);
        WowBuffer proof = new WowBuffer(57);
        proof.putU8(Codes.CMD_AUTH_RECONNECT_PROOF);
        proof.putBytes(r1);
        proof.putBytes(r2);
        proof.putBytes(new byte[20]);
        session.onBytes(new WowBuffer(proof.array()));
        assertEquals(Codes.CMD_AUTH_RECONNECT_PROOF, io.last()[0] & 0xFF);
        assertEquals(0, io.last()[1] & 0xFF);
        session.onBytes(new WowBuffer(realmList().array()));
        assertTrue(io.sawRealmList());
    }

    private void logonOk(String user, String pass) throws Exception {
        session.onBytes(new WowBuffer(challenge(user, Srp6.BUILD_8606).array()));
        assertEquals(0, io.lastError());
        Srp6.Client c = clientFromChallenge(user, pass);
        session.onBytes(new WowBuffer(proof(c).array()));
        assertEquals(0, io.last()[1] & 0xFF);
        assertEquals(20 + 4 + 4 + 2 + 2, io.last().length);
    }

    private Srp6.Client clientFromChallenge(String user, String pass) {
        WowBuffer in = new WowBuffer(io.last());
        in.getU8();
        in.getU8();
        in.getU8();
        byte[] bPub = in.getBytes(32);
        in.getU8();
        in.getU8();
        in.getU8();
        in.getBytes(32);
        byte[] salt = in.getBytes(32);
        return Srp6.clientRespond(user, pass, salt, bPub);
    }

    private void insertAccount(String user, String pass, int locked, String lockedIp, boolean banned)
            throws Exception {
        Srp6.Verifier v = Srp6.makeVerifier(user, pass);
        try (Connection c = db.get(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO account (id, username, sessionkey, v, s, locked, lockedIp, gmlevel, expansion, token) VALUES (1,'"
                    + user + "','','" + Bn.leToBeHex(v.vLe()) + "','" + Bn.leToBeHex(v.salt())
                    + "'," + locked + ",'" + lockedIp + "',0,1,'')");
            if (banned) {
                st.execute("INSERT INTO account_banned VALUES (1, 1, 1, 1)");
            }
        }
    }

    private static WowBuffer challenge(String user, int build) {
        byte[] name = user.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        WowBuffer body = new WowBuffer(48 + name.length);
        body.putBytes(new byte[]{'W', 'o', 'W', 0});
        body.putU8(2);
        body.putU8(4);
        body.putU8(3);
        body.putU16(build);
        body.putBytes(fourcc("x86"));
        body.putBytes(fourcc("Win"));
        body.putBytes(fourcc("enUS"));
        body.putU32(0);
        body.putU32(0x0100007F);
        body.putU8(name.length);
        body.putBytes(name);
        byte[] b = body.array();
        WowBuffer pkt = new WowBuffer(4 + b.length);
        pkt.putU8(Codes.CMD_AUTH_LOGON_CHALLENGE);
        pkt.putU8(0);
        pkt.putU16(b.length);
        pkt.putBytes(b);
        return pkt;
    }

    private static WowBuffer reconnectChallenge(String user) {
        byte[] pkt = challenge(user, Srp6.BUILD_8606).array();
        pkt[0] = (byte) Codes.CMD_AUTH_RECONNECT_CHALLENGE;
        return new WowBuffer(pkt);
    }

    private static WowBuffer proof(Srp6.Client c) {
        WowBuffer proof = new WowBuffer(75);
        proof.putU8(Codes.CMD_AUTH_LOGON_PROOF);
        proof.putBytes(c.aPubLe);
        proof.putBytes(c.m1);
        proof.putBytes(new byte[20]);
        proof.putU8(0);
        proof.putU8(0);
        return proof;
    }

    private static WowBuffer realmList() {
        WowBuffer pkt = new WowBuffer(5);
        pkt.putU8(Codes.CMD_REALM_LIST);
        pkt.putU32(0);
        return pkt;
    }

    private static byte[] fourcc(String s) {
        byte[] raw = Arrays.copyOf((s + "\0\0\0\0").getBytes(StandardCharsets.US_ASCII), 4);
        return new byte[]{raw[3], raw[2], raw[1], raw[0]};
    }

    private static final class CaptureIo implements AuthIo {
        String ip = "127.0.0.1";
        final List<byte[]> sent = new ArrayList<>();
        boolean closed;

        @Override
        public String remoteIp() {
            return ip;
        }

        @Override
        public void send(byte[] data) {
            sent.add(data);
        }

        @Override
        public void sendAndClose(byte[] data) {
            sent.add(data);
            closed = true;
        }

        @Override
        public void close() {
            closed = true;
        }

        byte[] last() {
            return sent.get(sent.size() - 1);
        }

        int lastError() {
            byte[] p = last();
            if ((p[0] & 0xFF) == Codes.CMD_AUTH_LOGON_CHALLENGE) {
                return p[2] & 0xFF;
            }
            return p[1] & 0xFF;
        }

        boolean sawRealmList() {
            for (byte[] p : sent) {
                if ((p[0] & 0xFF) == Codes.CMD_REALM_LIST) {
                    return true;
                }
            }
            return false;
        }
    }
}
