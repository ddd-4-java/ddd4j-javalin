package io.ddd4j.javalin.mq.kafka;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.kafka.mq.KafkaMQBrokerAdapter;
import io.ddd4j.mq.kafka.mq.KafkaMQEventPublisher;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.serialization.MQMessageSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.kafka.Ddd4jKafkaMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jKafkaMqGuiceModule extends io.ddd4j.guice.mq.kafka.Ddd4jKafkaMqGuiceModule {

    public Ddd4jKafkaMqGuiceModule(KafkaTemplate<String, String> kafkaTemplate, ConsumerFactory<String, String> consumerFactory) {
        super(kafkaTemplate, consumerFactory);
    }

    public Ddd4jKafkaMqGuiceModule(KafkaTemplate<String, String> kafkaTemplate, ConsumerFactory<String, String> consumerFactory, Ddd4jMQProperties mqProperties) {
        super(kafkaTemplate, consumerFactory, mqProperties);
    }

}
