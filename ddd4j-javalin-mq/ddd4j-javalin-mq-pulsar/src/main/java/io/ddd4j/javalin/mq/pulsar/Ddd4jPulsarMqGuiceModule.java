package io.ddd4j.javalin.mq.pulsar;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.pulsar.consumer.PulsarConsumerEndpointRegistrar;
import io.ddd4j.mq.pulsar.publisher.PulsarMQEventPublisher;
import io.ddd4j.mq.pulsar.spi.PulsarMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.pulsar.core.PulsarTemplate;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.pulsar.Ddd4jPulsarMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jPulsarMqGuiceModule extends io.ddd4j.guice.mq.pulsar.Ddd4jPulsarMqGuiceModule {

    public Ddd4jPulsarMqGuiceModule(PulsarTemplate<String> pulsarTemplate) {
        super(pulsarTemplate);
    }

    public Ddd4jPulsarMqGuiceModule(PulsarTemplate<String> pulsarTemplate, Ddd4jMQProperties mqProperties) {
        super(pulsarTemplate, mqProperties);
    }

}
