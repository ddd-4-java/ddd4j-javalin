package io.ddd4j.javalin.mq.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.sqs.spi.SqsMQBrokerAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-sqs Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 SQS 需真实 AWS 服务，本测试用 Mockito mock {@link AmazonSQS}（与 nats/ons 测试同构：
 * 验证适配装配正确性，而非 SQS 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jSqsMqGuiceModuleTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123/test-queue";

    /**
     * 验证 Guice 能创建包含 SQS MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        AmazonSQS mockSqs = mock(AmazonSQS.class);
        Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(mockSqs, QUEUE_URL));

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        AmazonSQS sqs = injector.getInstance(AmazonSQS.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockSqs, sqs, "AmazonSQS 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 SQS。
     */
    @Test
    void shouldReportSqsBrokerType() {
        AmazonSQS mockSqs = mock(AmazonSQS.class);
        Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(mockSqs, QUEUE_URL));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.SQS, brokerAdapter.brokerType(),
                "BrokerType 应为 SQS");
        assertInstanceOf(SqsMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 SqsMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 SQS 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportSqsType() {
        AmazonSQS mockSqs = mock(AmazonSQS.class);
        Injector injector = Guice.createInjector(new Ddd4jSqsMqGuiceModule(mockSqs, QUEUE_URL));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.SQS),
                "BrokerAdapter 应支持 SQS 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.ONS),
                "BrokerAdapter 不应支持 ONS 类型");
    }
}
