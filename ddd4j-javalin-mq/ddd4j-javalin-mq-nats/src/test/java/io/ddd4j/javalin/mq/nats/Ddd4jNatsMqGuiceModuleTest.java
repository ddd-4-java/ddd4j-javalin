package io.ddd4j.javalin.mq.nats;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.nats.spi.NatsMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import io.nats.client.Connection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-nats Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 NATS 需真实服务，本测试用 Mockito mock {@link Connection}（与 disruptor 测试同构：
 * 验证适配装配正确性，而非 NATS 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jNatsMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 NATS MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        Connection mockConn = mock(Connection.class);
        Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(mockConn));

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        Connection conn = injector.getInstance(Connection.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockConn, conn, "Connection 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 NATS。
     */
    @Test
    void shouldReportNatsBrokerType() {
        Connection mockConn = mock(Connection.class);
        Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(mockConn));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.NATS, brokerAdapter.brokerType(),
                "BrokerType 应为 NATS");
        assertInstanceOf(NatsMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 NatsMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 NATS 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportNatsType() {
        Connection mockConn = mock(Connection.class);
        Injector injector = Guice.createInjector(new Ddd4jNatsMqGuiceModule(mockConn));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.NATS),
                "BrokerAdapter 应支持 NATS 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.DISRUPTOR),
                "BrokerAdapter 不应支持 DISRUPTOR 类型");
    }
}
