package io.ddd4j.javalin.mq.activemq;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.activemq.spi.ActiveMQBrokerAdapter;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Ddd4jActiveMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        ActiveMQBrokerAdapter adapter = mock(ActiveMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportActiveMqBrokerType() {
        ActiveMQBrokerAdapter adapter = mock(ActiveMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.ACTIVEMQ);
        when(adapter.supports(MQBrokerType.ACTIVEMQ)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jActiveMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.ACTIVEMQ, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.ACTIVEMQ));
        assertFalse(brokerAdapter.supports(MQBrokerType.RABBIT));
    }
}
