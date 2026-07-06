package io.ddd4j.javalin.mq.mqttmica;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqttmica.spi.MicaMqttMQBrokerAdapter;
import io.ddd4j.mq.mqttmica.spi.MicaMqttProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.dromara.mica.mqtt.core.client.MqttClient;

import java.util.Objects;

/**
 * Javalin mica-mqtt Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jMicaMqttMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final MicaMqttProperties micaMqttProperties;
    private final MqttClient mqttClient;
    private final MicaMqttMQBrokerAdapter brokerAdapter;

    public Ddd4jMicaMqttMqGuiceModule() {
        this(new MicaMqttProperties());
    }

    public Ddd4jMicaMqttMqGuiceModule(MicaMqttProperties micaMqttProperties) {
        this(micaMqttProperties, new Ddd4jMQProperties(), null);
    }

    public Ddd4jMicaMqttMqGuiceModule(MicaMqttProperties micaMqttProperties, Ddd4jMQProperties mqProperties) {
        this(micaMqttProperties, mqProperties, null);
    }

    public Ddd4jMicaMqttMqGuiceModule(MqttClient mqttClient,
                                      MicaMqttProperties micaMqttProperties,
                                      Ddd4jMQProperties mqProperties) {
        this(micaMqttProperties, mqProperties, Objects.requireNonNull(mqttClient, "mqttClient"));
    }

    private Ddd4jMicaMqttMqGuiceModule(MicaMqttProperties micaMqttProperties,
                                       Ddd4jMQProperties mqProperties,
                                       MqttClient mqttClient) {
        super(mqProperties);
        this.micaMqttProperties = Objects.requireNonNull(micaMqttProperties, "micaMqttProperties");
        this.mqttClient = mqttClient;
        this.brokerAdapter = null;
    }

    public Ddd4jMicaMqttMqGuiceModule(MicaMqttMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jMicaMqttMqGuiceModule(MicaMqttMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.micaMqttProperties = new MicaMqttProperties();
        this.mqttClient = null;
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(MicaMqttProperties.class).toInstance(micaMqttProperties);
    }

    @Provides
    @Singleton
    public MicaMqttMQBrokerAdapter micaMqttMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        if (Objects.nonNull(mqttClient)) {
            return new MicaMqttMQBrokerAdapter(mqttClient, micaMqttProperties, mqProperties(), serialization());
        }
        return new MicaMqttMQBrokerAdapter(micaMqttProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(MicaMqttMQBrokerAdapter micaMqttMQBrokerAdapter) {
        return micaMqttMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(MicaMqttMQBrokerAdapter micaMqttMQBrokerAdapter) {
        return micaMqttMQBrokerAdapter.createPublisher(mqProperties());
    }
}
