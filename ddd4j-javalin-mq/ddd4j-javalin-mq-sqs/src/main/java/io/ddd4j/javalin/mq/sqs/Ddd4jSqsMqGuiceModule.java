package io.ddd4j.javalin.mq.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.sqs.consumer.SqsMQConsumerEndpointRegistrar;
import io.ddd4j.mq.sqs.publisher.SqsMQEventPublisher;
import io.ddd4j.mq.sqs.spi.SqsMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.sqs.Ddd4jSqsMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jSqsMqGuiceModule extends io.ddd4j.guice.mq.sqs.Ddd4jSqsMqGuiceModule {

    public Ddd4jSqsMqGuiceModule(AmazonSQS amazonSqs, String defaultQueueUrl) {
        super(amazonSqs, defaultQueueUrl);
    }

    public Ddd4jSqsMqGuiceModule(AmazonSQS amazonSqs, String defaultQueueUrl, Ddd4jMQProperties mqProperties) {
        super(amazonSqs, defaultQueueUrl, mqProperties);
    }

}
