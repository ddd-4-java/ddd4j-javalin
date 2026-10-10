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

    /** 默认容器镜像（已钉精确版本）。镜像来源：<a href="https://testcontainers.com/modules/artemis">Testcontainers ActiveMQ Artemis 模块</a>。 */
    public static final String DEFAULT_IMAGE = "apache/activemq-artemis:2.39.0";
    /** OpenWire 协议端口。 */
    public static final int OPENWIRE_PORT = 61616;
    /** 管理控制台端口。 */
    public static final int ADMIN_PORT = 8161;

    /**
     * 创建并配置测试容器。
     * @return 配置完成的容器实例。
     */
    @Override
    public GenericContainer<?> newContainer() {
        return new GenericContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withExposedPorts(OPENWIRE_PORT, ADMIN_PORT)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(java.time.Duration.ofMinutes(3)))
                .withReuse(true);
    }

    /**
     * 由容器解析连接字符串。
     * @param container 已启动的测试容器。
     * @return 容器连接串。
     */
    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("tcp://%s:%d",
                container.getHost(), container.getMappedPort(OPENWIRE_PORT));
    }
}