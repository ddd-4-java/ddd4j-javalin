package io.ddd4j.javalin.mq.mqttmica;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.mqttmica.MicaMqttMQClient;
import io.ddd4j.mq.mqttmica.MicaMqttProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-mqtt-mica Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@MicaMqttMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jMqttMicaMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        MicaMqttProperties props = new MicaMqttProperties();
        MicaMqttMQClient client = new MicaMqttMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jMqttMicaMqGuiceModule(client, props));

        MicaMqttMQClient resolvedClient = injector.getInstance(MicaMqttMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("mqtt-mica", resolvedMqClient.impl());
    }
}
