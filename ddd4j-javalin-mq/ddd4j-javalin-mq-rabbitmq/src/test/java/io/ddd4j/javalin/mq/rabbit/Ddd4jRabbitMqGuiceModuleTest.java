package io.ddd4j.javalin.mq.rabbit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rabbit.RabbitMQBrokerAdapter;
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

class Ddd4jRabbitMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        RabbitMQBrokerAdapter adapter = mock(RabbitMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportRabbitBrokerType() {
        RabbitMQBrokerAdapter adapter = mock(RabbitMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.RABBIT);
        when(adapter.supports(MQBrokerType.RABBIT)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.RABBIT, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.RABBIT));
        assertFalse(brokerAdapter.supports(MQBrokerType.KAFKA));
    }
}
