package io.ddd4j.javalin.mq.pulsar;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.pulsar.PulsarProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq PulsarMQClient (PulsarProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jPulsarMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final PulsarMQClient client;

    public Ddd4jPulsarMqGuiceModule(PulsarMQClient client) {
        this(client, new PulsarProperties());
    }

    public Ddd4jPulsarMqGuiceModule(PulsarMQClient client, PulsarProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(PulsarProperties.class).toInstance((PulsarProperties) mqProperties());
        bind(PulsarMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
