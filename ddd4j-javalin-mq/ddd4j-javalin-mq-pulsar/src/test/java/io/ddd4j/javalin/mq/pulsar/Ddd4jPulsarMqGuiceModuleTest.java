package io.ddd4j.javalin.mq.pulsar;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.pulsar.PulsarMQClient;
import io.ddd4j.mq.pulsar.PulsarProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-pulsar Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@PulsarMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jPulsarMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        PulsarProperties props = new PulsarProperties();
        PulsarMQClient client = new PulsarMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jPulsarMqGuiceModule(client, props));

        PulsarMQClient resolvedClient = injector.getInstance(PulsarMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("pulsar", resolvedMqClient.impl());
    }
}
