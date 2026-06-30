package io.ddd4j.javalin.mq.nats;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.nats.consumer.NatsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.nats.publisher.NatsMQEventPublisher;
import io.ddd4j.mq.nats.spi.NatsMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.nats.client.Connection;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.nats.Ddd4jNatsMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jNatsMqGuiceModule extends io.ddd4j.guice.mq.nats.Ddd4jNatsMqGuiceModule {

    public Ddd4jNatsMqGuiceModule(Connection connection) {
        super(connection);
    }

    public Ddd4jNatsMqGuiceModule(Connection connection, Ddd4jMQProperties mqProperties) {
        super(connection, mqProperties);
    }

}
