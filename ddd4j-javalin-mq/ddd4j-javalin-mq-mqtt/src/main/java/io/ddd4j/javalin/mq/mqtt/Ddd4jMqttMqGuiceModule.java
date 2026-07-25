package io.ddd4j.javalin.mq.mqtt;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.mqtt.MqttMQClient;
import io.ddd4j.mq.mqtt.MqttMQProperties;
import java.util.Objects;

/**
 * Javalin Guice module wiring the ddd4j-mq MqttMQClient (MqttMQProperties) as a singleton
 * {{@link MQClient}}.
 *
 * <p>Aligned with ddd4j-mq 2.0.x's single-MQClient-per-broker contract.
 */
public class Ddd4jMqttMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final MqttMQClient client;

    public Ddd4jMqttMqGuiceModule(MqttMQClient client) {
        this(client, new MqttMQProperties());
    }

    public Ddd4jMqttMqGuiceModule(MqttMQClient client, MqttMQProperties properties) {
        super(properties);
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(MqttMQProperties.class).toInstance((MqttMQProperties) mqProperties());
        bind(MqttMQClient.class).toInstance(client);
    }

    @Provides
    @Singleton
    public MQClient mqClient() {
        return client;
    }
}
