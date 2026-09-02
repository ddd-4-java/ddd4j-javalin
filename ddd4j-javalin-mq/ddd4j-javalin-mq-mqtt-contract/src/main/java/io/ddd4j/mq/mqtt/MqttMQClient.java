package io.ddd4j.mq.mqtt;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Eclipse Paho MQTT 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 *
 * <p>MQTT 无原生 broker-side tag filter（仅 topic 通配），应用层 {@link TagMatcher} tag 过滤保留。
 *
 * <p>注意：Paho 原生类名 {@code MqttClient} 与 ddd4j 的 {@link MQClient} 概念相近但不同，
 * 本类统一用全限定名 {@link org.eclipse.paho.client.mqttv3.MqttClient} 避免歧义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : MqttMQClient ###")
public class MqttMQClient implements MQClient {

    private final MqttMQProperties properties;
    private final AtomicReference<org.eclipse.paho.client.mqttv3.MqttClient> clientRef = new AtomicReference<>();

    /**
     * 双构造 1：注入已初始化的原生 Paho 客户端（用于 runtime 集成自动注入）。
     *
     * @param client 原生 MQTT 客户端
     */
    public MqttMQClient(org.eclipse.paho.client.mqttv3.MqttClient client) {
        this.properties = new MqttMQProperties();
        if (Objects.nonNull(client)) {
            this.clientRef.set(client);
        }
    }

    /**
     * 双构造 2：传入配置，{@link #connection()} 时 lazy 构造原生客户端。
     *
     * @param properties MQTT 配置
     */
    public MqttMQClient(MqttMQProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String impl() {
        return "mqtt";
    }

    @Override
    public String defaultConcat() {
        return "/";
    }

    /**
     * MQTT 无原生 broker-side tag selector，仅 topic 通配 → 强制应用层 {@link TagMatcher} 过滤。
     */
    @Override
    public boolean supportsBrokerTagFilter() {
        return false;
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        org.eclipse.paho.client.mqttv3.MqttClient client = connection();
        return event -> {
            String payload = serialization().serialize(event);
            String topic = resolveTopic(event, mqProperties);
            try {
                byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                MqttMessage msg = new MqttMessage(body);
                msg.setQos(qos());
                // Paho MQTT v3 没有 user properties；业务稳定 ID 仅保留在 MQEvent payload，
                // MQTT packet id 是短生命周期协议序号，不能作为 ddd4j-message-id 写入或回读。
                client.publish(topic, msg);
            } catch (Exception ex) {
                log.error("Publish MQTT [{}]: {} failed!", topic, payload, ex);
                throw new IllegalStateException("Publish MQTT event failed", ex);
            }
            log.info("Publish MQ [{}]: {}", topic, payload);
        };
    }

    // ========================= 消费者 =========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        org.eclipse.paho.client.mqttv3.MqttClient client = connection();
        // MQTT subscribe 通配：保留原生 subscribe `topic/#` 行为（首个 include tag 拼接到末尾 /#）
        String topic = resolveTopic(listener, mqProperties);
        String includeTag = TagMatcher.findIncludes(listener.getTags()).stream().findFirst().orElse(null);
        String subscribeTopic = Objects.isNull(includeTag) ? topic : topic + "/#";
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(String arrivedTopic, MqttMessage message) {
                try {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    MQEvent event = serialization().deserialize(payload, listener.payloadType());
                    if (Objects.isNull(event)) {
                        log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(defaultConcat()));
                        return;
                    }
                    // 应用层 tag 过滤（优先 user property 中的 tag，回落到 topic 末段）
                    if (!TagMatcher.match(readTag(message, arrivedTopic), listener.getTags())) {
                        return;
                    }
                    MqttAcknowledgment ack = new MqttAcknowledgment(message, arrivedTopic);
                    consume(listener, event, ack);
                    if (!ack.isAcknowledged()) {
                        ack.ackSingle();
                    }
                } catch (Throwable ex) {
                    log.error("Consume MQTT [{}] failed", listener.getRouteExpression(defaultConcat()), ex);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        client.subscribe(subscribeTopic, qos());
        return true;
    }

    /**
     * 从到达的物理 topic 末段解析 tag（保持与生产者 {@link #resolveTopic} 拼接规则一致）。
     */
    private String readTag(MqttMessage message, String arrivedTopic) {
        int lastSlash = arrivedTopic.lastIndexOf('/');
        if (lastSlash >= 0) {
            return arrivedTopic.substring(lastSlash + 1);
        }
        return null;
    }

    // ========================= 连接管理 =========================

    /**
     * QoS：注入原生客户端时取默认值 1，否则读 properties。
     */
    private int qos() {
        return properties.getQos();
    }

    private synchronized org.eclipse.paho.client.mqttv3.MqttClient connection() {
        org.eclipse.paho.client.mqttv3.MqttClient c = clientRef.get();
        if (Objects.nonNull(c)) {
            return c;
        }
        try {
            org.eclipse.paho.client.mqttv3.MqttClient nc = new org.eclipse.paho.client.mqttv3.MqttClient(
                    properties.getServerUri(), properties.newClientId());
            nc.connect(properties.connectOptions());
            clientRef.set(nc);
            c = nc;
        } catch (Exception ex) {
            throw new IllegalStateException("Open MQTT connection failed", ex);
        }
        return c;
    }
}
