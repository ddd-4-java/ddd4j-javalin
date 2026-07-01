package io.ddd4j.javalin.mq.disruptor;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.consumer.DisruptorMQConsumerEndpointRegistrar;
import io.ddd4j.mq.disruptor.core.DisruptorMQBus;
import io.ddd4j.mq.disruptor.core.DisruptorMQEventDispatcher;
import io.ddd4j.mq.disruptor.spi.DisruptorMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin Disruptor MQ Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jDisruptorMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final DisruptorMQProperties disruptorProperties;
    private final DisruptorMQBrokerAdapter brokerAdapter;

    public Ddd4jDisruptorMqGuiceModule() {
        this(new DisruptorMQProperties());
    }

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQProperties disruptorProperties) {
        this(disruptorProperties, new Ddd4jMQProperties());
    }

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQProperties disruptorProperties, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.disruptorProperties = Objects.requireNonNull(disruptorProperties, "disruptorProperties");
        this.brokerAdapter = null;
    }

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.disruptorProperties = new DisruptorMQProperties();
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(DisruptorMQProperties.class).toInstance(disruptorProperties);
    }

    @Provides
    @Singleton
    public DisruptorMQBrokerAdapter disruptorMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        DisruptorMQEventDispatcher dispatcher = new DisruptorMQEventDispatcher();
        DisruptorMQBus bus = new DisruptorMQBus(disruptorProperties, dispatcher);
        return new DisruptorMQBrokerAdapter(
                bus,
                mqProperties(),
                new DisruptorMQConsumerEndpointRegistrar(bus));
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(DisruptorMQBrokerAdapter disruptorMQBrokerAdapter) {
        return disruptorMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(DisruptorMQBrokerAdapter disruptorMQBrokerAdapter) {
        return disruptorMQBrokerAdapter.createPublisher(mqProperties());
    }
}
