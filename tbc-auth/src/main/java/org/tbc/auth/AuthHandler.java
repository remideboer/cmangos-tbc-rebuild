package org.tbc.auth;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.DbPool;
import org.tbc.common.WowBuffer;

import java.net.InetSocketAddress;

final class AuthHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private final DbPool db;
    private AuthSession session;
    private ChannelHandlerContext ctx;
    private final ByteBuf acc;

    AuthHandler(DbPool db, io.netty.buffer.ByteBufAllocator alloc) {
        this.db = db;
        this.acc = alloc.buffer();
    }

    AuthHandler(DbPool db) {
        this.db = db;
        this.acc = io.netty.buffer.Unpooled.buffer();
    }

    String remoteIp() {
        if (ctx == null) {
            return "0.0.0.0";
        }
        InetSocketAddress a = (InetSocketAddress) ctx.channel().remoteAddress();
        return a.getAddress().getHostAddress();
    }

    void send(byte[] data) {
        send(data, false);
    }

    void sendAndClose(byte[] data) {
        send(data, true);
    }

    private void send(byte[] data, boolean closeAfter) {
        if (ctx == null || !ctx.channel().isActive()) {
            return;
        }
        var future = ctx.writeAndFlush(ctx.alloc().buffer(data.length).writeBytes(data));
        if (closeAfter) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    void close() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.session = new AuthSession(db, this);
        log.info("auth connect {}", remoteIp());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            acc.writeBytes(buf);
        } finally {
            buf.release();
        }
        byte[] copy = new byte[acc.readableBytes()];
        acc.getBytes(acc.readerIndex(), copy);
        WowBuffer in = new WowBuffer(copy);
        int before = in.remaining();
        session.onBytes(in);
        int consumed = before - in.remaining();
        if (consumed > 0) {
            acc.skipBytes(consumed);
            acc.discardReadBytes();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        acc.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
