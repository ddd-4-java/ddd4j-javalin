package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared ActiveMQ Artemis container fixture.
 *
 * <p>Uses {@code apache/activemq-artemis:2.39.0} with TCP waiting on the OpenWire port
 * (61616). No credentials are required by default. The secondary mapped port (8161)
 * exposes the admin UI for debugging.
 */
public class ActiveMqTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {
    /**
     * 默认构造方法：Fixture 仅持有镜像与端口常量，容器由 {@link #newContainer()} 构建。
     */
    public ActiveMqTestContainerFixture() {
    }

    /** 默认容器镜像。 */
    public static final String DEFAULT_IMAGE = "apache/activemq-artemis:2.39.0";
    /** OpenWire 协议端口。 */
    public static final int OPENWIRE_PORT = 61616;
    /** 管理界面端口。 */
    public static final int ADMIN_PORT = 8161;

    @Override
    public GenericContainer<?> newContainer() {
        return new GenericContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withExposedPorts(OPENWIRE_PORT, ADMIN_PORT)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(java.time.Duration.ofMinutes(3)))
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("tcp://%s:%d",
                container.getHost(), container.getMappedPort(OPENWIRE_PORT));
    }
}