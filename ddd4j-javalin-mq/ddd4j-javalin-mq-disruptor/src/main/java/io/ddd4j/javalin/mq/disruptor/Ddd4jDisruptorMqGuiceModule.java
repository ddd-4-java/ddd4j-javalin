package io.ddd4j.javalin.mq.disruptor;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.disruptor.DisruptorMQClient;
import io.ddd4j.mq.disruptor.DisruptorMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq DisruptorMQClient (DisruptorMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jDisruptorMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final DisruptorMQClient client;

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQClient client) {
        this(client, new DisruptorMQProperties());
    }

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQClient client, DisruptorMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(DisruptorMQProperties.class).toInstance((DisruptorMQProperties) mqProperties());
        bind(DisruptorMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
