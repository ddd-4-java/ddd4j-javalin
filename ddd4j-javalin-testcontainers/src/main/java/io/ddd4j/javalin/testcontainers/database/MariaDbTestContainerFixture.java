package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared MariaDB container fixture.
 *
 * <p>Uses {@code mariadb:11} with {@code ddd4j_test} database and {@code test/test}
 * credentials.
 */
public class MariaDbTestContainerFixture extends AbstractTestContainerFixture<MariaDBContainer<?>> {

    public static final String DEFAULT_IMAGE = "mariadb:11";
    public static final String DEFAULT_DATABASE = "ddd4j_test";
    public static final String DEFAULT_USERNAME = "test";
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