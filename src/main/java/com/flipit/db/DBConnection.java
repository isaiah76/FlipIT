package com.flipit.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Properties;

public class DBConnection {
    private static final HikariDataSource ds;

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/config.properties")) {
            if (in == null) throw new RuntimeException("config.properties not found");

            Properties props = new Properties();
            props.load(in);

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.pass");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(8000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setKeepaliveTime(60000);
            config.setLeakDetectionThreshold(4000);

            config.setConnectionInitSql("SET time_zone = '" + ZoneId.systemDefault().getId() + "';");

            ds = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void closeConnection() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}