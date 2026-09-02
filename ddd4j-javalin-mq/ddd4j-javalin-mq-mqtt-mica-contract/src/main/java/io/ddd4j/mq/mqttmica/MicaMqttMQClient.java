package io.ddd4j.mq.mqttmica;

import io.ddd4j.kit.lang.IdKit;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.client.MqttClient;
import org.dromara.mica.mqtt.codec.message.MqttPublishMessage;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * mica-mqtt AIO 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 *
 * <p>mica-mqtt 协议层自动处理 PUBACK；无原生 broker-side tag filter，应用层 {@link TagMatcher} 过滤保留。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : MicaMqttMQClient ###")
public class MicaMqttMQClient implements MQClient {

    private final MicaMqttProperties properties;
    private final AtomicReference<MqttClient> clientRef = new AtomicReference<>();

    /**
     * 双构造 1：注入已初始化的原生 mica-mqtt 客户端（用于 runtime 集成自动注入）。
     *
     * @param client 原生 mica-mqtt 客户端
     */
    public MicaMqttMQClient(MqttClient client) {
        this.properties = new MicaMqttProperties();
        if (Objects.nonNull(client)) {
            this.clientRef.set(client);
        }
    }

    /**
     * 双构造 2：传入配置，{@link #client()} 时 lazy 构造原生 mica-mqtt 客户端。
     *
     * @param properties mica-mqtt 配置
     */
    public MicaMqttMQClient(MicaMqttProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String impl() {
        return "mqtt-mica";
    }

    @Override
    public String defaultConcat() {
        return "/";
    }

    /**
     * mica-mqtt 无原生 broker-side tag selector，仅 topic 通配 → 强制应用层 {@link TagMatcher} 过滤。
     */
    @Override
    public boolean supportsBrokerTagFilter() {
        return false;
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        MqttClient client = client();
        return event -> {
            String payload = serialization().serialize(event);
            String topic = resolveTopic(event, mqProperties);
            try {
                byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                boolean sent = client.publish(topic, body, qos(), builder -> builder.properties(mqttProperties -> {
                        if (StrKit.isNotEmpty(event.getMsgId())) {
                            mqttProperties.addUserProperty(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
                        }
                    }));
                if (!sent) {
                    log.warn("Publish mica-mqtt [{}] failed (connection lost), reconnecting and retrying", topic);
                    client.reconnect();
                    sent = client.publish(topic, body, qos(), builder -> builder.properties(mqttProperties -> {
                        if (StrKit.isNotEmpty(event.getMsgId())) {
                            mqttProperties.addUserProperty(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
                        }
                    }));
                }
                if (!sent) {
                    throw new IllegalStateException("Publish mica-mqtt event failed: " + topic);
                }
            } catch (Exception ex) {
                log.error("Publish mica-mqtt [{}]: {} failed!", topic, payload, ex);
                throw new IllegalStateException("Publish mica-mqtt event failed", ex);
            }
            log.info("Publish MQ [{}]: {}", topic, payload);
        };
    }

    // ========================= 消费者 =========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        MqttClient client = client();
        // mica subscribe 通配：保留原生 subscribe `topic/#` 行为（首个 include tag 拼接到末尾 /#）
        String topic = resolveTopic(listener, mqProperties);
        String includeTag = TagMatcher.findIncludes(listener.getTags()).stream().findFirst().orElse(null);
        String subscribeTopic = Objects.isNull(includeTag) ? topic : topic + "/#";
        client.subscribe(subscribeTopic, qos(), (ctx, arrivedTopic, message, payload) -> {
            try {
                String payloadStr = new String(payload, StandardCharsets.UTF_8);
                MQEvent event = serialization().deserialize(payloadStr, listener.payloadType());
                if (Objects.isNull(event)) {
                    log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(defaultConcat()));
                    return;
                }
                String messageId = messageId(message);
                if (StrKit.isNotEmpty(messageId)) {
                    event.setMsgId(messageId);
                }
                // 应用层 tag 过滤（tag 取 topic 末段）。
                String tag = null;
                int lastSlash = arrivedTopic.lastIndexOf('/');
                if (lastSlash >= 0) {
                    tag = arrivedTopic.substring(lastSlash + 1);
                }
                if (!TagMatcher.match(tag, listener.getTags())) {
                    return;
                }
                long deliveryMessageId = IdKit.getSnowflakeNextId();
                MicaMqttAcknowledgment ack = new MicaMqttAcknowledgment(deliveryMessageId, arrivedTopic, null);
                consume(listener, event, ack);
                if (!ack.isAcknowledged()) {
                    ack.ackSingle();
                }
            } catch (Throwable ex) {
                log.error("Consume mica-mqtt [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
            }
        });
        return true;
    }

    // ========================= 连接管理 =========================

    /**
     * QoS：注入原生客户端时取默认 QoS = QOS1，否则读 properties。
     */
    private MqttQoS qos() {
        return properties.mqttQoS();
    }

    private synchronized MqttClient client() {
        MqttClient c = clientRef.get();
        if (Objects.nonNull(c)) {
            return c;
        }
        c = properties.client();
        clientRef.set(c);
        return c;
    }

    static String messageId(MqttPublishMessage message) {
        String messageId = message.getProperties().getUserPropertiesMap().get(MessageHeaders.HEADER_MESSAGE_ID);
        return StrKit.isNotEmpty(messageId)
                ? messageId
                : message.getProperties().getUserPropertiesMap().get(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
    }
}
