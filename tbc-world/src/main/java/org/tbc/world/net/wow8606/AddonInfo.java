package org.tbc.world.net.wow8606;

import org.tbc.common.WowBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** spec/03-protocol/packets/addons.md */
public final class AddonInfo {
    public static final int STANDARD_CRC = 0x1c776d01;
    public static final byte[] PUBLIC_KEY = hex("""
            C35B5084B93E32428CD0C748FA0E5D545AA30E14BA9E0DB95D8BEEB684934575\
            FF31FE2F643F3D6D07D9449B408559344E10E1E74369EF7C16FCB4ED1B9528A8\
            2376513157302B790850101C4A1A2CC88B8F052D223DDB5A247A0F1350378F5A\
            CC9E04440E8701D4A315941634C6C2C3FB49FEE1F9DA8C503CBE2CBB57ED46B9\
            AD8BC6DF0ED60FBE80B38B1E77CFAD22CFB74BCFFBF06B11452D7A8118F2927E\
            98565D5E69720A0D030A85A2859CCBFB566E8F44BB8F0222686397BC85BAA8F7\
            B540683C77866F4BD788CA8AD7CE36F0456ED564790F17FC64DD106FF3F5E0A6\
            C3FB1B8C29EF8EE534CBD12ACE79C39A0D36EA01E0AA912054F072D81EC789D2
            """);

    public final String name;
    public final int crc;

    public AddonInfo(String name, int crc) {
        this.name = name;
        this.crc = crc;
    }

    public static List<AddonInfo> inflate(WowBuffer in) {
        if (in.remaining() < 4) {
            throw new IllegalArgumentException("addon blob missing size");
        }
        int uncompressed = in.getU32();
        if (uncompressed == 0 || uncompressed > 0xFFFFF) {
            throw new IllegalArgumentException("bad addon uncompressed size");
        }
        byte[] compressed = in.remainingBytes();
        byte[] raw = new byte[uncompressed];
        Inflater inf = new Inflater();
        inf.setInput(compressed);
        try {
            int n = inf.inflate(raw);
            if (n != uncompressed) {
                throw new IllegalArgumentException("addon inflate short");
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("addon inflate", e);
        } finally {
            inf.end();
        }
        WowBuffer addons = new WowBuffer(raw);
        List<AddonInfo> list = new ArrayList<>();
        while (addons.remaining() > 0) {
            String name = addons.getCString();
            int crc = addons.getU32();
            addons.getU32();
            addons.getU8();
            list.add(new AddonInfo(name, crc));
        }
        return list;
    }

    public static byte[] buildSmsg(List<AddonInfo> addons) {
        WowBuffer out = new WowBuffer(64 + addons.size() * 8);
        for (AddonInfo a : addons) {
            out.putU8(2);
            out.putU8(1);
            boolean sendKey = a.crc != STANDARD_CRC;
            out.putU8(sendKey ? 1 : 0);
            if (sendKey) {
                out.putBytes(PUBLIC_KEY);
            }
            out.putU32(0);
            out.putU8(0);
        }
        return out.array();
    }

    private static byte[] hex(String s) {
        String h = s.replace(" ", "").replace("\n", "").replace("\\", "");
        byte[] o = new byte[h.length() / 2];
        for (int i = 0; i < o.length; i++) {
            o[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        }
        return o;
    }
}
