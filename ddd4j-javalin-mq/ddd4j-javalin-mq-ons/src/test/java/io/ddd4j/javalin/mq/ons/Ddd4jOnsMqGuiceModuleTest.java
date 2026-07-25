package io.ddd4j.javalin.mq.ons;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.io.ddd4j.mq.ons.OnsMQClient;
import io.ddd4j.mq.io.ddd4j.mq.ons.OnsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-ons Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@OnsMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jOnsMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        OnsProperties props = new OnsProperties();
        OnsMQClient client = new OnsMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jOnsMqGuiceModule(client, props));

        OnsMQClient resolvedClient = injector.getInstance(OnsMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("ons", resolvedMqClient.impl());
    }
}
