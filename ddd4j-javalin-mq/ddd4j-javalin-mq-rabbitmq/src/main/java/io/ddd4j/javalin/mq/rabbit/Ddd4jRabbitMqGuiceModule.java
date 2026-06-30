package io.ddd4j.javalin.mq.rabbit;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
