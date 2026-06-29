package io.ddd4j.javalin.mq.activemq;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.activemq.spi.ActiveMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-activemq Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 ActiveMQ 需真实服务，本测试用 Mockito mock {@link JmsTemplate}（与 rabbitmq 测试同构：
 * 验证适配装配正确性，而非 ActiveMQ 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jActiveMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 ActiveMQ MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        JmsTemplate mockTemplate = mock(JmsTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(mockTemplate));

        // 核心契约可注入（证明 Guice 装配链路完整，含 ApplicationContext + EndpointRegistry 适配）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        JmsTemplate template = injector.getInstance(JmsTemplate.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockTemplate, template, "JmsTemplate 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 ACTIVEMQ。
     */
    @Test
    void shouldReportActiveMqBrokerType() {
        JmsTemplate mockTemplate = mock(JmsTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(mockTemplate));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.ACTIVEMQ, brokerAdapter.brokerType(),
                "BrokerType 应为 ACTIVEMQ");
        assertInstanceOf(ActiveMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 ActiveMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 ACTIVEMQ 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportActiveMqType() {
        JmsTemplate mockTemplate = mock(JmsTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jActiveMqGuiceModule(mockTemplate));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.ACTIVEMQ),
                "BrokerAdapter 应支持 ACTIVEMQ 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.RABBIT),
                "BrokerAdapter 不应支持 RABBIT 类型");
    }
}
