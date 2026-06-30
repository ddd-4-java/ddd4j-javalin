package io.ddd4j.javalin.mq.tdmq;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.tdmq.client.TdmqClient;
import io.ddd4j.mq.tdmq.client.TdmqClientPlaceholder;
import io.ddd4j.mq.tdmq.consumer.TdmqMQConsumerEndpointRegistrar;
import io.ddd4j.mq.tdmq.publisher.TdmqMQEventPublisher;
import io.ddd4j.mq.tdmq.spi.TdmqMQBrokerAdapter;

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
