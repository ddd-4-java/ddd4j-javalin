package io.ddd4j.javalin.mq.nats;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.nats.NatsProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq NatsMQClient (NatsProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jNatsMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final NatsMQClient client;

    public Ddd4jNatsMqGuiceModule(NatsMQClient client) {
        this(client, new NatsProperties());
    }

    public Ddd4jNatsMqGuiceModule(NatsMQClient client, NatsProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(NatsProperties.class).toInstance((NatsProperties) mqProperties());
        bind(NatsMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
