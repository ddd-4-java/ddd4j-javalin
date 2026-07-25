package io.ddd4j.javalin.mq.activemq;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.activemq.ActiveMQClient;
import io.ddd4j.mq.activemq.ActiveMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq ActiveMQClient (ActiveMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jActiveMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final ActiveMQClient client;

    public Ddd4jActiveMqGuiceModule(ActiveMQClient client) {
        this(client, new ActiveMQProperties());
    }

    public Ddd4jActiveMqGuiceModule(ActiveMQClient client, ActiveMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(ActiveMQProperties.class).toInstance((ActiveMQProperties) mqProperties());
        bind(ActiveMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
