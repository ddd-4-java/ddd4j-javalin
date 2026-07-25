package io.ddd4j.javalin.mq.nats.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for NATS. Full client wiring belongs to the sample module.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jNatsMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> NATS = new GenericContainer<>(
            DockerImageName.parse("nats:2-alpine"))
            .withExposedPorts(4222);

    @Test
    void shouldStartNatsContainer() {
        NATS.start();
        try {
            assertThat(NATS.getMappedPort(4222)).isPositive();
        } finally {
            NATS.stop();
        }
    }
}