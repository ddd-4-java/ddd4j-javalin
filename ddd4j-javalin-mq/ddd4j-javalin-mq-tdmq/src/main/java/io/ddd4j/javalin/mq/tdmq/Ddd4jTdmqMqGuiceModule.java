package io.ddd4j.javalin.mq.tdmq;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.tdmq.client.TdmqClient;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.tdmq.Ddd4jTdmqMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jTdmqMqGuiceModule extends io.ddd4j.guice.mq.tdmq.Ddd4jTdmqMqGuiceModule {

    public Ddd4jTdmqMqGuiceModule() {
        super();
    }

    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient) {
        super(tdmqClient);
    }

    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient, Ddd4jMQProperties mqProperties) {
        super(tdmqClient, mqProperties);
    }

}
