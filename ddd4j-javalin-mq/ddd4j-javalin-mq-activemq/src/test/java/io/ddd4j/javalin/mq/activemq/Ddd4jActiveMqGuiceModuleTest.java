package io.ddd4j.javalin.mq.activemq;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.activemq.ActiveMQClient;
import io.ddd4j.mq.activemq.ActiveMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-activemq Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@ActiveMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jActiveMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        ActiveMQProperties props = new ActiveMQProperties();
        ActiveMQClient client = new ActiveMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(client, props));

        ActiveMQClient resolvedClient = injector.getInstance(ActiveMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("activemq", resolvedMqClient.impl());
    }
}
