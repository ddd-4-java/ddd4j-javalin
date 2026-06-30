package io.ddd4j.javalin.mq.rocket;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.rocketmq.RocketMQProperties;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.rocket.Ddd4jRocketMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jRocketMqGuiceModule extends io.ddd4j.guice.mq.rocket.Ddd4jRocketMqGuiceModule {

    public Ddd4jRocketMqGuiceModule() {
        super();
    }

    public Ddd4jRocketMqGuiceModule(RocketMQProperties rocketProperties) {
        super(rocketProperties);
    }

    public Ddd4jRocketMqGuiceModule(RocketMQProperties rocketProperties, Ddd4jMQProperties mqProperties) {
        super(rocketProperties, mqProperties);
    }
}
