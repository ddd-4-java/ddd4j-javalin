package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared MariaDB container fixture.
 *
 * <p>Uses {@code mariadb:11.8.9} with {@code ddd4j_test} database and {@code test/test}
 * credentials.
 */
public class MariaDbTestContainerFixture extends AbstractTestContainerFixture<MariaDBContainer<?>> {

    /** 默认容器镜像（钉版；原浮动别名 11 与该补丁标签 digest 一致）。 */
    // 测试镜像来源: https://testcontainers.com/modules/mariadb/
    public static final String DEFAULT_IMAGE = "mariadb:11.8.9";
    /** 默认数据库名。 */
    public static final String DEFAULT_DATABASE = "ddd4j_test";
    /** 默认用户名。 */
    public static final String DEFAULT_USERNAME = "test";
    /** 默认密码。 */
    public static final String DEFAULT_PASSWORD = "test";

    @Override
    public MariaDBContainer<?> newContainer() {
        return new MariaDBContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withDatabaseName(DEFAULT_DATABASE)
                .withUsername(DEFAULT_USERNAME)
                .withPassword(DEFAULT_PASSWORD)
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(MariaDBContainer<?> container) {
        return container.getJdbcUrl();
    }
}