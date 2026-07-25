package io.ddd4j.javalin.mq.mqttmica;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.mqttmica.MicaMqttMQClient;
import io.ddd4j.mq.mqttmica.MicaMqttProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq MicaMqttMQClient (MicaMqttProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jMqttMicaMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final MicaMqttMQClient client;

    public Ddd4jMqttMicaMqGuiceModule(MicaMqttMQClient client) {
        this(client, new MicaMqttProperties());
    }

    public Ddd4jMqttMicaMqGuiceModule(MicaMqttMQClient client, MicaMqttProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(MicaMqttProperties.class).toInstance((MicaMqttProperties) mqProperties());
        bind(MicaMqttMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
