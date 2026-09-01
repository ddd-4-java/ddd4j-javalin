package io.ddd4j.javalin.mq.mqttmica.it;

import com.google.inject.Module;
import io.ddd4j.javalin.mq.mqttmica.Ddd4jMqttMicaMqGuiceModule;
import io.ddd4j.javalin.testcontainers.JunitJupiterTestContainers;
import io.ddd4j.javalin.testcontainers.messaging.AbstractMqIntegrationTest;
import io.ddd4j.mq.mqttmica.MicaMqttMQClient;
import io.ddd4j.mq.mqttmica.MicaMqttProperties;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Integration test for {@link Ddd4jMqttMicaMqGuiceModule} against a real Eclipse Mosquitto
 * broker brought up by Testcontainers, driven by the mica-mqtt AIO client
 * ({@code org.dromara.mica-mqtt-client}). 公共骨架继承自 {@link AbstractMqIntegrationTest}。
 *
 * <p>The message id travels as an MQTT5 user property (mica-mqtt writes
 * {@code ddd4jMessageId}) and is restored by the consumer.
 *
 * <p>mica-mqtt 的 subscribe 为异步（不等 SUBACK 即返回）；基类默认 3s 的
 * {@link #consumerSettleDelay()} 用于等待订阅注册完成，避免 publish 先于订阅到达 broker。
 */
@Tag("integration")
@JunitJupiterTestContainers
// mica-mqtt 2.6.6 客户端（smart-socket AIO）在 macOS arm64 上首连被 broker 拒绝
// （mosquitto 2.0 与 EMQX 5.8 均复现），且 publish 恒返回 true（入队而非确认发送），
// 消息从未到达 broker——核心库 ddd4j-mq-mqtt-mica 的 AIO 发送缺陷，javalin 适配层无法修复。
// 已在核心客户端加入 publish 返回值检查 + reconnect 重试，待 mica-mqtt 上游修复后移除 @Disabled。
// 同场景的 paho 版 ddd4j-javalin-mq-mqtt IT 已通过真实 round-trip。
@org.junit.jupiter.api.Disabled("mica-mqtt AIO 客户端在 macOS arm64 上 publish 静默丢失（核心库缺陷）")
class Ddd4jMicaMqttMqIT extends AbstractMqIntegrationTest<MicaMqttProperties, MicaMqttMQClient> {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(
            DockerImageName.parse("eclipse-mosquitto:2.0"))
            .withExposedPorts(1883)
            .withCommand("/usr/sbin/mosquitto", "-c", "/mosquitto-no-auth.conf", "-v");

    @Override
    protected String brokerName() {
        return "mqtt-mica";
    }

    @Override
    protected String topicName() {
        return "ddd4j.it.mica";
    }

    @Override
    protected GenericContainer<?> container() {
        return MOSQUITTO;
    }

    @Override
    protected MicaMqttProperties newProperties() {
        MicaMqttProperties props = new MicaMqttProperties();
        props.setServerIp(MOSQUITTO.getHost());
        props.setPort(MOSQUITTO.getMappedPort(1883));
        return props;
    }

    @Override
    protected MicaMqttMQClient newClient(MicaMqttProperties props) {
        return new MicaMqttMQClient(props);
    }

    @Override
    protected Module guiceModule(MicaMqttMQClient client, MicaMqttProperties props) {
        return new Ddd4jMqttMicaMqGuiceModule(client, props);
    }

    @Override
    protected Class<MicaMqttMQClient> clientClass() {
        return MicaMqttMQClient.class;
    }

    @Override
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(10);
    }
}
