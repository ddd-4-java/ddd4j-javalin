package io.ddd4j.javalin.mq.rabbit;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rabbit.RabbitMQBrokerAdapter;
import io.ddd4j.mq.rabbit.RabbitMQProperties;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin RabbitMQ Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jRabbitMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final RabbitMQProperties rabbitProperties;
    private final RabbitMQBrokerAdapter brokerAdapter;

    public Ddd4jRabbitMqGuiceModule() {
        this(new RabbitMQProperties());
    }

    public Ddd4jRabbitMqGuiceModule(RabbitMQProperties rabbitProperties) {
        this(rabbitProperties, new Ddd4jMQProperties());
    }

    public Ddd4jRabbitMqGuiceModule(RabbitMQProperties rabbitProperties, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.rabbitProperties = Objects.requireNonNull(rabbitProperties, "rabbitProperties");
        this.brokerAdapter = null;
    }

    public Ddd4jRabbitMqGuiceModule(RabbitMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jRabbitMqGuiceModule(RabbitMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.rabbitProperties = new RabbitMQProperties();
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(RabbitMQProperties.class).toInstance(rabbitProperties);
    }

    @Provides
    @Singleton
    public RabbitMQBrokerAdapter rabbitMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        return new RabbitMQBrokerAdapter(rabbitProperties, mqProperties(), serialization(), null);
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(RabbitMQBrokerAdapter rabbitMQBrokerAdapter) {
        return rabbitMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(RabbitMQBrokerAdapter rabbitMQBrokerAdapter) {
        return rabbitMQBrokerAdapter.createPublisher(mqProperties());
    }
}
