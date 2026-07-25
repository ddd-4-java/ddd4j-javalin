package io.ddd4j.javalin.mq.kafka;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq KafkaMQClient (KafkaMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jKafkaMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final KafkaMQClient client;

    public Ddd4jKafkaMqGuiceModule(KafkaMQClient client) {
        this(client, new KafkaMQProperties());
    }

    public Ddd4jKafkaMqGuiceModule(KafkaMQClient client, KafkaMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(KafkaMQProperties.class).toInstance((KafkaMQProperties) mqProperties());
        bind(KafkaMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
