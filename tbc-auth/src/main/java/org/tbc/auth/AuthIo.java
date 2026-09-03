package org.tbc.auth;

/** Bytes out of an auth TCP session. AuthHandler is the Netty impl. */
interface AuthIo {
    String remoteIp();

    void send(byte[] data);

    void sendAndClose(byte[] data);

    void close();
}
