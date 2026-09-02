package org.tbc.world;

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
import org.tbc.world.net.wow8606.WorldSocket;
import org.tbc.world.world.World;

import java.nio.file.Path;

public final class WorldMain {
    private static final Logger log = LoggerFactory.getLogger(WorldMain.class);

    public static void main(String[] args) throws Exception {
        Path confFile = Path.of(args.length > 0 ? args[0] : "conf/mangosd.conf");
        Conf conf = Conf.load(confFile, "Mangosd_");
        if (conf.getBool("Ra.Enable", false) || conf.getBool("SOAP.Enabled", false)) {
            log.info("RA/SOAP stay off (vision-Out, TP-NEG)");
        }
        DbPool login = new DbPool(conf.db("LoginDatabaseInfo"), "world-login");
        DbPool worldDb = new DbPool(conf.db("WorldDatabaseInfo"), "world-mangos");
        DbPool chars = new DbPool(conf.db("CharacterDatabaseInfo"), "world-chars");
        DbPool logs = new DbPool(conf.db("LogsDatabaseInfo"), "world-logs");
        World world = new World(conf, login, worldDb, chars);
        Thread tick = new Thread(world, "WorldRunnable");
        tick.setDaemon(false);
        tick.start();
        int port = conf.getInt("WorldServerPort", 8085);
        String bind = conf.get("BindIP", "0.0.0.0");
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
                            ch.pipeline().addLast(new WorldSocket(world));
                        }
                    });
            log.info("tbc-world listening {}:{} tick {}ms", bind, port, World.TICK_MS);
            b.bind(bind, port).sync().channel().closeFuture().sync();
        } finally {
            world.stop();
            tick.interrupt();
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            login.close();
            worldDb.close();
            chars.close();
            logs.close();
        }
    }
}
