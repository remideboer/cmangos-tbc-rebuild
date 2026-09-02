package org.tbc.common;

/** TBC AuthCrypt: HMAC-SHA1 mixer, not RC4. spec/03-protocol/world-framing-and-crypt.md */
public final class AuthCrypt {
    public static final byte[] RECV_SEED = Bn.fromHexBytes("38A78315F8922530719867B18C04E2AA");
    public static final int CRYPTED_SEND_LEN = 4;
    public static final int CRYPTED_RECV_LEN = 6;

    private byte[] key;
    private int sendI;
    private int sendJ;
    private int recvI;
    private int recvJ;
    private boolean initialized;

    public void init(byte[] k40) {
        this.key = Sha1.hmacSha1(RECV_SEED, k40);
        this.sendI = this.sendJ = this.recvI = this.recvJ = 0;
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void encryptSend(byte[] data) {
        encrypt(data, CRYPTED_SEND_LEN);
    }

    public void decryptRecv(byte[] data) {
        decrypt(data, CRYPTED_RECV_LEN);
    }

    /** Client encrypts the 6-byte C2S header with the send mixer. */
    public void encrypt(byte[] data, int n) {
        if (!initialized || data.length < n) {
            return;
        }
        for (int t = 0; t < n; t++) {
            sendI %= key.length;
            int x = ((data[t] ^ key[sendI]) + sendJ) & 0xFF;
            sendI++;
            data[t] = (byte) x;
            sendJ = x;
        }
    }

    /** Client decrypts the 4-byte S2C header with the recv mixer. */
    public void decrypt(byte[] data, int n) {
        if (!initialized || data.length < n) {
            return;
        }
        for (int t = 0; t < n; t++) {
            recvI %= key.length;
            int orig = data[t] & 0xFF;
            int x = ((orig - recvJ) ^ (key[recvI] & 0xFF)) & 0xFF;
            recvI++;
            recvJ = orig;
            data[t] = (byte) x;
        }
    }
}
