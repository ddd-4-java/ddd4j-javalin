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

    /**
     * 获取数据库连接。
     * @return 数据库连接。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * 获取数据库连接。
     * @param username 用户名。
     * @param password 密码。
     * @return 数据库连接。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * 返回日志写入器。
     * @return 日志写入器；未配置时返回 {@code null}。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override public PrintWriter getLogWriter() throws SQLException { return null; }
    /**
     * 设置日志写入器。
     * @param out 日志写入器。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override public void setLogWriter(PrintWriter out) throws SQLException { }
    /**
     * 设置登录超时秒数。
     * @param seconds 登录超时秒数。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override public void setLoginTimeout(int seconds) throws SQLException { }
    /**
     * 返回登录超时秒数。
     * @return 登录超时秒数。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override public int getLoginTimeout() throws SQLException { return 0; }
    /**
     * 返回父级日志记录器。
     * @return 父级日志记录器。
     * @throws SQLFeatureNotSupportedException 出现异常时抛出。
     */
    @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { return Logger.getLogger(""); }
    /**
     * 按接口解包当前对象。
     * @param <T> 类型参数。
     * @param iface 类型字面量。
     * @return 解包后的接口实例；无法解包时返回 {@code null}。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
    /**
     * 判断是否可解包为指定接口。
     * @param iface 类型字面量。
     * @return 可解包返回 {@code true}。
     * @throws SQLException 获取数据库连接失败时抛出。
     */
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}