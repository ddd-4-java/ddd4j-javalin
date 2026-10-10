package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared ActiveMQ Artemis container fixture.
 *
 * <p>Uses the official Testcontainers ActiveMQ 模块的 {@link ArtemisContainer}（{@code org.testcontainers:activemq}）
 * with TCP waiting on the OpenWire port (61616). No credentials are required by default
 * （模块默认 {@code artemis/artemis}，由 {@link ArtemisContainer#configure()} 注入
 * {@code ARTEMIS_USER}/{@code ARTEMIS_PASSWORD} 环境变量）。The secondary mapped port (8161)
 * exposes the admin UI for debugging.
 *
 * <p>构造后链式覆盖模块默认的 7 端口暴露与日志等待策略，保持与历史 GenericContainer 行为一致：
 * 仅暴露 61616/8161，端口监听等待 3 分钟。
 */
// 测试镜像来源: https://testcontainers.com/modules/artemis/
public class ActiveMqTestContainerFixture extends AbstractTestContainerFixture<ArtemisContainer> {

    /** 默认容器镜像（已钉补丁标签，非浮动）。 */
    public static final String DEFAULT_IMAGE = "apache/activemq-artemis:2.39.0";
    /** OpenWire 协议端口。 */
    public static final int OPENWIRE_PORT = 61616;
    /** 管理控制台端口。 */
    public static final int ADMIN_PORT = 8161;

    @Override
    public ArtemisContainer newContainer() {
        return new ArtemisContainer(DockerImageName.parse(DEFAULT_IMAGE))
                .withExposedPorts(OPENWIRE_PORT, ADMIN_PORT)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(java.time.Duration.ofMinutes(3)))
                .withReuse(true);
    }

    @Override
    protected String resolveConnectionString(ArtemisContainer container) {
        return String.format("tcp://%s:%d",
                container.getHost(), container.getMappedPort(OPENWIRE_PORT));
    }
}
