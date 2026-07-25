package io.ddd4j.javalin.data.mybatis.it;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Minimal {@link DataSource} that delegates to {@link DriverManager#getConnection}
 * with the given JDBC URL and credentials. Avoids pulling HikariCP into the
 * test-scope of {@code ddd4j-javalin-data-mybatisplus}.
 */
class SimpleDriverManagerDataSource implements DataSource {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    SimpleDriverManagerDataSource(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override public PrintWriter getLogWriter() throws SQLException { return null; }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { }
    @Override public void setLoginTimeout(int seconds) throws SQLException { }
    @Override public int getLoginTimeout() throws SQLException { return 0; }
    @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { return Logger.getLogger(""); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}