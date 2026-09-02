package org.tbc.world.net.wow8606;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.tbc.common.AuthCrypt;
import org.tbc.common.WorldHeader;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

import java.util.concurrent.ThreadLocalRandom;

public final class WorldSocket extends ChannelInboundHandlerAdapter implements PacketSink {
    private final World world;
    private final AuthCrypt crypt = new AuthCrypt();
    private WorldSession session;
    private ChannelHandlerContext ctx;
    private ByteBuf acc;
    private byte[] pendingHdr;

    public WorldSocket(World world) {
        this.world = world;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.acc = ctx.alloc().buffer();
        int seed = ThreadLocalRandom.current().nextInt();
        session = new WorldSession(this, seed);
        session.sendChallenge();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            acc.writeBytes(buf);
        } finally {
            buf.release();
        }
        while (true) {
            if (pendingHdr == null) {
                if (acc.readableBytes() < WorldHeader.CLIENT_HEADER) {
                    return;
                }
                pendingHdr = new byte[WorldHeader.CLIENT_HEADER];
                acc.readBytes(pendingHdr);
                if (crypt.isInitialized()) {
                    crypt.decryptRecv(pendingHdr);
                }
            }
            int size = ((pendingHdr[0] & 0xFF) << 8) | (pendingHdr[1] & 0xFF);
            int opcode = (pendingHdr[2] & 0xFF) | ((pendingHdr[3] & 0xFF) << 8)
                    | ((pendingHdr[4] & 0xFF) << 16) | ((pendingHdr[5] & 0xFF) << 24);
            if (!WorldHeader.validClientSize(size) || !Opcodes.valid(opcode)) {
                ctx.close();
                return;
            }
            int payloadLen = size - 4;
            if (acc.readableBytes() < payloadLen) {
                return;
            }
            byte[] payload = new byte[payloadLen];
            acc.readBytes(payload);
            acc.discardReadBytes();
            pendingHdr = null;
            if (opcode == Opcodes.CMSG_PING || opcode == Opcodes.CMSG_AUTH_SESSION) {
                session.handle(world, opcode, payload);
            } else {
                world.queuePacket(session, opcode, payload);
            }
        }
    }

    @Override
    public void send(int opcode, byte[] payload) {
        if (ctx == null || !ctx.channel().isActive()) {
            return;
        }
        byte[] pkt = org.tbc.common.WorldHeader.serverPacket(opcode, payload);
        if (crypt.isInitialized()) {
            crypt.encryptSend(pkt);
        }
        ctx.writeAndFlush(ctx.alloc().buffer(pkt.length).writeBytes(pkt));
    }

    @Override
    public void initCrypt(byte[] sessionKey) {
        crypt.init(sessionKey);
    }

    @Override
    public void close() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (session != null && session.player() != null) {
            world.queuePacket(session, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
        }
        if (acc != null) {
            acc.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
