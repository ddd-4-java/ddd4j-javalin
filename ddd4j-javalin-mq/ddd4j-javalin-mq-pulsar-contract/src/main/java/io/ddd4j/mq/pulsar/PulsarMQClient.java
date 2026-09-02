package io.ddd4j.mq.pulsar;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Apache Pulsar 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : PulsarMQClient ###")
public class PulsarMQClient implements MQClient {

    private final PulsarProperties properties;
    private final List<org.apache.pulsar.client.api.Consumer<?>> consumers = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, Producer<byte[]>> producers = new ConcurrentHashMap<>();
    private PulsarClient client;

    /**
     * 构造 1：传入配置，{@link #initProducer} 中通过 PulsarClient.builder().build() 创建原生客户端。
     *
     * @param properties Pulsar 配置
     */
    public PulsarMQClient(PulsarProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.client = null;
    }

    /**
     * 构造 2：注入已初始化的原生 Pulsar 客户端（用于 runtime 集成自动注入）。
     * 物理 topic 仍来自 {@link PulsarProperties#physicalTopic}，因此同时接受 properties。
     *
     * @param client     原生 PulsarClient
     * @param properties Pulsar 配置（topic 命名空间、订阅名等）
     */
    public PulsarMQClient(org.apache.pulsar.client.api.PulsarClient client, PulsarProperties properties) {
        this.client = client;
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    private static String messageIdString(MessageId id) {
        return Objects.isNull(id) ? null : id.toString();
    }

    /**
     * 优先读取 ddd4j 标准消息 ID，兼容旧键，最后回退到 Pulsar 投递 ID。
     */
    static String messageId(Message<?> message) {
        String messageId = message.getProperty(MessageHeaders.HEADER_MESSAGE_ID);
        if (Objects.isNull(messageId)) {
            messageId = message.getProperty(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
        }
        return Objects.nonNull(messageId) ? messageId : messageIdString(message.getMessageId());
    }

    @Override
    public String impl() {
        return "pulsar";
    }

    // ========================= 生产者 =========================

    /**
     * 仿照 {@code KafkaMQClient}：根据 {@link io.ddd4j.mq.MQProperties#getPartitionKeyStrategy()}
     * 计算 Pulsar 消息 routing key，保证同 key 进同 partition（顺序投递）。
     *
     * <p>Pulsar 在 {@code Key_Shared} 订阅下按此 key 做哈希路由；生产端通过
     * {@code TypedMessageBuilder.key(...)} 写入。
     *
     * <ul>
     *   <li>NONE → {@code null}（轮询，无顺序保证）</li>
     *   <li>TAG → event.tag</li>
     *   <li>TENANT → event.tenantId</li>
     *   <li>TAG_TENANT → 复用父类 {@code tag|tenant} 复合 key（默认）</li>
     *   <li>CUSTOM → 父类默认（占位：子类自行覆写）</li>
     * </ul>
     */
    @Override
    public String partitionKey(MQEvent event) {
        if (Objects.isNull(properties)) {
            return MQClient.super.partitionKey(event);  // 双构造 2：注入 client 时走父类默认
        }
        return switch (properties.getPartitionKeyStrategy()) {
            case NONE -> null;
            case TAG -> Objects.nonNull(event) ? event.getTag() : null;
            case TENANT -> Objects.nonNull(event) ? event.getTenantId() : null;
            case TAG_TENANT -> MQClient.super.partitionKey(event);
            case CUSTOM -> MQClient.super.partitionKey(event);  // 占位：子类应自己覆写
        };
    }

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        try {
            if (Objects.isNull(this.client)) {
                this.client = properties.client();
            }
            return event -> {
                try {
                    String topic = event.getTopic();
                    String tag = event.getTag();
                    String physical = properties.physicalTopic(topic, tag);
                    Producer<byte[]> p = producer(physical);
                    TypedMessageBuilder<byte[]> builder = p.newMessage()
                            .value(serialization().serialize(event).toString().getBytes(StandardCharsets.UTF_8))
                            .property(MessageHeaders.HEADER_DESTINATION_TOPIC, topic);
                    String routingKey = partitionKey(event);
                    if (Objects.nonNull(routingKey)) {
                        builder.key(routingKey); // Pulsar partition routing：同 key 进同 partition
                    }
                    if (Objects.nonNull(event.getMsgId())) {
                        builder.property(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
                    }
                    if (Objects.nonNull(event.getTenantId())) {
                        builder.property(MessageHeaders.HEADER_TENANT_ID, event.getTenantId());
                    }
                    if (Objects.nonNull(tag)) {
                        builder.property(tagHeaderKey(), tag);
                    }
                    builder.sendAsync();
                    logger().info("Publish MQ [{}]: {}", physical, serialization().serialize(event));
                } catch (Exception ex) {
                    throw new IllegalStateException("Publish Pulsar event failed", ex);
                }
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Init Pulsar producer failed", ex);
        }
    }

    // ========================= 消费者 =========================

    private Producer<byte[]> producer(String topic) throws Exception {
        return producers.computeIfAbsent(topic, t -> {
            try {
                return client.newProducer(Schema.BYTES)
                        .topic(t)
                        .batchingMaxPublishDelay(10, TimeUnit.MILLISECONDS)
                        .create();
            } catch (Exception ex) {
                throw new IllegalStateException("Create Pulsar producer failed: " + t, ex);
            }
        });
    }

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        Objects.requireNonNull(listener, "listener");
        // Pulsar 物理 topic 用 tenant/namespace/topic 拼接，不含 tag（tag 作为 property 写入并应用层过滤）
        String topic = properties.physicalTopic(
                Objects.nonNull(listener.getTopic()) ? listener.getTopic() : "ddd4j.default.topic", null);
        String subscriptionName = properties.getSubscriptionName() + "-" + listener.getRouteExpression(this.defaultConcat());
        org.apache.pulsar.client.api.Consumer<byte[]> consumer = client.newConsumer(Schema.BYTES)
                .topic(topic)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.valueOf(properties.getSubscriptionType()))
                .negativeAckRedeliveryDelay(properties.getNegativeAckRedeliveryDelayMs(), TimeUnit.MILLISECONDS)
                .messageListener((c, msg) -> {
                    try {
                        String tag = msg.getProperty(tagHeaderKey());
                        if (!TagMatcher.match(tag, listener.getTags())) {
                            ((org.apache.pulsar.client.api.Consumer) c).acknowledge(msg);
                            return;
                        }
                        String messageId = messageId(msg);
                        String payload = new String(msg.getValue(), StandardCharsets.UTF_8);
                        MQEvent event = serialization().deserialize(payload, listener.payloadType());
                        if (Objects.isNull(event)) {
                            logger().warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(this.defaultConcat()));
                            ((org.apache.pulsar.client.api.Consumer) c).acknowledge(msg);
                            return;
                        }
                        if (Objects.nonNull(tag)) {
                            event.setTag(tag);
                        }
                        if (Objects.nonNull(messageId)) {
                            event.setMsgId(messageId);
                        }
                        PulsarAcknowledgment ack = new PulsarAcknowledgment(c, msg, messageId, null);
                        try {
                            consume(listener, event, ack);
                            if (!ack.isAcknowledged()) {
                                ack.ackSingle();
                            }
                        } catch (Throwable ex) {
                            logger().error("Consume MQ [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
                            try {
                                ((org.apache.pulsar.client.api.Consumer) c).negativeAcknowledge(msg);
                            } catch (Exception ignore) {
                            }
                        }
                    } catch (Throwable ex) {
                        logger().error("Consume MQ [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
                        try {
                            ((org.apache.pulsar.client.api.Consumer) c).negativeAcknowledge(msg);
                        } catch (Exception ignore) {
                        }
                    }
                })
                .subscribe();
        consumers.add(consumer);
        return true;
    }

    @Override
    public void start() {
        // Pulsar consumer 在 .subscribe() 时已启动
    }

    // ========================= 关闭 =========================

    @Override
    public void close() {
        for (org.apache.pulsar.client.api.Consumer<?> c : new ArrayList<>(consumers)) {
            try {
                c.close();
            } catch (Exception ex) {
                logger().warn("Close Pulsar consumer failed", ex);
            }
        }
        consumers.clear();
        for (Producer<byte[]> p : producers.values()) {
            try {
                p.close();
            } catch (Exception ex) {
                logger().warn("Close Pulsar producer failed", ex);
            }
        }
        producers.clear();
        if (Objects.nonNull(client)) {
            try {
                client.close();
            } catch (Exception ex) {
                logger().warn("Close Pulsar client failed", ex);
            }
        }
    }
}
