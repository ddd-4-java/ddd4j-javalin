package io.ddd4j.javalin.mq.mqttmica;

import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqtt.mica.config.Ddd4jMicaMqttProperties;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;

/**
 * @deprecated use {@link io.ddd4j.guice.mq.mqttmica.Ddd4jMicaMqttMqGuiceModule} instead.
 */
@Deprecated
public class Ddd4jMicaMqttMqGuiceModule extends io.ddd4j.guice.mq.mqttmica.Ddd4jMicaMqttMqGuiceModule {

    public Ddd4jMicaMqttMqGuiceModule(MqttClientTemplate mqttClientTemplate, Ddd4jMicaMqttProperties micaMqttProperties) {
        super(mqttClientTemplate, micaMqttProperties);
    }

    public Ddd4jMicaMqttMqGuiceModule(MqttClientTemplate mqttClientTemplate, Ddd4jMQProperties mqProperties, Ddd4jMicaMqttProperties micaMqttProperties) {
        super(mqttClientTemplate, mqProperties, micaMqttProperties);
    }

}
