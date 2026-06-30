package io.ddd4j.javalin.mq.disruptor;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;
import io.ddd4j.mq.disruptor.consumer.DisruptorMQConsumerEndpointRegistrar;
import io.ddd4j.mq.disruptor.core.DisruptorMQBus;
import io.ddd4j.mq.disruptor.core.DisruptorMQEventDispatcher;
import io.ddd4j.mq.disruptor.spi.DisruptorMQBrokerAdapter;
import io.ddd4j.mq.disruptor.publisher.DisruptorMQEventPublisher;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.disruptor.Ddd4jDisruptorMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jDisruptorMqGuiceModule extends io.ddd4j.guice.mq.disruptor.Ddd4jDisruptorMqGuiceModule {

    public Ddd4jDisruptorMqGuiceModule() {
        super();
    }

    public Ddd4jDisruptorMqGuiceModule(DisruptorMQProperties disruptorProperties, Ddd4jMQProperties mqProperties) {
        super(disruptorProperties, mqProperties);
    }

}
