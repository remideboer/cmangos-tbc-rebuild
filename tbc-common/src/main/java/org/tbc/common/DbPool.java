package org.tbc.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DbPool implements AutoCloseable {
    private final HikariDataSource ds;

    public DbPool(Conf.DbInfo info, String poolName) {
        this(info.jdbcUrl(), info.user(), info.password(), poolName);
    }

    public DbPool(String jdbcUrl, String user, String password, String poolName) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setPoolName(poolName);
        cfg.setMaximumPoolSize(8);
        this.ds = new HikariDataSource(cfg);
    }

    public Connection get() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public void close() {
        ds.close();
    }
}
