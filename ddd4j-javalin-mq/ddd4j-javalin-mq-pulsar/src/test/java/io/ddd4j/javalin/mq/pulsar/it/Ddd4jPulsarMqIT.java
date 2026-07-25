package io.ddd4j.javalin.mq.pulsar.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for Pulsar. The official Testcontainers Pulsar module is not
 * yet GA, so we use a plain {@link GenericContainer} with the apachepulsar/pulsar image.
 * Full publish/consume verification belongs to the sample module.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jPulsarMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> PULSAR = new GenericContainer<>(
            DockerImageName.parse("apachepulsar/pulsar:3.2.0"))
            .withExposedPorts(6650, 8080)
            .withCommand("bin/pulsar", "standalone");

    @Test
    void shouldStartPulsarContainer() {
        PULSAR.start();
        try {
            assertThat(PULSAR.getMappedPort(6650)).isPositive();
        } finally {
            PULSAR.stop();
        }
    }
}