package io.ddd4j.javalin.mq.nats;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.nats.consumer.NatsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.nats.spi.NatsMQBrokerAdapter;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.nats.client.Connection;

import java.util.Objects;

/**
 * Javalin NATS Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jNatsMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final Connection connection;
    private final NatsMQBrokerAdapter brokerAdapter;

    public Ddd4jNatsMqGuiceModule(Connection connection) {
        this(connection, new Ddd4jMQProperties());
    }

    public Ddd4jNatsMqGuiceModule(Connection connection, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.connection = Objects.requireNonNull(connection, "connection");
        this.brokerAdapter = null;
    }

    public Ddd4jNatsMqGuiceModule(NatsMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jNatsMqGuiceModule(NatsMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.connection = null;
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Provides
    @Singleton
    public NatsMQBrokerAdapter natsMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        return new NatsMQBrokerAdapter(
                connection,
                mqProperties(),
                new NatsMQConsumerEndpointRegistrar(connection, mqProperties()));
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(NatsMQBrokerAdapter natsMQBrokerAdapter) {
        return natsMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(NatsMQBrokerAdapter natsMQBrokerAdapter) {
        return natsMQBrokerAdapter.createPublisher(mqProperties());
    }
}
