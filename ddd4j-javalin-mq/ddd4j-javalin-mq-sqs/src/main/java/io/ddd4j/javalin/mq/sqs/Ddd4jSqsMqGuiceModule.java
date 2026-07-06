package io.ddd4j.javalin.mq.sqs;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.sqs.spi.SqsBrokerAdapter;
import io.ddd4j.mq.sqs.spi.SqsMQProperties;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Objects;

/**
 * Javalin SQS Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jSqsMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final SqsMQProperties sqsProperties;
    private final SqsClient sqsClient;
    private final SqsBrokerAdapter brokerAdapter;

    public Ddd4jSqsMqGuiceModule() {
        this(new SqsMQProperties());
    }

    public Ddd4jSqsMqGuiceModule(SqsMQProperties sqsProperties) {
        this(sqsProperties, new Ddd4jMQProperties(), null);
    }

    public Ddd4jSqsMqGuiceModule(SqsMQProperties sqsProperties, Ddd4jMQProperties mqProperties) {
        this(sqsProperties, mqProperties, null);
    }

    public Ddd4jSqsMqGuiceModule(SqsClient sqsClient, SqsMQProperties sqsProperties, Ddd4jMQProperties mqProperties) {
        this(sqsProperties, mqProperties, Objects.requireNonNull(sqsClient, "sqsClient"));
    }

    private Ddd4jSqsMqGuiceModule(SqsMQProperties sqsProperties,
                                  Ddd4jMQProperties mqProperties,
                                  SqsClient sqsClient) {
        super(mqProperties);
        this.sqsProperties = Objects.requireNonNull(sqsProperties, "sqsProperties");
        this.sqsClient = sqsClient;
        this.brokerAdapter = null;
    }

    public Ddd4jSqsMqGuiceModule(SqsBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jSqsMqGuiceModule(SqsBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.sqsProperties = new SqsMQProperties();
        this.sqsClient = null;
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(SqsMQProperties.class).toInstance(sqsProperties);
    }

    @Provides
    @Singleton
    public SqsBrokerAdapter sqsBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        if (Objects.nonNull(sqsClient)) {
            return new SqsBrokerAdapter(sqsClient, sqsProperties, mqProperties(), serialization());
        }
        return new SqsBrokerAdapter(sqsProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(SqsBrokerAdapter sqsBrokerAdapter) {
        return sqsBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(SqsBrokerAdapter sqsBrokerAdapter) {
        return sqsBrokerAdapter.createPublisher(mqProperties());
    }
}
