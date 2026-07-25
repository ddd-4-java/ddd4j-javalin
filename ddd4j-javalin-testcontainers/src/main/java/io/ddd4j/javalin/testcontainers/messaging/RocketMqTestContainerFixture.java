package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared RocketMQ container fixture (namesrv + broker single container).
 *
 * <p>Uses the official {@code apache/rocketmq:5.1.0} image, exposing namesrv (9876) and
 * broker (10909/10911/10912) ports. The default broker config from the upstream image is
 * used.
 *
 * <p>The {@code connectionString()} returns the {@code host:port} pair for the namesrv
 * because {@code ddd4j-javalin-mq-rocketmq} resolves the namesrv address.
 */
public class RocketMqTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {

    public static final String DEFAULT_IMAGE = "apache/rocketmq:5.1.0";
    public static final int NAMESRV_PORT = 9876;
    public static final int BROKER_VIP_PORT = 10909;

    @Override
    public GenericContainer<?> newContainer() {
        return new GenericContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withExposedPorts(NAMESRV_PORT, BROKER_VIP_PORT)
                .waitingFor(Wait.forListeningPort())
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("%s:%d", container.getHost(),
                container.getMappedPort(NAMESRV_PORT));
    }
}