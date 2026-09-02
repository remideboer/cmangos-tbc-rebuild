package org.tbc.common;

import java.math.BigInteger;
import java.util.Arrays;

/** Little-endian unsigned integers matching CMaNGOS BigNumber AsByteArray / SetBinary. */
public final class Bn {
    private Bn() {}

    public static byte[] toLe(BigInteger n, int size) {
        byte[] be = n.toByteArray();
        int start = 0;
        if (be.length > 1 && be[0] == 0) {
            start = 1;
        }
        byte[] le = new byte[size];
        int len = be.length - start;
        for (int i = 0; i < len && i < size; i++) {
            le[i] = be[be.length - 1 - i];
        }
        return le;
    }

    public static byte[] toLeTrim(BigInteger n) {
        if (n.signum() == 0) {
            return new byte[]{0};
        }
        byte[] be = n.toByteArray();
        int start = 0;
        if (be.length > 1 && be[0] == 0) {
            start = 1;
        }
        int len = be.length - start;
        byte[] le = new byte[len];
        for (int i = 0; i < len; i++) {
            le[i] = be[be.length - 1 - i];
        }
        return le;
    }

    public static BigInteger fromLe(byte[] le) {
        byte[] be = new byte[le.length + 1];
        for (int i = 0; i < le.length; i++) {
            be[be.length - 1 - i] = le[i];
        }
        return new BigInteger(be);
    }

    public static BigInteger fromHex(String hex) {
        String h = hex.startsWith("0x") ? hex.substring(2) : hex;
        if (h.length() % 2 == 1) {
            h = "0" + h;
        }
        return new BigInteger(h, 16);
    }

    public static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) {
            sb.append(String.format("%02X", value));
        }
        return sb.toString();
    }

    /** CMaNGOS AsHexStr / SetHexStr: big-endian hex of a LE byte array. */
    public static String leToBeHex(byte[] le) {
        byte[] be = new byte[le.length];
        for (int i = 0; i < le.length; i++) {
            be[i] = le[le.length - 1 - i];
        }
        return toHex(be);
    }

    public static byte[] beHexToLe(String hex, int size) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        return toLe(fromHex(hex.trim()), size);
    }

    public static byte[] fromHexBytes(String hex) {
        String h = hex.replace(" ", "");
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    public static boolean equal(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
