package io.ddd4j.javalin.mq.sqs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.sqs.spi.SqsBrokerAdapter;
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
 * ddd4j-javalin-mq-sqs Guice integration test.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jSqsMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        SqsBrokerAdapter adapter = mock(SqsBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        when(adapter.brokerType()).thenReturn(MQBrokerType.SQS);
        when(adapter.supports(MQBrokerType.SQS)).thenReturn(true);
        Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
        assertNotNull(injector.getInstance(Ddd4jMQProperties.class));
    }

    @Test
    void shouldReportSqsBrokerType() {
        SqsBrokerAdapter adapter = mock(SqsBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.SQS);
        when(adapter.supports(MQBrokerType.SQS)).thenReturn(true);
        Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(adapter));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.SQS, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.SQS));
        assertFalse(brokerAdapter.supports(MQBrokerType.ONS));
    }
}
