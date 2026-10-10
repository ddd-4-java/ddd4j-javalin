package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostgreSQL container fixture.
 *
 * <p>Uses {@code postgres:16.15-alpine} with {@code ddd4j_test} database and
 * {@code test/test} credentials, exposing port 5432.
 */
public class PostgresTestContainerFixture extends AbstractTestContainerFixture<PostgreSQLContainer<?>> {

    /** 默认容器镜像（钉版；原浮动别名 16-alpine 与该补丁标签 digest 一致）。 */
    // 测试镜像来源: https://testcontainers.com/modules/postgresql/
    public static final String DEFAULT_IMAGE = "postgres:16.15-alpine";
    /** 默认数据库名。 */
    public static final String DEFAULT_DATABASE = "ddd4j_test";
    /** 默认用户名。 */
    public static final String DEFAULT_USERNAME = "test";
    /** 默认密码。 */
    public static final String DEFAULT_PASSWORD = "test";

    @Override
    public PostgreSQLContainer<?> newContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withDatabaseName(DEFAULT_DATABASE)
                .withUsername(DEFAULT_USERNAME)
                .withPassword(DEFAULT_PASSWORD)
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(PostgreSQLContainer<?> container) {
        return container.getJdbcUrl();
    }
}