package io.ddd4j.javalin.mq.kafka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.io.ddd4j.mq.kafka.KafkaMQClient;
import io.ddd4j.mq.io.ddd4j.mq.kafka.KafkaMQProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-kafka Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@KafkaMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jKafkaMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        KafkaMQProperties props = new KafkaMQProperties();
        KafkaMQClient client = new KafkaMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(client, props));

        KafkaMQClient resolvedClient = injector.getInstance(KafkaMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("kafka", resolvedMqClient.impl());
    }
}
