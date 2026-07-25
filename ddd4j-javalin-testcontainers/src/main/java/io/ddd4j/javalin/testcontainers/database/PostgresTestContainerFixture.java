package io.ddd4j.javalin.testcontainers.database;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostgreSQL container fixture.
 *
 * <p>Uses {@code postgres:16-alpine} with {@code ddd4j_test} database and
 * {@code test/test} credentials, exposing port 5432.
 */
public class PostgresTestContainerFixture extends AbstractTestContainerFixture<PostgreSQLContainer<?>> {

    public static final String DEFAULT_IMAGE = "postgres:16-alpine";
    public static final String DEFAULT_DATABASE = "ddd4j_test";
    public static final String DEFAULT_USERNAME = "test";
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