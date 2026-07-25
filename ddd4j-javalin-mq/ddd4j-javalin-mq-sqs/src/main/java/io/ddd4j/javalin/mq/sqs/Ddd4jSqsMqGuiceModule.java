package io.ddd4j.javalin.mq.sqs;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.sqs.SqsMQClient;
import io.ddd4j.mq.sqs.SqsProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq SqsMQClient (SqsProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jSqsMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final SqsMQClient client;

    public Ddd4jSqsMqGuiceModule(SqsMQClient client) {
        this(client, new SqsProperties());
    }

    public Ddd4jSqsMqGuiceModule(SqsMQClient client, SqsProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(SqsProperties.class).toInstance((SqsProperties) mqProperties());
        bind(SqsMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
