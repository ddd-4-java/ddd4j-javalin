package io.ddd4j.javalin.mq.mqtt.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for MQTT (Eclipse Mosquitto). Full Paho / HiveMQ client wiring
 * belongs to the sample module.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jMqttMqIT {

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