package io.ddd4j.javalin.mq.redisstream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.redisstream.RedisStreamMQClient;
import io.ddd4j.mq.redisstream.RedisStreamMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-redis-stream Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@RedisStreamMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jRedisStreamMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        RedisStreamMQProperties props = new RedisStreamMQProperties();
        RedisStreamMQClient client = new RedisStreamMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jRedisStreamMqGuiceModule(client, props));

        RedisStreamMQClient resolvedClient = injector.getInstance(RedisStreamMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("redisStream", resolvedMqClient.impl());
    }
}
