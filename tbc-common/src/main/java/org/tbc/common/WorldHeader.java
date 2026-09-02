package org.tbc.common;

public final class WorldHeader {
    public static final int CLIENT_HEADER = 6;
    public static final int SERVER_HEADER = 4;
    public static final int MAX_SIZE = 0x2800;
    public static final int NUM_MSG_TYPES = 0x424;

    private WorldHeader() {}

    public static void writeServer(WowBuffer out, int opcode, byte[] payload) {
        int size = 2 + (payload == null ? 0 : payload.length);
        out.putU16BE(size);
        out.putU16(opcode);
        if (payload != null && payload.length > 0) {
            out.putBytes(payload);
        }
    }

    public static byte[] serverPacket(int opcode, byte[] payload) {
        WowBuffer b = new WowBuffer(4 + (payload == null ? 0 : payload.length));
        writeServer(b, opcode, payload);
        return b.array();
    }

    /** ClientPktHeader: size BE = 4 + payload, opcode LE uint32. */
    public static byte[] clientPacket(int opcode, byte[] payload) {
        int plen = payload == null ? 0 : payload.length;
        WowBuffer b = new WowBuffer(CLIENT_HEADER + plen);
        b.putU16BE(4 + plen);
        b.putU32(opcode);
        if (plen > 0) {
            b.putBytes(payload);
        }
        return b.array();
    }

    public static boolean validClientSize(int sizeBe) {
        return sizeBe >= 4 && sizeBe <= MAX_SIZE;
    }
}
