package io.ddd4j.javalin.mq.mqttmica;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.mqttmica.spi.MicaMqttMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
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

class Ddd4jMicaMqttMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        MicaMqttMQBrokerAdapter adapter = mock(MicaMqttMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jMicaMqttMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportMicaMqttBrokerType() {
        MicaMqttMQBrokerAdapter adapter = mock(MicaMqttMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.MQTT_MICA);
        when(adapter.supports(MQBrokerType.MQTT_MICA)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jMicaMqttMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.MQTT_MICA, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.MQTT_MICA));
        assertFalse(brokerAdapter.supports(MQBrokerType.MQTT));
    }
}
