package io.ddd4j.javalin.mq.rabbit;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rabbit.consumer.RabbitMQConsumerEndpointRegistrar;
import io.ddd4j.mq.rabbit.publisher.RabbitMQEventPublisher;
import io.ddd4j.mq.rabbit.spi.RabbitMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.ApplicationContext;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.rabbit.Ddd4jRabbitMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jRabbitMqGuiceModule extends io.ddd4j.guice.mq.rabbit.Ddd4jRabbitMqGuiceModule {

    public Ddd4jRabbitMqGuiceModule(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    public Ddd4jRabbitMqGuiceModule(RabbitTemplate rabbitTemplate, Ddd4jMQProperties mqProperties) {
        super(rabbitTemplate, mqProperties);
    }

}
