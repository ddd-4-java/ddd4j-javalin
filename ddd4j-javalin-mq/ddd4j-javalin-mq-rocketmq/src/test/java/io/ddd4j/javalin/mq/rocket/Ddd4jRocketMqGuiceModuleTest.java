package io.ddd4j.javalin.mq.rocket;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.rocketmq.RocketMQClient;
import io.ddd4j.mq.rocketmq.RocketMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-rocketmq Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@RocketMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jRocketMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        RocketMQProperties props = new RocketMQProperties();
        RocketMQClient client = new RocketMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jRocketMqGuiceModule(client, props));

        RocketMQClient resolvedClient = injector.getInstance(RocketMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("rocket", resolvedMqClient.impl());
    }
}
