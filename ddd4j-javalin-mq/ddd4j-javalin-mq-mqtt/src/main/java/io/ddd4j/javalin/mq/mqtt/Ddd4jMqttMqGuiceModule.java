package io.ddd4j.javalin.mq.mqtt;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.javalin.mq.core.AbstractDdd4jMqGuiceModule;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqtt.spi.MqttMQBrokerAdapter;
import io.ddd4j.mq.mqtt.spi.MqttMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.Objects;

/**
 * Javalin MQTT Guice module.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jMqttMqGuiceModule extends AbstractDdd4jMqGuiceModule {

    private final MqttMQProperties mqttProperties;
    private final MqttClient mqttClient;
    private final MqttMQBrokerAdapter brokerAdapter;

    public Ddd4jMqttMqGuiceModule() {
        this(new MqttMQProperties());
    }

    public Ddd4jMqttMqGuiceModule(MqttMQProperties mqttProperties) {
        this(mqttProperties, new Ddd4jMQProperties(), null);
    }

    public Ddd4jMqttMqGuiceModule(MqttMQProperties mqttProperties, Ddd4jMQProperties mqProperties) {
        this(mqttProperties, mqProperties, null);
    }

    public Ddd4jMqttMqGuiceModule(MqttClient mqttClient,
                                  MqttMQProperties mqttProperties,
                                  Ddd4jMQProperties mqProperties) {
        this(mqttProperties, mqProperties, Objects.requireNonNull(mqttClient, "mqttClient"));
    }

    private Ddd4jMqttMqGuiceModule(MqttMQProperties mqttProperties,
                                   Ddd4jMQProperties mqProperties,
                                   MqttClient mqttClient) {
        super(mqProperties);
        this.mqttProperties = Objects.requireNonNull(mqttProperties, "mqttProperties");
        this.mqttClient = mqttClient;
        this.brokerAdapter = null;
    }

    public Ddd4jMqttMqGuiceModule(MqttMQBrokerAdapter brokerAdapter) {
        this(brokerAdapter, new Ddd4jMQProperties());
    }

    public Ddd4jMqttMqGuiceModule(MqttMQBrokerAdapter brokerAdapter, Ddd4jMQProperties mqProperties) {
        super(mqProperties);
        this.mqttProperties = new MqttMQProperties();
        this.mqttClient = null;
        this.brokerAdapter = Objects.requireNonNull(brokerAdapter, "brokerAdapter");
    }

    @Override
    protected void configure() {
        super.configure();
        bind(MqttMQProperties.class).toInstance(mqttProperties);
    }

    @Provides
    @Singleton
    public MqttMQBrokerAdapter mqttMQBrokerAdapter() {
        if (Objects.nonNull(brokerAdapter)) {
            return brokerAdapter;
        }
        if (Objects.nonNull(mqttClient)) {
            return new MqttMQBrokerAdapter(mqttClient, mqttProperties, mqProperties(), serialization());
        }
        return new MqttMQBrokerAdapter(mqttProperties, mqProperties(), serialization());
    }

    @Provides
    @Singleton
    public MQBrokerAdapter mqBrokerAdapter(MqttMQBrokerAdapter mqttMQBrokerAdapter) {
        return mqttMQBrokerAdapter;
    }

    @Provides
    @Singleton
    public MQEventPublisher mqEventPublisher(MqttMQBrokerAdapter mqttMQBrokerAdapter) {
        return mqttMQBrokerAdapter.createPublisher(mqProperties());
    }
}
