package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared ActiveMQ Classic container fixture.
 *
 * <p>Uses {@code apache/activemq-classic:5.18.3} with TCP waiting on the OpenWire port
 * (61616). No credentials are required by default. The secondary mapped port (8161)
 * exposes the admin UI for debugging.
 */
public class ActiveMqTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {

    public static final String DEFAULT_IMAGE = "apache/activemq-classic:5.18.3";
    public static final int OPENWIRE_PORT = 61616;
    public static final int ADMIN_PORT = 8161;

    @Override
    public GenericContainer<?> newContainer() {
        return new GenericContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withExposedPorts(OPENWIRE_PORT, ADMIN_PORT)
                .waitingFor(Wait.forListeningPort())
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("tcp://%s:%d",
                container.getHost(), container.getMappedPort(OPENWIRE_PORT));
    }
}