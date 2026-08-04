package io.ddd4j.javalin.mq.mqtt;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.mqtt.MqttMQClient;
import io.ddd4j.mq.mqtt.MqttMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-mqtt Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@MqttMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jMqttMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        MqttMQProperties props = new MqttMQProperties();
        MqttMQClient client = new MqttMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jMqttMqGuiceModule(client, props));

        MqttMQClient resolvedClient = injector.getInstance(MqttMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("mqtt", resolvedMqClient.impl());
    }
}
