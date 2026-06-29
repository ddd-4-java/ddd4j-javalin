package io.ddd4j.javalin.mq.mqttmica;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.mq.mqtt.mica.config.Ddd4jMicaMqttProperties;
import io.ddd4j.mq.mqtt.mica.spi.MicaMqttMQBrokerAdapter;
import io.ddd4j.mq.publish.MQEventPublisher;
import io.ddd4j.mq.registry.MQBrokerType;
import io.ddd4j.mq.spi.MQBrokerAdapter;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * ddd4j-javalin-mq-mqtt-mica Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，核心契约（MQEventPublisher / MQBrokerAdapter）可注入且 BrokerType 正确。
 * 由于 MQTT 需真实 Broker，本测试用 Mockito mock {@link MqttClientTemplate}（与 mqtt 测试同构：
 * 验证适配装配正确性，而非 MQTT 服务连通性）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jMicaMqttMqGuiceModuleTest {

    /**
     * 验证 Guice 能创建包含 mica-mqtt MQ 的 Injector，且核心契约可解析。
     */
    @Test
    void shouldResolveCoreContractsFromGuice() {
        MqttClientTemplate mockTemplate = mock(MqttClientTemplate.class);
        Ddd4jMicaMqttProperties micaProps = new Ddd4jMicaMqttProperties();
        Injector injector = Guice.createInjector(new Ddd4jMicaMqttMqGuiceModule(mockTemplate, micaProps));

        // 核心契约可注入（证明 Guice 装配链路完整）
        MQEventPublisher publisher = injector.getInstance(MQEventPublisher.class);
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);
        MqttClientTemplate template = injector.getInstance(MqttClientTemplate.class);

        assertNotNull(publisher, "MQEventPublisher 应可从 Guice 解析");
        assertNotNull(brokerAdapter, "MQBrokerAdapter 应可从 Guice 解析");
        assertSame(mockTemplate, template, "MqttClientTemplate 应是业务方提供的同一实例");
    }

    /**
     * 验证 BrokerAdapter 正确报告 broker 类型为 MQTT_MICA。
     */
    @Test
    void shouldReportMqttMicaBrokerType() {
        MqttClientTemplate mockTemplate = mock(MqttClientTemplate.class);
        Ddd4jMicaMqttProperties micaProps = new Ddd4jMicaMqttProperties();
        Injector injector = Guice.createInjector(new Ddd4jMicaMqttMqGuiceModule(mockTemplate, micaProps));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertEquals(MQBrokerType.MQTT_MICA, brokerAdapter.brokerType(),
                "BrokerType 应为 MQTT_MICA");
        assertInstanceOf(MicaMqttMQBrokerAdapter.class, brokerAdapter,
                "BrokerAdapter 应是 MicaMqttMQBrokerAdapter 实例");
    }

    /**
     * 验证 BrokerAdapter 的 supports 方法对 MQTT_MICA 类型返回 true。
     */
    @Test
    void brokerAdapterShouldSupportMqttMicaType() {
        MqttClientTemplate mockTemplate = mock(MqttClientTemplate.class);
        Ddd4jMicaMqttProperties micaProps = new Ddd4jMicaMqttProperties();
        Injector injector = Guice.createInjector(new Ddd4jMicaMqttMqGuiceModule(mockTemplate, micaProps));
        MQBrokerAdapter brokerAdapter = injector.getInstance(MQBrokerAdapter.class);

        assertTrue(brokerAdapter.supports(MQBrokerType.MQTT_MICA),
                "BrokerAdapter 应支持 MQTT_MICA 类型");
        assertFalse(brokerAdapter.supports(MQBrokerType.MQTT),
                "BrokerAdapter 不应支持 MQTT 类型");
    }
}
