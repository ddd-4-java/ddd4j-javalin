package io.ddd4j.mq.kafka;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Kafka 客户端实现（对齐 base-mq KafkaClient，纯 Java 零 Spring 依赖）。
 *
 * <p>双构造：
 * <ul>
 *   <li>{@link #KafkaMQClient(Producer<String, String>)} —— 注入已初始化的原生 Kafka producer（runtime 自动装配用）</li>
 *   <li>{@link #KafkaMQClient(KafkaMQProperties)} —— 自行根据 properties 构造 producer</li>
 * </ul>
 *
 * <p>借鉴 1：分区 key（producer 按 tag/tenantId 路由，同 key 进同 partition 保证顺序）
 * <p>借鉴 2：producer send 异步 callback（非阻塞发送、统一 ack/nack 收口）
 * <p>借鉴 4：AdminClient 自动创建 topic（启动时确保 topic 存在，分区/副本数可配）
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : KafkaMQClient ###")
public class KafkaMQClient implements MQClient {

    /**
     * KafkaMQProperties 用于懒构造
     */
    private final KafkaMQProperties properties;
    /**
     * 已注入或懒构造的 Kafka producer
     */
    private Producer<String, String> producer;
    private Callback callback;

    /**
     * 构造方法 1：注入原生 producer（runtime 自动装配用）。
     */
    public KafkaMQClient(Producer<String, String> producer, Callback callback) {
        this.properties = null;
        this.producer = Objects.requireNonNull(producer, "KafkaMQ Producer is required");
        this.callback = callback;
    }

    /**
     * 构造方法 2：自行根据 properties 构造 producer（lazy）。
     */
    public KafkaMQClient(KafkaMQProperties properties, Callback callback) {
        this.properties = Objects.requireNonNull(properties, "KafkaMQ Properties is required");
        this.producer = null;
        this.callback = callback;
    }

    @Override
    public String impl() {
        return "kafka";
    }

    /**
     * Kafka topic 用 {@code "_"} 拼接 namespace（与 ActiveMQ 的 {@code "."} 区分）。
     */
    @Override
    public String defaultConcat() {
        return "_";
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        if (Objects.isNull(producer) && Objects.nonNull(this.properties)) {
            this.producer = new KafkaProducer<>(properties.producerProperties());
            // 启动时通过 AdminClient 确保 topic 存在
            if (properties.isAutoCreateTopics()) {
                ensureTopic(mqProperties.getNamespace(), mqProperties.getDefaultTopic());
            }
        }
        Producer<String, String> producer1 = this.producer;
        return mqEvent -> {
            String payload = serialization().serialize(mqEvent);
            String topic = resolveTopic(mqEvent, mqProperties);
            String key = partitionKey(mqEvent);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
            if (Objects.nonNull(mqEvent.getMsgId())) {
                record.headers().add(MessageHeaders.HEADER_MESSAGE_ID,
                        mqEvent.getMsgId().getBytes(StandardCharsets.UTF_8));
            }
            // 异步 send 回调（非阻塞发布、统一 ack/nack 收口）
            producer1.send(record, Objects.nonNull(callback) ? callback : new SendCallback(topic, payload));
            log.info("Publish MQ [{}]: {}", topic, payload);
        };
    }

    /**
     * 根据 {@link PartitionKeyStrategy} 枚举计算 partition key。
     * <p>Kafka 保证同 key 进同 partition → 同 tag / 同 tenantId 消息严格顺序。
     * <p>TAG_TENANT/TAG/TENANT 三种复用父类 {@link #partitionKey(MQEvent)} 默认行为；NONE 返回 null；
     * CUSTOM 时应由子类覆写 {@link #partitionKey(MQEvent)} 自定义。
     */
    @Override
    public String partitionKey(MQEvent event) {
        if (Objects.isNull(properties)) {
            return MQClient.super.partitionKey(event);  // 双构造 1：注入 producer 时走父类默认
        }
        return switch (properties.getPartitionKeyStrategy()) {
            case NONE -> null;
            case TAG -> Objects.nonNull(event) ? event.getTag() : null;
            case TENANT -> Objects.nonNull(event) ? event.getTenantId() : null;
            case TAG_TENANT -> MQClient.super.partitionKey(event);
            case CUSTOM -> MQClient.super.partitionKey(event);  // 占位：子类应自己覆写
        };
    }

    /**
     * 借鉴 4：通过 AdminClient 自动创建 topic（如不存在）。
     */
    private void ensureTopic(String namespace, String defaultTopic) {
        if (Objects.isNull(this.properties)) {
            return;
        }
        // 统一复用父类 resolveTopic（与 initProducer 内的 topic 解析保持一致）
        String topic = resolveTopic(namespace, defaultTopic, null, defaultConcat());
        try (AdminClient admin = AdminClient.create(properties.adminProperties())) {
            if (admin.listTopics().names().get().contains(topic)) {
                return;
            }
            NewTopic newTopic = new NewTopic(topic,
                    properties.getDefaultTopicPartitions(),
                    properties.getDefaultTopicReplication());
            admin.createTopics(Collections.singletonList(newTopic)).all().get();
            log.info("Kafka topic auto-created via AdminClient: {}", topic);
        } catch (Exception ex) {
            // 自动建 topic 失败不阻断 producer —— broker 可能禁用了自动创建或权限不足
            log.warn("Kafka AdminClient create topic failed (will retry on next publish): {}", ex.getMessage());
        }
    }

    @Override
    public boolean initConsumer(MQListener mqListener, MQProperties mqProperties) throws Exception {
        if (Objects.isNull(properties)) {
            return false;
        }
        Properties props = properties.consumerProperties(buildGroupId(mqListener));
        props.put("bootstrap.servers", properties.getBootstrapServers());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(resolveTopic(mqListener, mqProperties)));
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ddd4j-kafka-" + mqListener.getMethod().getName());
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    String payload = record.value();
                    MQEvent mqEvent = serialization().deserialize(payload, mqListener.payloadType());
                    if (Objects.isNull(mqEvent)) {
                        consumer.commitSync();
                        log.warn("Consume MQ [{}] failed: the mqEvent is null", mqListener.getRouteExpression(this.defaultConcat()));
                        continue;
                    }
                    if (!TagMatcher.match(mqEvent.getTag(), mqListener.getTags())) {
                        continue;
                    }
                    KafkaMessageAcknowledgment ack = new KafkaMessageAcknowledgment(consumer, record);
                    try {
                        consume(mqListener, mqEvent, ack);
                        if (!ack.isAcknowledged() && !mqProperties.isAutoAck()) {
                            consumer.commitSync();
                        }
                    } catch (Throwable e) {
                        log.error("Consume MQ [{}] failed: {}", mqListener.getTopic(), serialization().serialize(mqEvent), e);
                        if (!mqProperties.isAutoAck()) {
                            consumer.commitSync();
                        }
                    }
                }
            }
        });
        return true;
    }

    // ========================= 消费者 =========================

    /**
     * 构造 consumer group.id（兜底）。
     */
    private String buildGroupId(MQListener listener) {
        return StrKit.isNotEmpty(listener.getGroup())
                ? listener.getGroup()
                : "ddd4j-" + listener.getMethod().getName();
    }

    /**
     * 异步发送回调（统一收口，不阻塞 producer.send()）。
     */
    public static final class SendCallback implements Callback {

        private final String topic;
        private final String payload;

        SendCallback(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (Objects.nonNull(exception)) {
                log.error("Kafka send failed: topic={}, payload={}", topic, payload, exception);
            } else if (log.isDebugEnabled()) {
                log.debug("Kafka send success: topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        }
    }
}
