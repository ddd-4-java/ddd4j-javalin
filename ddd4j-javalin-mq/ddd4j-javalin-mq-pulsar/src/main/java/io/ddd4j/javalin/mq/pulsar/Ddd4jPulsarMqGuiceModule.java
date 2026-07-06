package io.ddd4j.javalin.mq.pulsar;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.pulsar.spi.PulsarMQBrokerAdapter;
import io.ddd4j.mq.pulsar.spi.PulsarMQProperties;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.apache.pulsar.client.api.PulsarClient;

import java.util.Objects;

/**
 * Javalin Pulsar Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jPulsarMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final PulsarMQProperties pulsarProperties;
    private final PulsarClient pulsarClient;
    private final PulsarMQBrokerAdapter brokerAdapter;

    public Ddd4jPulsarMqGuiceModule() {
        this(new PulsarMQProperties());
    }

    public Ddd4jPulsarMqGuiceModule(PulsarMQProperties pulsarProperties) {
        this(pulsarProperties, new Ddd4jMQProperties(), null);
    }

    public Ddd4jPulsarMqGuiceModule(PulsarMQProperties pulsarProperties, Ddd4jMQProperties mqProperties) {
        this(pulsarProperties, mqProperties, null);
    }

    public Ddd4jPulsarMqGuiceModule(PulsarClient pulsarClient,
                                    PulsarMQProperties pulsarProperties,
                                    Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.pulsarProperties = Objects.requireNonNull(pulsarProperties, "pulsarProperties");
        this.pulsarClient = Objects.requireNonNull(pulsarClient, "pulsarClient");
        this.brokerAdapter = null;
    }

    private Ddd4jPulsarMqGuiceModule(PulsarMQProperties pulsarProperties,
                                     Ddd4jMQProperties mqProperties,
                                     PulsarMQBrokerAdapter brokerAdapter) {
        super(mqProperties);
        this.pulsarProperties = Objects.requireNonNull(pulsarProperties, "pulsarProperties");
        this.pulsarClient = null;
        this.brokerAdapter = brokerAdapter;
    }

    public Ddd4jPulsarMqGuiceModule(PulsarMQBrokerAdapter brokerAdapter) {
        this(new PulsarMQProperties(), new Ddd4jMQProperties(), Objects.requireNonNull(brokerAdapter, "brokerAdapter"));
    }

    @Override
    protected void configure() {
        super.configure();
        bind(PulsarMQProperties.class).toInstance(pulsarProperties);
    }

    @Provides
    @Singleton
    public PulsarMQBrokerAdapter pulsarMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        if (Objects.nonNull(pulsarClient)) {
            return new PulsarMQBrokerAdapter(pulsarClient, pulsarProperties, mqProperties(), serialization());
        }
        return new PulsarMQBrokerAdapter(pulsarProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(PulsarMQBrokerAdapter pulsarMQBrokerAdapter) {
        return pulsarMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(PulsarMQBrokerAdapter pulsarMQBrokerAdapter) {
        return pulsarMQBrokerAdapter.createPublisher(mqProperties());
    }
}
