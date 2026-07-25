package io.ddd4j.javalin.mq.disruptor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.disruptor.DisruptorMQClient;
import io.ddd4j.mq.disruptor.DisruptorMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-disruptor Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@link DisruptorMQClient} and the
 * generic {@link MQClient} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jDisruptorMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        DisruptorMQProperties props = new DisruptorMQProperties();
        DisruptorMQClient client = new DisruptorMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jDisruptorMqGuiceModule(client, props));

        DisruptorMQClient resolvedClient = injector.getInstance(DisruptorMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("disruptor", resolvedMqClient.impl());
    }
}