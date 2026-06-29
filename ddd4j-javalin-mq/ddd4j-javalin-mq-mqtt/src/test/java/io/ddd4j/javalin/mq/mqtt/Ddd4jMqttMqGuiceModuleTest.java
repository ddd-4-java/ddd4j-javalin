package io.ddd4j.javalin.mq.mqtt;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.mqtt.config.Ddd4jMqttProperties;
import io.ddd4j.mq.mqtt.spi.MqttMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.messaging.MessageChannel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-mqtt Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 MQTT 需真实 Broker，本测试用 Mockito mock {@link MessageChannel}/{@link MqttPahoClientFactory}
 * （与 pulsar/redis-stream 测试同构：验证适配装配正确性，而非 MQTT 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jMqttMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 MQTT MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        MessageChannel mockChannel = mock(MessageChannel.class);
        MqttPahoClientFactory mockFactory = mock(MqttPahoClientFactory.class);
        Ddd4jMqttProperties mqttProps = new Ddd4jMqttProperties();
        Injector injector = Guice.createInjector(new Ddd4jMqttMqGuiceModule(mockChannel, mockFactory, mqttProps));

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        MessageChannel channel = injector.getInstance(MessageChannel.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockChannel, channel, "MessageChannel 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 MQTT。
     */
    @Test
    void shouldReportMqttBrokerType() {
        MessageChannel mockChannel = mock(MessageChannel.class);
        MqttPahoClientFactory mockFactory = mock(MqttPahoClientFactory.class);
        Ddd4jMqttProperties mqttProps = new Ddd4jMqttProperties();
        Injector injector = Guice.createInjector(new Ddd4jMqttMqGuiceModule(mockChannel, mockFactory, mqttProps));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.MQTT, brokerAdapter.brokerType(),
                "BrokerType 应为 MQTT");
        assertInstanceOf(MqttMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 MqttMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 MQTT 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportMqttType() {
        MessageChannel mockChannel = mock(MessageChannel.class);
        MqttPahoClientFactory mockFactory = mock(MqttPahoClientFactory.class);
        Ddd4jMqttProperties mqttProps = new Ddd4jMqttProperties();
        Injector injector = Guice.createInjector(new Ddd4jMqttMqGuiceModule(mockChannel, mockFactory, mqttProps));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.MQTT),
                "BrokerAdapter 应支持 MQTT 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.REDIS_STREAM),
                "BrokerAdapter 不应支持 REDIS_STREAM 类型");
    }
}
