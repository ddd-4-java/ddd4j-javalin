package io.ddd4j.javalin.mq.nats;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.io.ddd4j.mq.nats.NatsMQClient;
import io.ddd4j.mq.io.ddd4j.mq.nats.NatsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-nats Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@NatsMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jNatsMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        NatsProperties props = new NatsProperties();
        NatsMQClient client = new NatsMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(client, props));

        NatsMQClient resolvedClient = injector.getInstance(NatsMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("nats", resolvedMqClient.impl());
    }
}
