package io.ddd4j.javalin.mq.redisstream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.redisstream.spi.RedisStreamMQBrokerAdapter;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-redis-stream Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 Redis 需真实服务，本测试用 Mockito mock {@link StringRedisTemplate}（与 pulsar 测试同构：
 * 验证适配装配正确性，而非 Redis 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jRedisStreamMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 Redis Stream MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRedisStreamMqGuiceModule(mockRedis));

        // 核心契约可注入（证明 Guice 装配链路完整，含 StaticApplicationContext 适配）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        StringRedisTemplate redisTemplate = injector.getInstance(StringRedisTemplate.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockRedis, redisTemplate, "StringRedisTemplate 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 REDIS_STREAM。
     */
    @Test
    void shouldReportRedisStreamBrokerType() {
        StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRedisStreamMqGuiceModule(mockRedis));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.REDIS_STREAM, brokerAdapter.brokerType(),
                "BrokerType 应为 REDIS_STREAM");
        assertInstanceOf(RedisStreamMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 RedisStreamMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 REDIS_STREAM 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportRedisStreamType() {
        StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRedisStreamMqGuiceModule(mockRedis));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.REDIS_STREAM),
                "BrokerAdapter 应支持 REDIS_STREAM 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.PULSAR),
                "BrokerAdapter 不应支持 PULSAR 类型");
    }
}
