package io.ddd4j.mq.mqttmica;

import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.client.MqttClient;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;

import java.util.Objects;
import java.util.UUID;

/**
 * mica-mqtt AIO 客户端配置（纯 Java，零 Spring 依赖）。
 *
 * <p>版本与 {@code ${mica-mqtt.version}} = 2.6.6 对齐。
 * 连接/认证/命名空间等通用字段继承自 {@link MQProperties}，本类仅保留 broker 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MicaMqttProperties extends MQProperties {

    /**
     * MQTT 服务器 IP
     */
    private String serverIp = "127.0.0.1";
    /**
     * MQTT 服务器端口
     */
    private int port = 1883;
    /**
     * 客户端 ID 前缀
     */
    private String clientIdPrefix = "ddd4j-mica-";
    /**
     * 是否使用 SSL 连接
     */
    private boolean useSsl = false;
    /**
     * 默认 QoS 级别
     */
    private int qos = 1;
    /**
     * 心跳保活秒数
     */
    private int keepAliveSeconds = 30;
    /**
     * 读取缓冲区大小（字节）
     */
    private int readBufferSize = 8 * 1024;
    /**
     * 最大未完成消息数
     */
    private int maxInflight = 100;

    public String newClientId() {
        return (Objects.isNull(clientIdPrefix) ? "ddd4j-mica-" : clientIdPrefix) + UUID.randomUUID();
    }

    public MqttClient client() {
        MqttClientCreator creator = MqttClient.create()
                .ip(serverIp)
                .port(port)
                .username(getUsername())
                .password(getPassword())
                .clientId(newClientId())
                .keepAliveSecs(keepAliveSeconds)
                .readBufferSize(readBufferSize);
        if (useSsl) {
            creator.useSsl();
        }
        return creator.connectSync();
    }

    public MqttQoS mqttQoS() {
        return switch (qos) {
            case 0 -> MqttQoS.QOS0;
            case 2 -> MqttQoS.QOS2;
            default -> MqttQoS.QOS1;
        };
    }
}
