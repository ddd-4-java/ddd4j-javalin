package io.ddd4j.javalin.mq.rabbit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.rabbit.spi.RabbitMQBrokerAdapter;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-rabbitmq Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 RabbitMQ 需真实服务，本测试用 Mockito mock {@link RabbitTemplate}（与 kafka/rocketmq 测试同构：
 * 验证适配装配正确性，而非 RabbitMQ 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jRabbitMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 RabbitMQ MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        RabbitTemplate mockTemplate = mock(RabbitTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(mockTemplate));

        // 核心契约可注入（证明 Guice 装配链路完整，含 ApplicationContext + EndpointRegistry 适配）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        RabbitTemplate template = injector.getInstance(RabbitTemplate.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockTemplate, template, "RabbitTemplate 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 RABBIT（rabbitmq）。
     */
    @Test
    void shouldReportRabbitBrokerType() {
        RabbitTemplate mockTemplate = mock(RabbitTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(mockTemplate));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.RABBIT, brokerAdapter.brokerType(),
                "BrokerType 应为 RABBIT");
        assertInstanceOf(RabbitMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 RabbitMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 RABBIT 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportRabbitType() {
        RabbitTemplate mockTemplate = mock(RabbitTemplate.class);
        Injector injector = Guice.createInjector(new Ddd4jRabbitMqGuiceModule(mockTemplate));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.RABBIT),
                "BrokerAdapter 应支持 RABBIT 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.KAFKA),
                "BrokerAdapter 不应支持 KAFKA 类型");
    }
}
