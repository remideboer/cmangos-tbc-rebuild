package org.tbc.common;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * WoW SRP6 (not RFC 5054). N/g=7/k=3, interleaved K. Wire integers little-endian.
 * spec/03-protocol/auth-srp6.md
 */
public final class Srp6 {
    public static final BigInteger N = Bn.fromHex("894B645E89E1535BBDAD5B8B290650530801B18EBFBF5E8FAB3C82872A3E9BB7");
    public static final BigInteger G = BigInteger.valueOf(7);
    public static final byte[] VERSION_CHALLENGE = Bn.fromHexBytes("BAA31E99A00B2157FC373FB369CDD2F1");
    public static final int BUILD_8606 = 8606;

    private static final SecureRandom RNG = new SecureRandom();

    /** I = SHA1(USERNAME:PASSWORD). CMaNGOS AccountMgr::normalizeString uppercases both. */
    public static byte[] shaUsernamePassword(String username, String password) {
        return Sha1.hashUtf8(username.toUpperCase(Locale.ROOT) + ":" + password.toUpperCase(Locale.ROOT));
    }

    public static Verifier makeVerifier(String username, String password, byte[] salt) {
        byte[] i = shaUsernamePassword(username, password);
        // CMaNGOS CalculateVerifier: SetHexStr(I) + AsByteArray + reverse-20 is identity for a 20-byte SHA1.
        byte[] xHash = Sha1.hash(salt, i);
        BigInteger x = Bn.fromLe(xHash);
        BigInteger v = G.modPow(x, N);
        return new Verifier(salt, Bn.toLe(v, 32));
    }

    public static Verifier makeVerifier(String username, String password) {
        byte[] salt = new byte[32];
        RNG.nextBytes(salt);
        return makeVerifier(username, password, salt);
    }

    public record Verifier(byte[] salt, byte[] vLe) {}

    public static final class Session {
        public final byte[] salt;
        public final byte[] vLe;
        public final byte[] bLe;
        public final byte[] bPubLe;
        public byte[] aPubLe;
        public byte[] k;
        public byte[] m1;
        public byte[] m2;

        Session(byte[] salt, byte[] vLe, byte[] bLe, byte[] bPubLe) {
            this.salt = salt;
            this.vLe = vLe;
            this.bLe = bLe;
            this.bPubLe = bPubLe;
        }
    }

    public static Session serverChallenge(byte[] salt, byte[] vLe) {
        byte[] bRaw = new byte[19];
        RNG.nextBytes(bRaw);
        BigInteger b = Bn.fromLe(bRaw);
        BigInteger v = Bn.fromLe(vLe);
        BigInteger gmod = G.modPow(b, N);
        BigInteger B = v.multiply(BigInteger.valueOf(3)).add(gmod).mod(N);
        return new Session(salt, vLe, Bn.toLe(b, 19), Bn.toLe(B, 32));
    }

    public static boolean serverSessionKey(Session s, byte[] aPubLe) {
        BigInteger A = Bn.fromLe(aPubLe);
        if (A.signum() == 0 || A.mod(N).signum() == 0) {
            return false;
        }
        s.aPubLe = aPubLe;
        byte[] uHash = Sha1.hash(aPubLe, s.bPubLe);
        BigInteger u = Bn.fromLe(uHash);
        BigInteger v = Bn.fromLe(s.vLe);
        BigInteger b = Bn.fromLe(s.bLe);
        BigInteger S = A.multiply(v.modPow(u, N)).modPow(b, N);
        s.k = interleaveK(Bn.toLe(S, 32));
        return true;
    }

    public static byte[] computeM1(String username, byte[] salt, byte[] aPub, byte[] bPub, byte[] k) {
        byte[] nHash = Sha1.hash(Bn.toLeTrim(N));
        byte[] gHash = Sha1.hash(Bn.toLeTrim(G));
        byte[] ng = new byte[20];
        for (int i = 0; i < 20; i++) {
            ng[i] = (byte) (nHash[i] ^ gHash[i]);
        }
        byte[] uHash = Sha1.hashUtf8(username.toUpperCase(Locale.ROOT));
        return Sha1.hash(ng, uHash, salt, aPub, bPub, k);
    }

    public static boolean proofM1(Session s, String username, byte[] clientM1) {
        s.m1 = computeM1(username, s.salt, s.aPubLe, s.bPubLe, s.k);
        s.m2 = Sha1.hash(s.aPubLe, s.m1, s.k);
        return Bn.equal(s.m1, clientM1);
    }

    /** Client side for tests: a, A, S, K, M1. */
    public static final class Client {
        public byte[] aLe;
        public byte[] aPubLe;
        public byte[] k;
        public byte[] m1;
        public byte[] m2;
    }

    public static Client clientRespond(String username, String password, byte[] salt, byte[] bPubLe) {
        byte[] aRaw = new byte[19];
        RNG.nextBytes(aRaw);
        BigInteger a = Bn.fromLe(aRaw);
        BigInteger A = G.modPow(a, N);
        byte[] aPub = Bn.toLe(A, 32);
        byte[] i = shaUsernamePassword(username, password);
        byte[] xHash = Sha1.hash(salt, i);
        BigInteger x = Bn.fromLe(xHash);
        byte[] uHash = Sha1.hash(aPub, bPubLe);
        BigInteger u = Bn.fromLe(uHash);
        BigInteger B = Bn.fromLe(bPubLe);
        BigInteger gx = G.modPow(x, N);
        BigInteger kgx = gx.multiply(BigInteger.valueOf(3)).mod(N);
        BigInteger base = B.subtract(kgx).mod(N);
        BigInteger exp = a.add(u.multiply(x));
        BigInteger S = base.modPow(exp, N);
        Client c = new Client();
        c.aLe = Bn.toLe(a, 19);
        c.aPubLe = aPub;
        c.k = interleaveK(Bn.toLe(S, 32));
        c.m1 = computeM1(username, salt, aPub, bPubLe, c.k);
        c.m2 = Sha1.hash(aPub, c.m1, c.k);
        return c;
    }

    public static byte[] reconnectClientR2(String username, byte[] r1, byte[] reconnectProof, byte[] k) {
        return Sha1.hash(username.toUpperCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                r1, reconnectProof, k);
    }

    static byte[] interleaveK(byte[] s32) {
        byte[] even = new byte[16];
        byte[] odd = new byte[16];
        for (int i = 0; i < 16; i++) {
            even[i] = s32[i * 2];
            odd[i] = s32[i * 2 + 1];
        }
        byte[] he = Sha1.hash(even);
        byte[] ho = Sha1.hash(odd);
        byte[] k = new byte[40];
        for (int i = 0; i < 20; i++) {
            k[i * 2] = he[i];
            k[i * 2 + 1] = ho[i];
        }
        return k;
    }
}
