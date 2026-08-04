package io.ddd4j.javalin.mq.rabbit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.rabbitmq.RabbitMQClient;
import io.ddd4j.mq.rabbitmq.RabbitMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-rabbitmq Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@RabbitMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jRabbitMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        RabbitMQProperties props = new RabbitMQProperties();
        RabbitMQClient client = new RabbitMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(client, props));

        RabbitMQClient resolvedClient = injector.getInstance(RabbitMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("rabbit", resolvedMqClient.impl());
    }
}
