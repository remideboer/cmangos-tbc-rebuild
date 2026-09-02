package org.tbc.auth;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.Conf;
import org.tbc.common.DbPool;

import java.nio.file.Path;

public final class AuthMain {
    private static final Logger log = LoggerFactory.getLogger(AuthMain.class);

    public static void main(String[] args) throws Exception {
        Path confFile = Path.of(args.length > 0 ? args[0] : "conf/realmd.conf");
        Conf conf = Conf.load(confFile, "Realmd_");
        int port = conf.getInt("RealmServerPort", 3724);
        String bind = conf.get("BindIP", "0.0.0.0");
        DbPool db = new DbPool(conf.db("LoginDatabaseInfo"), "auth-login");
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new AuthHandler(db));
                        }
                    });
            log.info("tbc-auth listening {}:{}", bind, port);
            b.bind(bind, port).sync().channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            db.close();
        }
    }
}
