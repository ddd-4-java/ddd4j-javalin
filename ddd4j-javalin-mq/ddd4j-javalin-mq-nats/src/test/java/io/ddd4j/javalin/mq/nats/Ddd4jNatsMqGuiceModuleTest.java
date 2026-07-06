package io.ddd4j.javalin.mq.nats;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.nats.spi.NatsMQBrokerAdapter;
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

class Ddd4jNatsMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        NatsMQBrokerAdapter adapter = mock(NatsMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportNatsBrokerType() {
        NatsMQBrokerAdapter adapter = mock(NatsMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.NATS);
        when(adapter.supports(MQBrokerType.NATS)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jNatsMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.NATS, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.NATS));
        assertFalse(brokerAdapter.supports(MQBrokerType.DISRUPTOR));
    }
}
