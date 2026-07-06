package io.ddd4j.javalin.mq.tdmq;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.event.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.tdmq.client.TdmqClient;
import io.ddd4j.mq.tdmq.client.TdmqClientPlaceholder;
import io.ddd4j.mq.tdmq.spi.TdmqMQBrokerAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-tdmq Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 TDMQ 需真实腾讯云服务，本测试用默认 TdmqClientPlaceholder + Mockito mock TdmqClient
 * （与 disruptor 测试同构：验证适配装配正确性，而非 TDMQ 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jTdmqMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 TDMQ MQ 的 Injector（默认占位客户端），且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsWithPlaceholderClient() {
        Injector injector = Guice.createInjector(new Ddd4jTdmqMqGuiceModule());

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        TdmqClient client = injector.getInstance(TdmqClient.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertInstanceOf(TdmqClientPlaceholder.class, client, "默认应是 TdmqClientPlaceholder");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 TDMQ。
     */
    @Test
    void shouldReportTdmqBrokerType() {
        Injector injector = Guice.createInjector(new Ddd4jTdmqMqGuiceModule());
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.TDMQ, brokerAdapter.brokerType(),
                "BrokerType 应为 TDMQ");
        assertInstanceOf(TdmqMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 TdmqMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 TDMQ 类型返回 true，且业务方自定义客户端可注入。
     */
    @Test
    void brokerAdapterShouldSupportTdmqTypeWithCustomClient() {
        TdmqClient mockClient = mock(TdmqClient.class);
        Injector injector = Guice.createInjector(new Ddd4jTdmqMqGuiceModule(mockClient));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.TDMQ),
                "BrokerAdapter 应支持 TDMQ 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.MQTT),
                "BrokerAdapter 不应支持 MQTT 类型");
        // 业务方提供的自定义客户端应可注入
        assertSame(mockClient, injector.getInstance(TdmqClient.class),
                "业务方自定义 TdmqClient 应可注入");
    }
}
