package io.ddd4j.javalin.mq.sqs.it;

import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skeleton integration test for AWS SQS. The official Testcontainers SQS module is not GA,
 * so we use the LocalStack community image as a stand-in. Full publish/consume verification
 * requires LocalStack + AWS SDK wiring (sample module).
 */
@Tag("integration")
@JunitJupiterTestContainers
class Ddd4jSqsMqIT {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> LOCALSTACK = new GenericContainer<>(
            DockerImageName.parse("localstack/localstack:3.4"))
            .withEnv("SERVICES", "sqs")
            .withExposedPorts(4566);

    @Test
    void shouldStartLocalstackContainer() {
        LOCALSTACK.start();
        try {
            assertThat(LOCALSTACK.getMappedPort(4566)).isPositive();
        } finally {
            LOCALSTACK.stop();
        }
    }
}