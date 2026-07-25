package io.ddd4j.javalin.mq.ons;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.ons.OnsMQClient;
import io.ddd4j.mq.ons.OnsProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq OnsMQClient (OnsProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jOnsMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final OnsMQClient client;

    public Ddd4jOnsMqGuiceModule(OnsMQClient client) {
        this(client, new OnsProperties());
    }

    public Ddd4jOnsMqGuiceModule(OnsMQClient client, OnsProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(OnsProperties.class).toInstance((OnsProperties) mqProperties());
        bind(OnsMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
