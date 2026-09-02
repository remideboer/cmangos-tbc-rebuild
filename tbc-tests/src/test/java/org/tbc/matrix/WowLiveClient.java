package org.tbc.matrix;

import org.tbc.common.AuthCrypt;
import org.tbc.common.Codes;
import org.tbc.common.Sha1;
import org.tbc.common.Srp6;
import org.tbc.common.WorldHeader;
import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.AddonInfo;
import org.tbc.world.net.wow8606.Opcodes;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.Deflater;

/** Minimal 8606 TCP client for live matrix tests. */
final class WowLiveClient implements AutoCloseable {
    record Pkt(int opcode, byte[] payload) {}

    private Socket auth;
    private Socket world;
    private DataInputStream authIn;
    private OutputStream authOut;
    private DataInputStream worldIn;
    private OutputStream worldOut;
    private final AuthCrypt crypt = new AuthCrypt();
    byte[] sessionKey;
    String username;
    int serverSeed;
    long playerGuid;

    static String env(String key, String dflt) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? dflt : v;
    }

    static String authHost() {
        return env("TBC_AUTH_HOST", "127.0.0.1");
    }

    static int authPort() {
        return Integer.parseInt(env("TBC_AUTH_PORT", "3724"));
    }

    static String worldHost() {
        return env("TBC_WORLD_HOST", "127.0.0.1");
    }

    static int worldPort() {
        return Integer.parseInt(env("TBC_WORLD_PORT", "8085"));
    }

    static String account() {
        return env("TBC_ACCOUNT", "");
    }

    static String password() {
        return env("TBC_PASSWORD", "");
    }

    static boolean portOpen(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 400);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean authUp() {
        return portOpen(authHost(), authPort());
    }

    static boolean worldUp() {
        return portOpen(worldHost(), worldPort());
    }

    static boolean hasCredentials() {
        return !account().isEmpty() && !password().isEmpty();
    }

    void connectAuth() throws IOException {
        auth = new Socket();
        auth.connect(new InetSocketAddress(authHost(), authPort()), 2000);
        auth.setSoTimeout(4000);
        authIn = new DataInputStream(auth.getInputStream());
        authOut = auth.getOutputStream();
    }

    void sendAuth(byte[] pkt) throws IOException {
        authOut.write(pkt);
        authOut.flush();
    }

    static byte[] fourcc(String s) {
        byte[] raw = Arrays.copyOf((s + "\0\0\0\0").getBytes(StandardCharsets.US_ASCII), 4);
        return new byte[]{raw[3], raw[2], raw[1], raw[0]};
    }

    byte[] logonChallenge(String user, int build) throws IOException {
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
        sendAuth(pkt.array());
        int cmd = authIn.readUnsignedByte();
        int unk = authIn.readUnsignedByte();
        int err = authIn.readUnsignedByte();
        if (err != 0) {
            return new byte[]{(byte) cmd, (byte) unk, (byte) err};
        }
        byte[] rest = authIn.readNBytes(32 + 1 + 1 + 1 + 32 + 32 + 16 + 1);
        WowBuffer all = new WowBuffer(3 + rest.length);
        all.putU8(cmd);
        all.putU8(unk);
        all.putU8(err);
        all.putBytes(rest);
        return all.array();
    }

    int logon(String user, String pass) throws IOException {
        connectAuth();
        username = user.toUpperCase(Locale.ROOT);
        byte[] ch = logonChallenge(username, Srp6.BUILD_8606);
        if (ch.length == 3) {
            return ch[2] & 0xFF;
        }
        WowBuffer in = new WowBuffer(ch);
        in.getU8();
        in.getU8();
        in.getU8();
        byte[] bPub = in.getBytes(32);
        in.getU8();
        in.getU8();
        in.getU8();
        in.getBytes(32);
        byte[] salt = in.getBytes(32);
        in.getBytes(16);
        in.getU8();
        Srp6.Client c = Srp6.clientRespond(username, pass, salt, bPub);
        WowBuffer proof = new WowBuffer(75);
        proof.putU8(Codes.CMD_AUTH_LOGON_PROOF);
        proof.putBytes(c.aPubLe);
        proof.putBytes(c.m1);
        proof.putBytes(new byte[20]);
        proof.putU8(0);
        proof.putU8(0);
        sendAuth(proof.array());
        int cmd = authIn.readUnsignedByte();
        int err = authIn.readUnsignedByte();
        if (err != 0) {
            return err;
        }
        authIn.readFully(new byte[20 + 4 + 4 + 2]);
        sessionKey = c.k;
        return 0;
    }

    byte[] realmList() throws IOException {
        WowBuffer pkt = new WowBuffer(5);
        pkt.putU8(Codes.CMD_REALM_LIST);
        pkt.putU32(0);
        sendAuth(pkt.array());
        int cmd = authIn.readUnsignedByte();
        int size = readU16Le(authIn);
        byte[] body = authIn.readNBytes(size);
        WowBuffer out = new WowBuffer(3 + body.length);
        out.putU8(cmd);
        out.putU16(size);
        out.putBytes(body);
        return out.array();
    }

    int reconnect(String user) throws IOException {
        closeAuth();
        connectAuth();
        username = user.toUpperCase(Locale.ROOT);
        byte[] name = username.getBytes(StandardCharsets.US_ASCII);
        WowBuffer body = new WowBuffer(48 + name.length);
        body.putBytes(new byte[]{'W', 'o', 'W', 0});
        body.putU8(2);
        body.putU8(4);
        body.putU8(3);
        body.putU16(Srp6.BUILD_8606);
        body.putBytes(fourcc("x86"));
        body.putBytes(fourcc("Win"));
        body.putBytes(fourcc("enUS"));
        body.putU32(0);
        body.putU32(0x0100007F);
        body.putU8(name.length);
        body.putBytes(name);
        byte[] b = body.array();
        WowBuffer pkt = new WowBuffer(4 + b.length);
        pkt.putU8(Codes.CMD_AUTH_RECONNECT_CHALLENGE);
        pkt.putU8(0);
        pkt.putU16(b.length);
        pkt.putBytes(b);
        sendAuth(pkt.array());
        int cmd = authIn.readUnsignedByte();
        int err = authIn.readUnsignedByte();
        if (cmd != Codes.CMD_AUTH_RECONNECT_CHALLENGE || err != 0) {
            return err == 0 ? 1 : err;
        }
        byte[] seed = authIn.readNBytes(16);
        authIn.readFully(new byte[16]);
        byte[] r1 = new byte[16];
        Arrays.fill(r1, (byte) 1);
        byte[] r2 = Srp6.reconnectClientR2(username, r1, seed, sessionKey);
        WowBuffer proof = new WowBuffer(57);
        proof.putU8(Codes.CMD_AUTH_RECONNECT_PROOF);
        proof.putBytes(r1);
        proof.putBytes(r2);
        proof.putBytes(new byte[20]);
        sendAuth(proof.array());
        int pcmd = authIn.readUnsignedByte();
        int perr = authIn.readUnsignedByte();
        if (pcmd == Codes.CMD_AUTH_RECONNECT_PROOF) {
            try {
                authIn.readNBytes(2);
            } catch (IOException ignored) {
                // short packet
            }
        }
        return perr;
    }

    void connectWorld() throws IOException {
        world = new Socket();
        world.connect(new InetSocketAddress(worldHost(), worldPort()), 2000);
        world.setSoTimeout(4000);
        worldIn = new DataInputStream(world.getInputStream());
        worldOut = world.getOutputStream();
        Pkt ch = readWorld(false);
        if (ch.opcode != Opcodes.SMSG_AUTH_CHALLENGE) {
            throw new IOException("expected AUTH_CHALLENGE got " + ch.opcode);
        }
        WowBuffer seedBuf = new WowBuffer(ch.payload);
        serverSeed = seedBuf.getU32();
    }

    void authSession() throws IOException {
        int clientSeed = 7;
        WowBuffer proof = new WowBuffer(username.length() + 4 + 4 + 4 + 40);
        proof.putBytes(username.getBytes(StandardCharsets.US_ASCII));
        proof.putU32(0);
        proof.putU32(clientSeed);
        proof.putU32(serverSeed);
        proof.putBytes(sessionKey);
        byte[] digest = Sha1.hash(proof.array());
        WowBuffer in = new WowBuffer(256);
        in.putU32(Srp6.BUILD_8606);
        in.putU32(0);
        in.putCString(username);
        in.putU32(clientSeed);
        in.putBytes(digest);
        WowBuffer add = new WowBuffer(32);
        add.putCString("Blizzard");
        add.putU32(AddonInfo.STANDARD_CRC);
        add.putU32(0);
        add.putU8(0);
        byte[] uncompressed = add.array();
        Deflater def = new Deflater();
        def.setInput(uncompressed);
        def.finish();
        byte[] z = new byte[128];
        int n = def.deflate(z);
        def.end();
        in.putU32(uncompressed.length);
        in.putBytes(Arrays.copyOf(z, n));
        sendWorldRaw(WorldHeader.clientPacket(Opcodes.CMSG_AUTH_SESSION, in.array()), false);
        crypt.init(sessionKey);
        boolean ok = false;
        long deadline = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < deadline) {
            Pkt p = readWorld(true);
            if (p.opcode == Opcodes.SMSG_AUTH_RESPONSE) {
                ok = p.payload.length > 0 && (p.payload[0] & 0xFF) == Codes.AUTH_OK;
                break;
            }
        }
        if (!ok) {
            throw new IOException("world AUTH_RESPONSE not AUTH_OK");
        }
    }

    long enterWorld() throws IOException {
        sendOpcode(Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        Pkt enumer = waitFor(Opcodes.SMSG_CHAR_ENUM, 3000);
        if (enumer == null) {
            throw new IOException("no CHAR_ENUM");
        }
        WowBuffer en = new WowBuffer(enumer.payload);
        int count = en.remaining() > 0 ? en.getU8() : 0;
        if (count == 0) {
            WowBuffer create = new WowBuffer(32);
            create.putCString("Mtx" + Integer.toHexString((int) System.nanoTime() & 0xffff));
            create.putU8(1);
            create.putU8(1);
            create.putU8(0);
            create.putU8(1);
            create.putU8(1);
            create.putU8(1);
            create.putU8(1);
            create.putU8(0);
            create.putU8(0);
            sendOpcode(Opcodes.CMSG_CHAR_CREATE, create.array());
            waitFor(Opcodes.SMSG_CHAR_CREATE, 2000);
            sendOpcode(Opcodes.CMSG_CHAR_ENUM, new byte[0]);
            enumer = waitFor(Opcodes.SMSG_CHAR_ENUM, 3000);
            if (enumer == null) {
                throw new IOException("no CHAR_ENUM after create");
            }
            en = new WowBuffer(enumer.payload);
            count = en.getU8();
        }
        if (count < 1) {
            throw new IOException("no characters");
        }
        playerGuid = en.getU64();
        WowBuffer login = new WowBuffer(8);
        login.putU64(playerGuid);
        sendOpcode(Opcodes.CMSG_PLAYER_LOGIN, login.array());
        waitFor(Opcodes.SMSG_LOGIN_VERIFY_WORLD, 4000);
        drain(200);
        return playerGuid;
    }

    void sendOpcode(int opcode, byte[] payload) throws IOException {
        sendWorldRaw(WorldHeader.clientPacket(opcode, payload), true);
    }

    boolean ping(int seq) throws IOException {
        WowBuffer b = new WowBuffer(8);
        b.putU32(seq);
        b.putU32(0);
        sendOpcode(Opcodes.CMSG_PING, b.array());
        return waitFor(Opcodes.SMSG_PONG, 1500) != null;
    }

    Pkt waitFor(int opcode, int timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            int left = (int) Math.max(50, deadline - System.currentTimeMillis());
            world.setSoTimeout(left);
            try {
                Pkt p = readWorld(true);
                if (p.opcode == opcode) {
                    return p;
                }
            } catch (java.net.SocketTimeoutException e) {
                return null;
            }
        }
        return null;
    }

    void drain(int ms) {
        long deadline = System.currentTimeMillis() + ms;
        try {
            while (System.currentTimeMillis() < deadline) {
                world.setSoTimeout((int) Math.max(20, deadline - System.currentTimeMillis()));
                readWorld(true);
            }
        } catch (Exception ignored) {
            // timeout or closed
        }
        try {
            world.setSoTimeout(4000);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void sendWorldRaw(byte[] pkt, boolean encrypted) throws IOException {
        if (encrypted && crypt.isInitialized() && pkt.length >= 6) {
            crypt.encrypt(pkt, 6);
        }
        worldOut.write(pkt);
        worldOut.flush();
    }

    private Pkt readWorld(boolean encrypted) throws IOException {
        byte[] hdr = worldIn.readNBytes(4);
        if (hdr.length < 4) {
            throw new IOException("short world header");
        }
        if (encrypted && crypt.isInitialized()) {
            crypt.decrypt(hdr, 4);
        }
        int size = ((hdr[0] & 0xFF) << 8) | (hdr[1] & 0xFF);
        int opcode = (hdr[2] & 0xFF) | ((hdr[3] & 0xFF) << 8);
        if (size < 2 || size > WorldHeader.MAX_SIZE) {
            throw new IOException("bad world size " + size);
        }
        int plen = size - 2;
        byte[] payload = plen == 0 ? new byte[0] : worldIn.readNBytes(plen);
        if (payload.length < plen) {
            throw new IOException("short world payload");
        }
        return new Pkt(opcode, payload);
    }

    private static int readU16Le(DataInputStream in) throws IOException {
        int lo = in.readUnsignedByte();
        int hi = in.readUnsignedByte();
        return lo | (hi << 8);
    }

    private void closeAuth() {
        try {
            if (auth != null) {
                auth.close();
            }
        } catch (IOException ignored) {
            // ignore
        }
        auth = null;
    }

    @Override
    public void close() {
        closeAuth();
        try {
            if (world != null) {
                world.close();
            }
        } catch (IOException ignored) {
            // ignore
        }
        world = null;
    }
}
