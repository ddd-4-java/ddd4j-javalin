package io.ddd4j.javalin.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Shared WireMock container fixture. Uses a plain {@link GenericContainer} so the
 * module doesn't pull in the separate {@code org.wiremock:wiremock-testcontainers-module}
 * dependency; the official {@code wiremock/wiremock} image is used verbatim and exposes
 * port 8080.
 *
 * <p>For most stubbing scenarios prefer the {@code wiremock-standalone} Java client
 * (which is included as a test dependency) to interact with the running container's
 * REST admin endpoint at {@code http://host:8080/__admin}.
 */
public class WireMockTestContainerFixture extends AbstractTestContainerFixture<GenericContainer<?>> {

    /** 默认容器镜像（已钉精确版本）。镜像来源：<a href="https://testcontainers.com/modules/wiremock">Testcontainers WireMock 模块</a>。 */
    public static final String DEFAULT_IMAGE = "wiremock/wiremock:3.5.0";
    /** 默认端口。 */
    public static final int DEFAULT_PORT = 8080;

    /**
     * 创建并配置测试容器。
     * @return 配置完成的容器实例。
     */
    @Override
    public GenericContainer<?> newContainer() {
        return new GenericContainer<>(DEFAULT_IMAGE)
                .withExposedPorts(DEFAULT_PORT)
                .waitingFor(Wait.forListeningPort())
                .withReuse(true);
    }

    /**
     * 由容器解析连接字符串。
     * @param container 已启动的测试容器。
     * @return 容器连接串。
     */
    @Override
    protected String resolveConnectionString(GenericContainer<?> container) {
        return String.format("http://%s:%d",
                container.getHost(), container.getMappedPort(DEFAULT_PORT));
    }
}