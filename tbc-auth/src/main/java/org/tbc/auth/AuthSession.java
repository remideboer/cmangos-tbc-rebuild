package org.tbc.auth;

import org.tbc.common.Bn;
import org.tbc.common.Codes;
import org.tbc.common.DbPool;
import org.tbc.common.Srp6;
import org.tbc.common.WowBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AuthSession {
    private static final Logger log = LoggerFactory.getLogger(AuthSession.class);
    private static final SecureRandom RNG = new SecureRandom();

    private static final int ST_CHALLENGE = 0;
    private static final int ST_PROOF = 1;
    private static final int ST_RECON_PROOF = 2;
    private static final int ST_AUTHED = 3;
    private static final int ST_CLOSED = 4;

    private final DbPool db;
    private final AuthIo handler;
    private int status = ST_CHALLENGE;
    private String login = "";
    private int build;
    private String os = "";
    private String platform = "";
    private Srp6.Session srp;
    private byte[] reconnectProof;
    private byte[] sessionKey;
    private int accountId;
    private int gmlevel;
    private int expansion;
    private int securityFlags;

    AuthSession(DbPool db, AuthIo handler) {
        this.db = db;
        this.handler = handler;
    }

    void onBytes(WowBuffer in) {
        while (in.remaining() > 0 && status != ST_CLOSED) {
            int start = in.rpos();
            if (!completeCommand(in)) {
                in.rpos(start);
                return;
            }
            in.rpos(start);
            int cmd = in.getU8();
            if (cmd == Codes.CMD_AUTH_LOGON_CHALLENGE && status == ST_CHALLENGE) {
                handleLogonChallenge(in);
            } else if (cmd == Codes.CMD_AUTH_LOGON_PROOF && status == ST_PROOF) {
                handleLogonProof(in);
            } else if (cmd == Codes.CMD_AUTH_RECONNECT_CHALLENGE && status == ST_CHALLENGE) {
                handleReconnectChallenge(in);
            } else if (cmd == Codes.CMD_AUTH_RECONNECT_PROOF && status == ST_RECON_PROOF) {
                handleReconnectProof(in);
            } else if (cmd == Codes.CMD_REALM_LIST && status == ST_AUTHED) {
                in.skip(4);
                sendRealmList();
            } else {
                log.warn("unexpected auth cmd {} status {}", cmd, status);
                status = ST_CLOSED;
                handler.close();
                return;
            }
        }
    }

    private static boolean completeCommand(WowBuffer in) {
        if (in.remaining() < 1) {
            return false;
        }
        int cmd = in.getU8();
        if (cmd == Codes.CMD_AUTH_LOGON_CHALLENGE || cmd == Codes.CMD_AUTH_RECONNECT_CHALLENGE) {
            if (in.remaining() < 3) {
                return false;
            }
            in.getU8();
            int size = in.getU16();
            return in.remaining() >= size;
        }
        if (cmd == Codes.CMD_AUTH_LOGON_PROOF) {
            return in.remaining() >= 32 + 20 + 20 + 2;
        }
        if (cmd == Codes.CMD_AUTH_RECONNECT_PROOF) {
            return in.remaining() >= 16 + 20 + 20;
        }
        if (cmd == Codes.CMD_REALM_LIST) {
            return in.remaining() >= 4;
        }
        return true;
    }

    private void handleLogonChallenge(WowBuffer in) {
        if (in.remaining() < 4) {
            return;
        }
        in.getU8();
        int size = in.getU16();
        if (in.remaining() < size) {
            return;
        }
        in.skip(4);
        int v1 = in.getU8();
        int v2 = in.getU8();
        int v3 = in.getU8();
        build = in.getU16();
        platform = reverse4(in.getBytes(4));
        os = reverse4(in.getBytes(4));
        in.skip(4);
        in.getU32();
        in.getU32();
        int nameLen = in.getU8();
        if (nameLen < 1 || nameLen > 16 || in.remaining() < nameLen) {
            failChallenge(Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT);
            return;
        }
        login = new String(in.getBytes(nameLen), StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
        log.info("challenge {} build {} os {} platform {}", login, build, os, platform);
        if (build != Srp6.BUILD_8606) {
            failChallenge(Codes.AUTH_LOGON_FAILED_VERSION_INVALID);
            return;
        }
        try (Connection c = db.get()) {
            if (ipBanned(c)) {
                failChallenge(Codes.AUTH_LOGON_FAILED_BANNED);
                return;
            }
            PreparedStatement ps = c.prepareStatement(
                    "SELECT id, sessionkey, v, s, locked, lockedIp, gmlevel, expansion, token FROM account WHERE username = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                failChallenge(Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT);
                return;
            }
            accountId = rs.getInt("id");
            gmlevel = rs.getInt("gmlevel");
            expansion = rs.getInt("expansion");
            String token = rs.getString("token");
            int locked = rs.getInt("locked");
            String lockedIp = rs.getString("lockedIp");
            if (locked == 1 && lockedIp != null && !lockedIp.equals(handler.remoteIp())) {
                failChallenge(Codes.AUTH_LOGON_FAILED_FAIL_NOACCESS);
                return;
            }
            if (accountBanned(c, accountId)) {
                failChallenge(Codes.AUTH_LOGON_FAILED_BANNED);
                return;
            }
            byte[] v = Bn.beHexToLe(rs.getString("v"), 32);
            byte[] s = Bn.beHexToLe(rs.getString("s"), 32);
            if (v == null || s == null) {
                failChallenge(Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT);
                return;
            }
            securityFlags = (token != null && !token.isEmpty()) ? 0x04 : 0;
            srp = Srp6.serverChallenge(pad32(s), pad32(v));
            status = ST_PROOF;
            WowBuffer out = new WowBuffer(128);
            out.putU8(Codes.CMD_AUTH_LOGON_CHALLENGE);
            out.putU8(0);
            out.putU8(0);
            out.putBytes(srp.bPubLe);
            out.putU8(1);
            out.putU8(7);
            out.putU8(32);
            out.putBytes(Bn.toLe(Srp6.N, 32));
            out.putBytes(pad32(s));
            out.putBytes(Srp6.VERSION_CHALLENGE);
            out.putU8(securityFlags);
            if ((securityFlags & 0x04) != 0) {
                out.putU8(1);
            }
            handler.send(out.array());
        } catch (Exception e) {
            log.error("challenge", e);
            failChallenge(Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT);
        }
    }

    private void handleLogonProof(WowBuffer in) {
        if (in.remaining() < 32 + 20 + 20 + 2) {
            return;
        }
        byte[] A = in.getBytes(32);
        byte[] M1 = in.getBytes(20);
        in.skip(20);
        in.skip(2);
        boolean keyOk = srp != null && Srp6.serverSessionKey(srp, A);
        boolean m1Ok = keyOk && Srp6.proofM1(srp, login, M1);
        if (!keyOk || !m1Ok) {
            log.warn("proof fail {} keyOk={} m1Ok={} A={} M1={}", login, keyOk, m1Ok,
                    Bn.toHex(A), Bn.toHex(M1));
            failProof(Codes.AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT);
            return;
        }
        sessionKey = srp.k;
        persistSuccess();
        status = ST_AUTHED;
        log.info("proof ok {} id {}", login, accountId);
        WowBuffer out = new WowBuffer(32);
        out.putU8(Codes.CMD_AUTH_LOGON_PROOF);
        out.putU8(0);
        out.putBytes(srp.m2);
        out.putU32(Codes.ACCOUNT_FLAG_PROPASS);
        out.putU32(0);
        out.putU16(0);
        handler.send(out.array());
    }

    private void handleReconnectChallenge(WowBuffer in) {
        in.getU8();
        int size = in.getU16();
        if (in.remaining() < size) {
            return;
        }
        in.skip(4 + 3);
        build = in.getU16();
        in.skip(4 + 4 + 4 + 4 + 4);
        int nameLen = in.getU8();
        login = new String(in.getBytes(nameLen), StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
        try (Connection c = db.get()) {
            PreparedStatement ps = c.prepareStatement("SELECT sessionkey FROM account WHERE username = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                status = ST_CLOSED;
                handler.close();
                return;
            }
            sessionKey = Bn.beHexToLe(rs.getString(1), 40);
            if (sessionKey == null) {
                handler.close();
                return;
            }
            reconnectProof = new byte[16];
            RNG.nextBytes(reconnectProof);
            status = ST_RECON_PROOF;
            WowBuffer out = new WowBuffer(40);
            out.putU8(Codes.CMD_AUTH_RECONNECT_CHALLENGE);
            out.putU8(0);
            out.putBytes(reconnectProof);
            out.putBytes(Srp6.VERSION_CHALLENGE);
            handler.send(out.array());
        } catch (Exception e) {
            log.error("reconnect challenge", e);
            handler.close();
        }
    }

    private void handleReconnectProof(WowBuffer in) {
        if (in.remaining() < 16 + 20 + 20) {
            return;
        }
        byte[] r1 = in.getBytes(16);
        byte[] r2 = in.getBytes(20);
        in.skip(20);
        byte[] expect = Srp6.reconnectClientR2(login, r1, reconnectProof, sessionKey);
        if (!Bn.equal(expect, r2)) {
            handler.close();
            return;
        }
        status = ST_AUTHED;
        WowBuffer out = new WowBuffer(8);
        out.putU8(Codes.CMD_AUTH_RECONNECT_PROOF);
        out.putU8(0);
        out.putU16(0);
        handler.send(out.array());
    }

    private void sendRealmList() {
        try (Connection c = db.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT id, name, address, port, icon, realmflags, timezone, allowedSecurityLevel, population FROM realmlist");
            ResultSet rs = ps.executeQuery();
            List<byte[]> realms = new ArrayList<>();
            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                int allowed = rs.getInt("allowedSecurityLevel");
                int flags = rs.getInt("realmflags");
                if (gmlevel < allowed) {
                    continue;
                }
                int chars = 0;
                try (PreparedStatement pc = c.prepareStatement(
                        "SELECT numchars FROM realmcharacters WHERE realmid = ? AND acctid = ?")) {
                    pc.setInt(1, id);
                    pc.setInt(2, accountId);
                    ResultSet rc = pc.executeQuery();
                    if (rc.next()) {
                        chars = rc.getInt(1);
                    }
                }
                WowBuffer r = new WowBuffer(128);
                r.putU8(rs.getInt("icon"));
                r.putU8(gmlevel < allowed ? 1 : 0);
                r.putU8(flags);
                r.putCString(rs.getString("name"));
                r.putCString(rs.getString("address") + ":" + rs.getInt("port"));
                r.putFloat(rs.getFloat("population"));
                r.putU8(chars);
                r.putU8(rs.getInt("timezone"));
                r.putU8(0x2C);
                realms.add(r.array());
                count++;
            }
            WowBuffer body = new WowBuffer(256);
            body.putU32(0);
            body.putU16(count);
            for (byte[] r : realms) {
                body.putBytes(r);
            }
            body.putU16(0x0010);
            WowBuffer pkt = new WowBuffer(body.size() + 4);
            pkt.putU8(Codes.CMD_REALM_LIST);
            pkt.putU16(body.size());
            pkt.putBytes(body.array());
            handler.send(pkt.array());
        } catch (Exception e) {
            log.error("realm list", e);
        }
    }

    private void persistSuccess() {
        try (Connection c = db.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "UPDATE account SET sessionkey = ?, os = ?, platform = ? WHERE id = ?");
            ps.setString(1, Bn.leToBeHex(sessionKey));
            ps.setString(2, os);
            ps.setString(3, platform);
            ps.setInt(4, accountId);
            ps.executeUpdate();
            try (PreparedStatement logon = c.prepareStatement(
                    "INSERT INTO account_logons (accountId, ip, loginTime, loginSource) VALUES (?, ?, NOW(), 0)")) {
                logon.setInt(1, accountId);
                logon.setString(2, handler.remoteIp());
                logon.executeUpdate();
            }
        } catch (Exception e) {
            log.error("persist sessionkey", e);
        }
    }

    private boolean ipBanned(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM ip_banned WHERE ip = ? AND (expires_at = banned_at OR expires_at > UNIX_TIMESTAMP())");
        ps.setString(1, handler.remoteIp());
        return ps.executeQuery().next();
    }

    private boolean accountBanned(Connection c, int id) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM account_banned WHERE account_id = ? AND active = 1 AND (expires_at = banned_at OR expires_at > UNIX_TIMESTAMP())");
        ps.setInt(1, id);
        return ps.executeQuery().next();
    }

    private void failChallenge(int err) {
        log.warn("challenge fail {} err {}", login, err);
        status = ST_CLOSED;
        WowBuffer o = new WowBuffer(4);
        o.putU8(Codes.CMD_AUTH_LOGON_CHALLENGE);
        o.putU8(0);
        o.putU8(err);
        handler.sendAndClose(o.array());
    }

    private void failProof(int err) {
        status = ST_CLOSED;
        WowBuffer o = new WowBuffer(4);
        o.putU8(Codes.CMD_AUTH_LOGON_PROOF);
        o.putU8(err);
        o.putU8(0);
        o.putU8(0);
        handler.sendAndClose(o.array());
    }

    private static String reverse4(byte[] b) {
        return new String(new byte[]{b[3], b[2], b[1], b[0]}, StandardCharsets.US_ASCII).replace("\0", "");
    }

    private static byte[] pad32(byte[] b) {
        return pad(b, 32);
    }

    private static byte[] pad(byte[] b, int n) {
        if (b.length == n) {
            return b;
        }
        byte[] o = new byte[n];
        System.arraycopy(b, 0, o, 0, Math.min(b.length, n));
        return o;
    }
}
