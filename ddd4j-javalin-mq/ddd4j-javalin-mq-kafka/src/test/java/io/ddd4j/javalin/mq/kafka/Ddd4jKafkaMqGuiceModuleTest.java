package io.ddd4j.javalin.mq.kafka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.kafka.mq.KafkaMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.serialization.MQMessageSerialization;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-kafka Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 Kafka 需真实集群，本测试用 Mockito mock spring-kafka 的 KafkaTemplate/ConsumerFactory
 * （与 nats/ons/sqs 测试同构：验证适配装配正确性，而非 Kafka 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jKafkaMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 Kafka MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ConsumerFactory<String, String> mockFactory = mock(ConsumerFactory.class);
        Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(mockTemplate, mockFactory));

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        MQMessageSerialization serialization = injector.getInstance(MQMessageSerialization.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertNotNull(serialization, "MQMessageSerialization 应可从 Guice 解析");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 KAFKA。
     */
    @Test
    void shouldReportKafkaBrokerType() {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ConsumerFactory<String, String> mockFactory = mock(ConsumerFactory.class);
        Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(mockTemplate, mockFactory));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.KAFKA, brokerAdapter.brokerType(),
                "BrokerType 应为 KAFKA");
        assertInstanceOf(KafkaMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 KafkaMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 KAFKA 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportKafkaType() {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ConsumerFactory<String, String> mockFactory = mock(ConsumerFactory.class);
        Injector injector = Guice.createInjector(new Ddd4jKafkaMqGuiceModule(mockTemplate, mockFactory));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.KAFKA),
                "BrokerAdapter 应支持 KAFKA 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.SQS),
                "BrokerAdapter 不应支持 SQS 类型");
    }
}
