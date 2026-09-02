package org.tbc.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha1 {
    private Sha1() {}

    public static byte[] hash(byte[]... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (byte[] p : parts) {
                md.update(p);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] hashUtf8(String s) {
        return hash(s.getBytes(StandardCharsets.US_ASCII));
    }

    public static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
