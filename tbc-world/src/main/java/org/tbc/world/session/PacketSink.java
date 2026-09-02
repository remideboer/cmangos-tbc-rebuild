package org.tbc.world.session;

public interface PacketSink {
    void send(int opcode, byte[] payload);

    void close();

    default void initCrypt(byte[] sessionKey) {}
}
