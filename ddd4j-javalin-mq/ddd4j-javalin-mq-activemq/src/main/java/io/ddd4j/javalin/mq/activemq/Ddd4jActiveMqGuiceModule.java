package io.ddd4j.javalin.mq.activemq;

import io.ddd4j.mq.activemq.consumer.ActiveMQConsumerEndpointRegistrar;
import io.ddd4j.mq.activemq.publisher.ActiveMQEventPublisher;
import io.ddd4j.mq.activemq.spi.ActiveMQBrokerAdapter;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.activemq.Ddd4jActiveMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jActiveMqGuiceModule extends io.ddd4j.guice.mq.activemq.Ddd4jActiveMqGuiceModule {

    public Ddd4jActiveMqGuiceModule(JmsTemplate jmsTemplate) {
        super(jmsTemplate);
    }

    public Ddd4jActiveMqGuiceModule(JmsTemplate jmsTemplate, Ddd4jMQProperties mqProperties) {
        super(jmsTemplate, mqProperties);
    }

}
