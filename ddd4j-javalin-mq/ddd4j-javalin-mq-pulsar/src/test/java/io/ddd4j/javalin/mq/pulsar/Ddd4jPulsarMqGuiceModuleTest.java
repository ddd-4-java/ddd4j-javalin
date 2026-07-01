package io.ddd4j.javalin.mq.pulsar;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.pulsar.spi.PulsarMQBrokerAdapter;
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

class Ddd4jPulsarMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        PulsarMQBrokerAdapter adapter = mock(PulsarMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jPulsarMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportPulsarBrokerType() {
        PulsarMQBrokerAdapter adapter = mock(PulsarMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.PULSAR);
        when(adapter.supports(MQBrokerType.PULSAR)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jPulsarMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.PULSAR, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.PULSAR));
        assertFalse(brokerAdapter.supports(MQBrokerType.KAFKA));
    }
}
