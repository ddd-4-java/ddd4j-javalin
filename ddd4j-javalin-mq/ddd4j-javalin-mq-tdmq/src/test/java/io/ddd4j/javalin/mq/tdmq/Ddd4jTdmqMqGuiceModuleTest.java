package io.ddd4j.javalin.mq.tdmq;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.tdmq.TdmqMQClient;
import io.ddd4j.mq.tdmq.TdmqProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-tdmq Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@TdmqMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jTdmqMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        TdmqProperties props = new TdmqProperties();
        TdmqMQClient client = new TdmqMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jTdmqMqGuiceModule(client, props));

        TdmqMQClient resolvedClient = injector.getInstance(TdmqMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("tdmq", resolvedMqClient.impl());
    }
}
