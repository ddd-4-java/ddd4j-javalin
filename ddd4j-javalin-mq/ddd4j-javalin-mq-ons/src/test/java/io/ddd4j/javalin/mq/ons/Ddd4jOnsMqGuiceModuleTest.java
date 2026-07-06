package io.ddd4j.javalin.mq.ons;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.ons.spi.OnsMQBrokerAdapter;
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

class Ddd4jOnsMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        OnsMQBrokerAdapter adapter = mock(OnsMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jOnsMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportOnsBrokerType() {
        OnsMQBrokerAdapter adapter = mock(OnsMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.ONS);
        when(adapter.supports(MQBrokerType.ONS)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jOnsMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.ONS, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.ONS));
        assertFalse(brokerAdapter.supports(MQBrokerType.NATS));
    }
}
