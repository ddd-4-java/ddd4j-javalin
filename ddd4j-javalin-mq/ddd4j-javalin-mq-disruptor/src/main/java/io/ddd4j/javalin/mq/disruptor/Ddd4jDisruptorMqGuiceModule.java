package io.ddd4j.javalin.mq.disruptor;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.disruptor.config.DisruptorMQProperties;

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
