package io.ddd4j.mq.rocketmq;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * RocketMQ 客户端实现（对齐 base-mq RocketClient，纯 Java 零 Spring 依赖）。
 *
 * <p>命名 {@code RocketMQClient}。
 *
 * <p>双构造：
 * <ul>
 *   <li>{@link #RocketMQClient(DefaultMQProducer)} —— 注入已初始化的原生 producer（runtime 自动装配用）</li>
 *   <li>{@link #RocketMQClient(RocketMQProperties)} —— 自行根据 properties 构造 producer（lazy）</li>
 * </ul>
 *
 * <p>借鉴 Kafka：
 * <ul>
 *   <li>partition key：producer 按 {@link MessageQueueSelector} 路由，同 key 进同 queue 保证顺序</li>
 *   <li>producer send 异步 callback（非阻塞发送）</li>
 *   <li>tag 过滤走 RocketMQ 原生 subscribe 表达式（{@code tagA || tagB}），应用层 TagMatcher 兜底 excludes</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : rocketMQClient ###")
public class RocketMQClient implements MQClient {

    /**
     * 按 partition key 哈希选 MessageQueue（同 key 必进同 queue）。
     */
    private static final MessageQueueSelector SELECTOR_BY_KEY = (mqs, msg, arg) -> {
        String key = Objects.toString(arg, "");
        int index = Math.abs(key.hashCode()) % mqs.size();
        return mqs.get(index);
    };
    /**
     * 懒构造使用的配置（构造方法 2 传入）
     */
    private final RocketMQProperties properties;
    /**
     * 已注入或懒构造的 RocketMQ producer
     */
    private volatile DefaultMQProducer producer;
    /**
     * 异步发送回调（可为 null，则用内置兜底）。
     */
    private SendCallback callback;

    /**
     * 构造方法 1：注入原生 producer（runtime 自动装配用）。
     */
    public RocketMQClient(DefaultMQProducer producer) {
        this.producer = Objects.requireNonNull(producer, "RocketMQ Producer is required");
        this.properties = null;
    }

    /**
     * 构造方法 1'：注入原生 producer + 异步发送回调。
     */
    public RocketMQClient(DefaultMQProducer producer, SendCallback callback) {
        this.producer = Objects.requireNonNull(producer, "RocketMQ Producer is required");
        this.properties = null;
        this.callback = callback;
    }

    /**
     * 构造方法 2：自行根据 properties 构造 producer（lazy）。
     */
    public RocketMQClient(RocketMQProperties properties) {
        this.producer = null;
        this.properties = Objects.requireNonNull(properties, "RocketMQ Properties is required");
    }

    /**
     * 构造方法 2'：自行根据 properties 构造 producer + 异步发送回调（lazy）。
     */
    public RocketMQClient(RocketMQProperties properties, SendCallback callback) {
        this.producer = null;
        this.properties = Objects.requireNonNull(properties, "RocketMQ Properties is required");
        this.callback = callback;
    }

    // ========================= 生产者 =========================

    @Override
    public String impl() {
        return "rocket";
    }

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        try {
            if (Objects.isNull(producer) && Objects.nonNull(this.properties)) {
                DefaultMQProducer p;
                String username = this.properties.getUsername();
                String password = this.properties.getPassword();
                if (StrKit.isNotEmpty(username) && StrKit.isNotEmpty(password)) {
                    p = new DefaultMQProducer(this.properties.getProducerGroup(),
                            new AclClientRPCHook(new SessionCredentials(username, password)));
                } else {
                    p = new DefaultMQProducer(this.properties.getProducerGroup());
                }
                if (StrKit.isNotEmpty(this.properties.getNameServer())) {
                    p.setNamesrvAddr(this.properties.getNameServer());
                }
                p.start();
                this.producer = p;
                log.info("Init RocketMQ producer with {}", this.properties);
            }
            DefaultMQProducer finalProducer = this.producer;
            return event -> {
                String payload = serialization().serialize(event);
                String topic = resolveTopic(event, mqProperties);
                String tag = Objects.isNull(event.getTag()) ? "" : event.getTag();
                try {
                    Message msg = new Message(event.getTopic(), tag, payload.getBytes(StandardCharsets.UTF_8));
                    // tag header 与 broker 端 selector property name 一致（无 .，合法标识符）
                    if (Objects.nonNull(event.getTag())) {
                        msg.putUserProperty(tagHeaderKey(), event.getTag());
                    }
                    if (Objects.nonNull(event.getTenantId())) {
                        msg.putUserProperty("ddd4jTenantId", event.getTenantId());
                    }
                    if (StrKit.isNotEmpty(event.getMsgId())) {
                        msg.putUserProperty(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
                    }
                    String key = partitionKey(event);
                    if (StrKit.isEmpty(key)) {
                        finalProducer.send(msg, Objects.nonNull(callback) ? callback : new SendLogCallback(topic, payload));
                    } else {
                        // 同 key 进同 queue：MessageQueueSelector 按 key 哈希选 queue，保证顺序
                        finalProducer.send(msg, SELECTOR_BY_KEY, key,
                                Objects.nonNull(callback) ? callback : new SendLogCallback(topic, payload));
                    }
                    log.info("Publish MQ [{}]: {}", topic, payload);
                } catch (Exception e) {
                    log.error("Publish MQ [{}]: {} failed!", topic, payload, e);
                }
            };
        } catch (MQClientException e) {
            log.error("Init RocketMQ producer failed", e);
            return null;
        }
    }

    /**
     * 根据 {@link PartitionKeyStrategy} 枚举计算 partition key（仿 KafkaMQClient）。
     * <p>RocketMQ 借 {@link MessageQueueSelector} 保证同 key 进同 MessageQueue → 同 tag / 同租户消息严格顺序。
     * <p>TAG_TENANT/TAG/TENANT 三种复用父类默认行为；NONE 返回 null；
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

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        DefaultMQPushConsumer consumer;
        if (Objects.nonNull(this.properties)) {
            consumer = this.properties.newConsumer(listener.getGroup());
            String username = this.properties.getUsername();
            String password = this.properties.getPassword();
            if (StrKit.isNotEmpty(username) && StrKit.isNotEmpty(password)) {
                consumer = new DefaultMQPushConsumer(listener.getGroup(),
                        new AclClientRPCHook(new SessionCredentials(username, password)));
                if (StrKit.isNotEmpty(this.properties.getNameServer())) {
                    consumer.setNamesrvAddr(this.properties.getNameServer());
                }
            }
        } else {
            consumer = new DefaultMQPushConsumer(listener.getGroup());
        }
        // broker 端 tag 过滤：RocketMQ 原生 subscribe(topic, tagExpression) 由 broker 端按表达式过滤；
        // 仅当 tags 含纯正向 include 时下发精确表达式（tagA || tagB），否则回退 * + 应用层 TagMatcher 兜底。
        Set<String> includes = TagMatcher.findIncludes(listener.getTags());
        String subscription = includes.isEmpty() ? "*"
                : includes.stream().collect(Collectors.joining(" || "));
        String topic = resolveTopic(listener, mqProperties);
        consumer.subscribe(listener.getTopic(), subscription);
        consumer.registerMessageListener((MessageListenerConcurrently) (messageExts, context) -> {
            for (MessageExt messageExt : messageExts) {
                String payload = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                MQEvent event;
                try {
                    event = serialization().deserialize(payload, listener.payloadType());
                } catch (Throwable ex) {
                    log.error("Consume MQ [{}] deserialize failed: {}", listener.getRouteExpression(this.defaultConcat()), payload, ex);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                if (Objects.isNull(event)) {
                    log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(this.defaultConcat()));
                    continue;
                }
                String messageId = messageId(messageExt);
                if (StrKit.isNotEmpty(messageId)) {
                    event.setMsgId(messageId);
                } else if (StrKit.isEmpty(event.getMsgId()) && StrKit.isNotEmpty(messageExt.getMsgId())) {
                    event.setMsgId(messageExt.getMsgId());
                }
                // 若 subscribe 没做精确过滤（含 excludes/通配），在应用层再用 TagMatcher 兜底
                if (includes.isEmpty() && !TagMatcher.match(event.getTag(), listener.getTags())) {
                    continue;
                }
                RocketAcknowledgment ack = new RocketAcknowledgment(messageExt);
                try {
                    consume(listener, event, ack);
                    if (ack.shouldReconsume()) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                    if (!ack.isAcknowledged()) {
                        // RocketMQ 无显式单条 ack，consume 正常返回即视为消费成功
                    }
                } catch (Throwable ex) {
                    log.error("Consume MQ [{}] failed: {}", listener.getRouteExpression(this.defaultConcat()),
                            serialization().serialize(event), ex);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        log.info("Listen MQ [{}]: topic={}, tags={}", impl(), topic, subscription);
        consumer.start();
        return true;
    }

    // ========================= 消费者 =========================

    /**
     * 异步发送回调（统一收口，不阻塞 producer.send()）。
     */
    public static final class SendLogCallback implements SendCallback {

        private final String topic;
        private final String payload;

        SendLogCallback(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
            if (log.isDebugEnabled()) {
                log.debug("RocketMQ send success: topic={}, msgId={}", topic, sendResult.getMsgId());
            }
        }

        @Override
        public void onException(Throwable e) {
            log.error("RocketMQ send failed: topic={}, payload={}", topic, payload, e);
        }
    }

    static String messageId(MessageExt message) {
        String messageId = message.getUserProperty(MessageHeaders.HEADER_MESSAGE_ID);
        return StrKit.isNotEmpty(messageId)
                ? messageId
                : message.getUserProperty(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
    }
}
