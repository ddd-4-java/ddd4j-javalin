package io.ddd4j.javalin.mq.rocket;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rocketmq.consumer.RocketMQConsumerEndpointRegistrar;
import io.ddd4j.mq.rocketmq.publisher.RocketMQEventPublisher;
import io.ddd4j.mq.rocketmq.spi.RocketMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.ApplicationContext;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.rocket.Ddd4jRocketMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jRocketMqGuiceModule extends io.ddd4j.guice.mq.rocket.Ddd4jRocketMqGuiceModule {

    public Ddd4jRocketMqGuiceModule(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    public Ddd4jRocketMqGuiceModule(RocketMQTemplate rocketMQTemplate, Ddd4jMQProperties mqProperties, RocketMQProperties rocketMQProperties) {
        super(rocketMQTemplate, mqProperties, rocketMQProperties);
    }

}
