package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared RabbitMQ container fixture.
 *
 * <p>Uses the official {@code rabbitmq:3.13.7-management-alpine} image with the default guest/guest
 * credentials. The management UI is exposed on the secondary mapped port (15672) and can be
 * browsed manually during debugging.
 *
 * <p>Connection string is the AMQP URI ({@code amqp://guest:guest@host:port/}) consumed by
 * both the official Java client and {@code ddd4j-javalin-mq-rabbitmq}.
 */
public class RabbitMqTestContainerFixture extends AbstractTestContainerFixture<RabbitMQContainer> {

    /** 默认容器镜像（已钉精确版本）。镜像来源：<a href="https://testcontainers.com/modules/rabbitmq">Testcontainers RabbitMQ 模块</a>。 */
    public static final String DEFAULT_IMAGE = "rabbitmq:3.13.7-management-alpine";

    /**
     * 创建并配置测试容器。
     * @return 配置完成的容器实例。
     */
    @Override
    public RabbitMQContainer newContainer() {
        return new RabbitMQContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    /**
     * 由容器解析连接字符串。
     * @param container 已启动的测试容器。
     * @return 容器连接串。
     */
    @Override
    protected String resolveConnectionString(RabbitMQContainer container) {
        return container.getAmqpUrl();
    }
}