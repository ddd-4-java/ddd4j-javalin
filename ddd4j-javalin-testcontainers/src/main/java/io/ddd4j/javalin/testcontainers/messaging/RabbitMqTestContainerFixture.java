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
    /**
     * 默认构造方法：Fixture 仅持有镜像与端口常量，容器由 {@link #newContainer()} 构建。
     */
    public RabbitMqTestContainerFixture() {
    }

    /** 默认容器镜像。 */
    public static final String DEFAULT_IMAGE = "rabbitmq:3.13.7-management-alpine";

    @Override
    public RabbitMQContainer newContainer() {
        return new RabbitMQContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    @Override
    protected String resolveConnectionString(RabbitMQContainer container) {
        return container.getAmqpUrl();
    }
}