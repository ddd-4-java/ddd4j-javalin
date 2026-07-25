package io.ddd4j.javalin.testcontainers;

import org.testcontainers.containers.GenericContainer;

/**
 * Common contract for Testcontainers fixtures used by ddd4j-javalin integration tests.
 *
 * <p>Each fixture encapsulates a single container type, configured with sensible defaults
 * (image, exposed port, waiting strategy) and exposes a {@link #newContainer()} factory
 * method for tests to instantiate a fresh container per test class.
 *
 * <p>Note: we deliberately use {@code GenericContainer<? extends GenericContainer<?>>}
 * as the parameterised type (rather than the usual
 * {@code C extends GenericContainer<C>} self-type idiom) so that subclasses like
 * {@code MySQLContainer<?>} work without requiring them to redeclare the wildcard.
 */
public abstract class AbstractTestContainerFixture<C extends GenericContainer<?>> {

    /**
     * Build a fresh container instance. Tests should keep the returned container as a
     * {@code @Container static} field annotated with
     * {@link org.testcontainers.junit.jupiter.Testcontainers} so that lifecycle management
     * (start / stop / Ryuk cleanup) is handled by JUnit.
     *
     * @return newly constructed, <em>unstarted</em> container.
     */
    public abstract C newContainer();

    /**
     * Build the JDBC URL or equivalent connection string used by data sources, ddd4j Guice
     * Modules, etc. By default delegates to {@link #newContainer()} and reads the host /
     * mapped port; subclasses may override when the connection string has a different shape
     * (e.g. Keycloak realms, Kafka brokers).
     */
    public String connectionString() {
        C container = newContainer();
        container.start();
        try {
            return resolveConnectionString(container);
        } finally {
            container.stop();
        }
    }

    /**
     * Hook for subclasses to format the connection string from the started container.
     */
    protected abstract String resolveConnectionString(C container);
}