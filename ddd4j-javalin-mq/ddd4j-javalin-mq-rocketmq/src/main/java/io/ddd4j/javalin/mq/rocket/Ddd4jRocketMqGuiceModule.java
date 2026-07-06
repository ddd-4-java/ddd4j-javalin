package io.ddd4j.javalin.mq.rocket;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.rocketmq.RocketMQBrokerAdapter;
import io.ddd4j.mq.rocketmq.RocketMQProperties;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin RocketMQ Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jRocketMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final RocketMQProperties rocketProperties;
    private final RocketMQBrokerAdapter brokerAdapter;

    public Ddd4jRocketMqGuiceModule() {
        this(new RocketMQProperties());
    }

    public Ddd4jRocketMqGuiceModule(RocketMQProperties rocketProperties) {
        this(rocketProperties, new Ddd4jMQProperties());
    }

    public Ddd4jRocketMqGuiceModule(RocketMQProperties rocketProperties, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.rocketProperties = Objects.requireNonNull(rocketProperties, "rocketProperties");
        this.brokerAdapter = null;
    }

    public Ddd4jRocketMqGuiceModule(RocketMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jRocketMqGuiceModule(RocketMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.rocketProperties = new RocketMQProperties();
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(RocketMQProperties.class).toInstance(rocketProperties);
    }

    @Provides
    @Singleton
    public RocketMQBrokerAdapter rocketMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        return new RocketMQBrokerAdapter(rocketProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(RocketMQBrokerAdapter rocketMQBrokerAdapter) {
        return rocketMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(RocketMQBrokerAdapter rocketMQBrokerAdapter) {
        return rocketMQBrokerAdapter.createPublisher(mqProperties());
    }
}
