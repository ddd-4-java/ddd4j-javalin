package io.ddd4j.javalin.mq.rocket.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.RocketMqTestContainerFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for RocketMQ. The full publish / consume round-trip requires
 * Apache RocketMQ client wiring (delegate to {@code ddd4j-javalin-sample-mq-rocketmq} once
 * implemented). This class proves the Testcontainers wiring is sound.
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jRocketMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> ROCKETMQ = new RocketMqTestContainerFixture().newContainer();

    @Test
    void shouldStartRocketMqContainer() {
        ROCKETMQ.start();
        try {
            assertThat(ROCKETMQ.getMappedPort(RocketMqTestContainerFixture.NAMESRV_PORT)).isPositive();
        } finally {
            ROCKETMQ.stop();
        }
    }
}