package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared MySQL container fixture.
 *
 * <p>Uses {@code mysql:8.0.46} with {@code test} database and {@code test/test} credentials by
 * default, exposing port 3306. Waiting strategy is the default JDBC {@code SELECT 1} probe
 * shipped by {@link MySQLContainer}.
 *
 * <p>Typical usage in {@code @Tag("integration")} tests:
 * <pre>{@code
 * @Container static MySQLContainer<?> MYSQL = new MySqlTestContainerFixture().newContainer();
 *
 * @BeforeAll static void start() { MYSQL.start(); }
 * }</pre>
 */
public class MySqlTestContainerFixture extends AbstractTestContainerFixture<MySQLContainer<?>> {

    /** 默认容器镜像（钉版；原浮动别名 8.0 与该补丁标签 digest 一致）。 */
    // 测试镜像来源: https://testcontainers.com/modules/mysql/
    public static final String DEFAULT_IMAGE = "mysql:8.0.46";
    /** 默认数据库名。 */
    public static final String DEFAULT_DATABASE = "ddd4j_test";
    /** 默认用户名。 */
    public static final String DEFAULT_USERNAME = "test";
    /** 默认密码。 */
    public static final String DEFAULT_PASSWORD = "test";

    @Override
    public MySQLContainer<?> newContainer() {
        return new MySQLContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withDatabaseName(DEFAULT_DATABASE)
                .withUsername(DEFAULT_USERNAME)
                .withPassword(DEFAULT_PASSWORD)
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(MySQLContainer<?> container) {
        return container.getJdbcUrl();
    }
}