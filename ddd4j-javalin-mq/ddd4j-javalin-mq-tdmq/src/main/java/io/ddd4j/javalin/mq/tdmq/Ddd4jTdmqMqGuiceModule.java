package io.ddd4j.javalin.mq.tdmq;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.tdmq.client.TdmqClient;
import io.ddd4j.mq.tdmq.client.TdmqClientPlaceholder;
import io.ddd4j.mq.tdmq.spi.TdmqMQBrokerAdapter;
import io.ddd4j.mq.tdmq.spi.TdmqMQProperties;

/**
 * Javalin TDMQ Guice module。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jTdmqMqGuiceModule extends AbstractModule {

    private final TdmqClient tdmqClient;
    private final Ddd4jMQProperties mqProperties;
    private final TdmqMQProperties tdmqProperties;

    public Ddd4jTdmqMqGuiceModule() {
        this(new TdmqClientPlaceholder(), new Ddd4jMQProperties(), new TdmqMQProperties());
    }

    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient) {
        this(tdmqClient, new Ddd4jMQProperties(), new TdmqMQProperties());
    }

    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient, Ddd4jMQProperties mqProperties) {
        this(tdmqClient, mqProperties, new TdmqMQProperties());
    }

    public Ddd4jTdmqMqGuiceModule(TdmqClient tdmqClient,
                                  Ddd4jMQProperties mqProperties,
                                  TdmqMQProperties tdmqProperties) {
        this.tdmqClient = tdmqClient;
        this.mqProperties = mqProperties;
        this.tdmqProperties = tdmqProperties;
    }

    @Override
    protected void configure() {
        bind(TdmqClient.class).toInstance(tdmqClient);
        bind(Ddd4jMQProperties.class).toInstance(mqProperties);
        bind(TdmqMQProperties.class).toInstance(tdmqProperties);
    }

    @Provides
    @Singleton
    public TdmqMQBrokerAdapter tdmqMQBrokerAdapter(TdmqClient tdmqClient,
                                                   Ddd4jMQProperties mqProperties,
                                                   TdmqMQProperties tdmqProperties) {
        return new TdmqMQBrokerAdapter(tdmqClient, tdmqProperties, mqProperties);
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(TdmqMQBrokerAdapter tdmqMQBrokerAdapter) {
        return tdmqMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(TdmqMQBrokerAdapter tdmqMQBrokerAdapter,
                                             Ddd4jMQProperties mqProperties) {
        return tdmqMQBrokerAdapter.createPublisher(mqProperties);
    }
}
