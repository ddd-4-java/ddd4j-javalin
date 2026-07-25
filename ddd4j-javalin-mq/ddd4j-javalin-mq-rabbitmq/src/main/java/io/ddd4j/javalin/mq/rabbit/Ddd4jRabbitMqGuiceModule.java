package io.ddd4j.javalin.mq.rabbit;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq RabbitMQClient (RabbitMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jRabbitMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final RabbitMQClient client;

    public Ddd4jRabbitMqGuiceModule(RabbitMQClient client) {
        this(client, new RabbitMQProperties());
    }

    public Ddd4jRabbitMqGuiceModule(RabbitMQClient client, RabbitMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(RabbitMQProperties.class).toInstance((RabbitMQProperties) mqProperties());
        bind(RabbitMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
