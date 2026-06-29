package io.ddd4j.javalin.mq.rocket;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.rocket.spi.RocketMQBrokerAdapter;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-rocketmq Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 RocketMQ 需真实集群，本测试用 Mockito mock {@link RocketMQTemplate}（与 kafka 测试同构：
 * 验证适配装配正确性，而非 RocketMQ 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jRocketMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 RocketMQ MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        RocketMQTemplate mockTemplate = mock(RocketMQTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRocketMqGuiceModule(mockTemplate));

        // 核心契约可注入（证明 Guice 装配链路完整，含 LightweightApplicationContext 适配）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        RocketMQTemplate template = injector.getInstance(RocketMQTemplate.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockTemplate, template, "RocketMQTemplate 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 ROCKET（rocketmq）。
     */
    @Test
    void shouldReportRocketBrokerType() {
        RocketMQTemplate mockTemplate = mock(RocketMQTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRocketMqGuiceModule(mockTemplate));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.ROCKET, brokerAdapter.brokerType(),
                "BrokerType 应为 ROCKET");
        assertInstanceOf(RocketMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 RocketMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 ROCKET 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportRocketType() {
        RocketMQTemplate mockTemplate = mock(RocketMQTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRocketMqGuiceModule(mockTemplate));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.ROCKET),
                "BrokerAdapter 应支持 ROCKET 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.KAFKA),
                "BrokerAdapter 不应支持 KAFKA 类型");
    }
}
