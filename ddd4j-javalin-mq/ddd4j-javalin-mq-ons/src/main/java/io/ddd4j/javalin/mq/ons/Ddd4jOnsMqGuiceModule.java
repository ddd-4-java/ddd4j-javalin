package io.ddd4j.javalin.mq.ons;

import com.aliyun.openservices.ons.api.Producer;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.ons.spi.OnsMQBrokerAdapter;
import io.ddd4j.mq.ons.spi.OnsMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;

import java.util.Objects;

/**
 * Javalin ONS Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jOnsMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final OnsMQProperties onsProperties;
    private final Producer producer;
    private final OnsMQBrokerAdapter brokerAdapter;

    public Ddd4jOnsMqGuiceModule(OnsMQProperties onsProperties) {
        this(onsProperties, new Ddd4jMQProperties());
    }

    public Ddd4jOnsMqGuiceModule(OnsMQProperties onsProperties, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.onsProperties = Objects.requireNonNull(onsProperties, "onsProperties");
        this.producer = null;
        this.brokerAdapter = null;
    }

    public Ddd4jOnsMqGuiceModule(Producer producer, OnsMQProperties onsProperties) {
        this(producer, onsProperties, new Ddd4jMQProperties());
    }

    public Ddd4jOnsMqGuiceModule(Producer producer, OnsMQProperties onsProperties, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.onsProperties = Objects.requireNonNull(onsProperties, "onsProperties");
        this.producer = Objects.requireNonNull(producer, "producer");
        this.brokerAdapter = null;
    }

    public Ddd4jOnsMqGuiceModule(OnsMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jOnsMqGuiceModule(OnsMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.onsProperties = new OnsMQProperties();
        this.producer = null;
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(OnsMQProperties.class).toInstance(onsProperties);
    }

    @Provides
    @Singleton
    public OnsMQBrokerAdapter onsMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        if (Objects.nonNull(producer)) {
            return new OnsMQBrokerAdapter(producer, onsProperties, mqProperties(), serialization());
        }
        return new OnsMQBrokerAdapter(onsProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(OnsMQBrokerAdapter onsMQBrokerAdapter) {
        return onsMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(OnsMQBrokerAdapter onsMQBrokerAdapter) {
        return onsMQBrokerAdapter.createPublisher(mqProperties());
    }
}
