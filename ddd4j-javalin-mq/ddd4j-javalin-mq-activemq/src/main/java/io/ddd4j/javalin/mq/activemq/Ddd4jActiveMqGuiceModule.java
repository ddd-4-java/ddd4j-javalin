package io.ddd4j.javalin.mq.activemq;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.activemq.config.ActiveMQProperties;
import io.ddd4j.mq.activemq.spi.ActiveMQBrokerAdapter;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin ActiveMQ Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jActiveMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final ActiveMQProperties activeMQProperties;
    private final ActiveMQBrokerAdapter brokerAdapter;

    public Ddd4jActiveMqGuiceModule() {
        this(new ActiveMQProperties());
    }

    public Ddd4jActiveMqGuiceModule(ActiveMQProperties activeMQProperties) {
        this(activeMQProperties, new Ddd4jMQProperties());
    }

    public Ddd4jActiveMqGuiceModule(ActiveMQProperties activeMQProperties, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.activeMQProperties = Objects.requireNonNull(activeMQProperties, "activeMQProperties");
        this.brokerAdapter = null;
    }

    public Ddd4jActiveMqGuiceModule(ActiveMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jActiveMqGuiceModule(ActiveMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.activeMQProperties = new ActiveMQProperties();
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(ActiveMQProperties.class).toInstance(activeMQProperties);
    }

    @Provides
    @Singleton
    public ActiveMQBrokerAdapter activeMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        return new ActiveMQBrokerAdapter(activeMQProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(ActiveMQBrokerAdapter activeMQBrokerAdapter) {
        return activeMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(ActiveMQBrokerAdapter activeMQBrokerAdapter) {
        return activeMQBrokerAdapter.createPublisher(mqProperties());
    }
}
