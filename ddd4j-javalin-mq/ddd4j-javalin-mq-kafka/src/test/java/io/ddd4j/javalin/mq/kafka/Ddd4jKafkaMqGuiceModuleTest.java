package io.ddd4j.javalin.mq.kafka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.kafka.KafkaMQBrokerAdapter;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.serialization.MQMessageSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ddd4j-javalin-mq-kafka Guice integration test.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jKafkaMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        KafkaMQBrokerAdapter adapter = mock(KafkaMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
        assertNotNull(injector.getInstance(MQMessageSerialization.class));
    }

    @Test
    void shouldReportKafkaBrokerType() {
        KafkaMQBrokerAdapter adapter = mock(KafkaMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.KAFKA);
        when(adapter.supports(MQBrokerType.KAFKA)).thenReturn(true);
        Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(adapter));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.KAFKA, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.KAFKA));
        assertFalse(brokerAdapter.supports(MQBrokerType.SQS));
    }
}
