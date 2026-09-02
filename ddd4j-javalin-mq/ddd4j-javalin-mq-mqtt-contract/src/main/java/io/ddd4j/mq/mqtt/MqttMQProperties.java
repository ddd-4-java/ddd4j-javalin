package io.ddd4j.mq.mqtt;

import io.ddd4j.mq.MQProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Objects;
import java.util.UUID;

/**
 * Eclipse Paho MQTT v3 适配器配置（纯 Java，零 Spring 依赖）。
 *
 * <p>对应 {@code org.eclipse.paho:org.eclipse.paho.client.mqttv3} 原生 MQTT 客户端。
 * 连接/认证/命名空间等通用字段继承自 {@link MQProperties}，本类仅保留 broker 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MqttMQProperties extends MQProperties {

    /**
     * MQTT Broker 地址（例：{@code tcp://host:1883} 或 {@code ssl://host:8883}）。
     */
    private String serverUri = "tcp://localhost:1883";
    /**
     * 客户端 ID 前缀
     */
    private String clientIdPrefix = "ddd4j-mq-";
    /**
     * QoS level: 0 (at most once), 1 (at least once), 2 (exactly once).
     */
    private int qos = 1;
    private boolean cleanSession = true;
    private int keepAliveSeconds = 30;
    private int connectionTimeoutSeconds = 30;
    private boolean automaticReconnect = true;
    private int maxInflight = 100;
    private String willTopic;
    private String willPayload;
    private int willQos = 0;
    private boolean willRetained = false;

    public String newClientId() {
        return (Objects.isNull(clientIdPrefix) ? "ddd4j-mq-" : clientIdPrefix) + UUID.randomUUID();
    }

    public MqttConnectOptions connectOptions() {
        MqttConnectOptions o = new MqttConnectOptions();
        o.setCleanSession(cleanSession);
        o.setKeepAliveInterval(keepAliveSeconds);
        o.setConnectionTimeout(connectionTimeoutSeconds);
        o.setAutomaticReconnect(automaticReconnect);
        o.setMaxInflight(maxInflight);
        if (Objects.nonNull(getUsername()) && !io.ddd4j.kit.lang.StrKit.isBlank(getUsername())) {
            o.setUserName(getUsername());
        }
        if (Objects.nonNull(getPassword()) && !io.ddd4j.kit.lang.StrKit.isBlank(getPassword())) {
            o.setPassword(getPassword().toCharArray());
        }
        if (Objects.nonNull(willTopic) && !io.ddd4j.kit.lang.StrKit.isBlank(willTopic)) {
            o.setWill(willTopic, Objects.isNull(willPayload) ? new byte[0] : willPayload.getBytes(), willQos, willRetained);
        }
        return o;
    }
}
