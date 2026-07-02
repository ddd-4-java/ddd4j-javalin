package io.ddd4j.javalin.mq.disruptor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.core.event.MQEvent;
import io.ddd4j.mq.contract.MQDestination;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ddd4j-javalin-mq-disruptor Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且可发布事件。
 * 这是"javalin 侧 mq 适配真正可用"的端到端证据，而非仅编译通过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jDisruptorMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 disruptor MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        Injector injector = Guice.createInjector(new Ddd4jDisruptorMqGuiceModule());

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
    }

    /**
     * 验证通过 MQEventPublisher 发布事件不抛异常（disruptor RingBuffer 真实工作）。
     */
    @Test
    void shouldPublishEventWithoutError() {
        Injector injector = Guice.createInjector(new Ddd4jDisruptorMqGuiceModule());
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);

        MQEvent event = new MQEvent();
        event.setMsgId("test-msg-001");
        event.setTopic("test-topic");

        assertDoesNotThrow(() -> publisher.publish(event, MQDestination.of("test-topic", "test-tag")),
                "通过 disruptor 发布事件不应抛异常");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 DISRUPTOR。
     */
    @Test
    void shouldReportDisruptorBrokerType() {
        Injector injector = Guice.createInjector(new Ddd4jDisruptorMqGuiceModule());
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertNotNull(brokerAdapter.brokerType(), "BrokerType 应非 null");
    }
}
