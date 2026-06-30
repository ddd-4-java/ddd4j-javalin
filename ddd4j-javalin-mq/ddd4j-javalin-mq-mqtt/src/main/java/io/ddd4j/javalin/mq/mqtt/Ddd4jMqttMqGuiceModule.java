package io.ddd4j.javalin.mq.mqtt;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqtt.config.Ddd4jMqttProperties;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.messaging.MessageChannel;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.mqtt.Ddd4jMqttMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jMqttMqGuiceModule extends io.ddd4j.guice.mq.mqtt.Ddd4jMqttMqGuiceModule {

    public Ddd4jMqttMqGuiceModule(MessageChannel mqttOutboundChannel, MqttPahoClientFactory mqttClientFactory, Ddd4jMqttProperties mqttProperties) {
        super(mqttOutboundChannel, mqttClientFactory, mqttProperties);
    }

    public Ddd4jMqttMqGuiceModule(MessageChannel mqttOutboundChannel, MqttPahoClientFactory mqttClientFactory, Ddd4jMQProperties mqProperties, Ddd4jMqttProperties mqttProperties) {
        super(mqttOutboundChannel, mqttClientFactory, mqProperties, mqttProperties);
    }

}
