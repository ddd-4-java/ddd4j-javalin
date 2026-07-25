package io.ddd4j.javalin.testcontainers.messaging;

import io.ddd4j.javalin.testcontainers.AbstractTestContainerFixture;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared RabbitMQ container fixture.
 *
 * <p>Uses the official {@code rabbitmq:3-management} image with the default guest/guest
 * credentials. The management UI is exposed on the secondary mapped port (15672) and can be
 * browsed manually during debugging.
 *
 * <p>Connection string is the AMQP URI ({@code amqp://guest:guest@host:port/}) consumed by
 * both the official Java client and {@code ddd4j-javalin-mq-rabbitmq}.
 */
public class RabbitMqTestContainerFixture extends AbstractTestContainerFixture<RabbitMQContainer> {

    public static final String DEFAULT_IMAGE = "rabbitmq:3-management";

    @Override
    public RabbitMQContainer newContainer() {
        return new RabbitMQContainer(DockerImageName.parse(DEFAULT_IMAGE)).withReuse(true);
    }

    @Override
    protected String resolveConnectionString(RabbitMQContainer container) {
        return container.getAmqpUrl();
    }
}