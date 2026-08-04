package io.ddd4j.javalin.mq.sqs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.sqs.SqsMQClient;
import io.ddd4j.mq.sqs.SqsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ddd4j-javalin-mq-sqs Guice integration test.
 *
 * <p>Verifies Guice assembly: the broker-specific {@SqsMQClient} and the generic
 * {{@link MQClient}} contract are both resolvable, and the bound properties
 * equal the constructor argument.
 */
class Ddd4jSqsMqGuiceModuleTest {

    @Test
    void shouldResolveBrokerClientAndGenericContracts() {
        SqsProperties props = new SqsProperties();
        SqsMQClient client = new SqsMQClient(props);
        Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(client, props));

        SqsMQClient resolvedClient = injector.getInstance(SqsMQClient.class);
        MQClient resolvedMqClient = injector.getInstance(MQClient.class);
        MQProperties resolvedProps = injector.getInstance(MQProperties.class);

        assertNotNull(resolvedClient);
        assertSame(client, resolvedClient);
        assertSame(client, resolvedMqClient);
        assertSame(props, resolvedProps);
        assertEquals("sqs", resolvedMqClient.impl());
    }
}
