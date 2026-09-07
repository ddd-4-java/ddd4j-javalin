package io.ddd4j.javalin.testcontainers.cloud;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * LocalStack SQS 测试容器 Fixture。
 *
 * <p>使用 Testcontainers Java 的 LocalStack 专用模块，统一 SQS 集成测试的镜像、
 * 服务启用和端点解析。</p>
 */
public class LocalStackTestContainerFixture extends AbstractTestContainerFixture<LocalStackContainer> {

    public static final String DEFAULT_IMAGE = "localstack/localstack:3.4";

    /**
     * 创建仅启用 SQS 服务的 LocalStack 容器。
     *
     * @return 未启动的 LocalStack 容器
     */
    @Override
    public LocalStackContainer newContainer() {
        return new LocalStackContainer(DockerImageName.parse(DEFAULT_IMAGE))
                .withServices(LocalStackContainer.Service.SQS);
    }

    /**
     * 获取 SQS 服务端点。
     *
     * @param container 已启动的 LocalStack 容器
     * @return SQS endpoint URI 字符串
     */
    @Override
    protected String resolveConnectionString(LocalStackContainer container) {
        return container.getEndpointOverride(LocalStackContainer.Service.SQS).toString();
    }
}
