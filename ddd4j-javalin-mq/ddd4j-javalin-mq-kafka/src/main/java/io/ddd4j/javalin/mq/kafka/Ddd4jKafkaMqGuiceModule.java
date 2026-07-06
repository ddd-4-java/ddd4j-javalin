package io.ddd4j.javalin.mq.kafka;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.kafka.KafkaMQBrokerAdapter;
import io.ddd4j.mq.kafka.KafkaMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.serialization.MQEventSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin Kafka MQ Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jKafkaMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final KafkaMQProperties kafkaProperties;
    private final KafkaMQBrokerAdapter brokerAdapter;

    public Ddd4jKafkaMqGuiceModule() {
        this(new KafkaMQProperties());
    }

    public Ddd4jKafkaMqGuiceModule(KafkaMQProperties kafkaProperties) {
        this(kafkaProperties, new Ddd4jMQProperties());
    }

    public Ddd4jKafkaMqGuiceModule(KafkaMQProperties kafkaProperties, Ddd4jMQProperties mqProperties) {
        this(kafkaProperties, mqProperties, null);
    }

    public Ddd4jKafkaMqGuiceModule(KafkaMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jKafkaMqGuiceModule(KafkaMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.kafkaProperties = new KafkaMQProperties();
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    private Ddd4jKafkaMqGuiceModule(
            KafkaMQProperties kafkaProperties,
            Ddd4jMQProperties mqProperties,
            MQEventSerialization serialization) {
        super(mqProperties, Objects.isNull(serialization)
                ? new io.ddd4j.mq.serialization.JsonMQMessageSerialization()
                : serialization);
        this.kafkaProperties = Objects.requireNonNull(kafkaProperties, "kafkaProperties");
        this.brokerAdapter = null;
    }

    @Override
    protected void configure() {
        super.configure();
        bind(KafkaMQProperties.class).toInstance(kafkaProperties);
    }

    @Provides
    @Singleton
    public KafkaMQBrokerAdapter kafkaMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        return new KafkaMQBrokerAdapter(kafkaProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(KafkaMQBrokerAdapter kafkaMQBrokerAdapter) {
        return kafkaMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(KafkaMQBrokerAdapter kafkaMQBrokerAdapter) {
        return kafkaMQBrokerAdapter.createPublisher(mqProperties());
    }
}
