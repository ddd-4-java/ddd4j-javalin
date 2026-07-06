package io.ddd4j.javalin.mq.redisstream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.config.Ddd4jMQProperties;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.redisstream.RedisStreamMQBrokerAdapter;
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

class Ddd4jRedisStreamMqGuiceModuleTest {

    @Test
    void shouldResolveCoreContractsFromGuice() {
        RedisStreamMQBrokerAdapter adapter = mock(RedisStreamMQBrokerAdapter.class);
        MQEventPublisher publisher = mock(MQEventPublisher.class);
        when(adapter.createPublisher(any(Ddd4jMQProperties.class))).thenReturn(publisher);
        Injector injector = Guice.createInjector(new Ddd4jRedisStreamMqGuiceModule(adapter));

        assertSame(publisher, injector.getInstance(MQEventPublisher.class));
        assertSame(adapter, injector.getInstance(MQBrokerAdapter.class));
    }

    @Test
    void shouldReportRedisStreamBrokerType() {
        RedisStreamMQBrokerAdapter adapter = mock(RedisStreamMQBrokerAdapter.class);
        when(adapter.brokerType()).thenReturn(MQBrokerType.REDIS_STREAM);
        when(adapter.supports(MQBrokerType.REDIS_STREAM)).thenReturn(true);
        MQBrokerAdapter brokerAdapter = Guice.createInjector(new Ddd4jRedisStreamMqGuiceModule(adapter))
                .getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.REDIS_STREAM, brokerAdapter.brokerType());
        assertTrue(brokerAdapter.supports(MQBrokerType.REDIS_STREAM));
        assertFalse(brokerAdapter.supports(MQBrokerType.PULSAR));
    }
}
