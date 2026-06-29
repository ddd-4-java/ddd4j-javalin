package io.ddd4j.javalin.mq.ons;

import com.aliyun.openservices.ons.api.Producer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.ons.spi.OnsMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.registry.MQBrokerType;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-ons Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 ONS 需真实阿里云服务，本测试用 Mockito mock {@link Producer}（与 nats 测试同构：
 * 验证适配装配正确性，而非 ONS 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jOnsMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 ONS MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        Producer mockProducer = mock(Producer.class);
        Properties props = new Properties();
        Injector injector = Guice.createInjector(new Ddd4jOnsMqGuiceModule(mockProducer, props));

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        Producer producer = injector.getInstance(Producer.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockProducer, producer, "Producer 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 ONS。
     */
    @Test
    void shouldReportOnsBrokerType() {
        Producer mockProducer = mock(Producer.class);
        Properties props = new Properties();
        Injector injector = Guice.createInjector(new Ddd4jOnsMqGuiceModule(mockProducer, props));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.ONS, brokerAdapter.brokerType(),
                "BrokerType 应为 ONS");
        assertInstanceOf(OnsMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 OnsMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 ONS 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportOnsType() {
        Producer mockProducer = mock(Producer.class);
        Properties props = new Properties();
        Injector injector = Guice.createInjector(new Ddd4jOnsMqGuiceModule(mockProducer, props));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.ONS),
                "BrokerAdapter 应支持 ONS 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.NATS),
                "BrokerAdapter 不应支持 NATS 类型");
    }
}
