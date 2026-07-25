package io.ddd4j.javalin.mq.mqttmica.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for the mica-mqtt broker bridge (Eclipse Mosquitto image).
 * Full client wiring belongs to the sample module.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jMicaMqttMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
            DockerImageName.parse("eclipse-mosquitto:2.0"))
            .withExposedPorts(1883);

    @Test
    void shouldStartMosquittoContainer() {
        MOSQUITTO.start();
        try {
            assertThat(MOSQUITTO.getMappedPort(1883)).isPositive();
        } finally {
            MOSQUITTO.stop();
        }
    }
}