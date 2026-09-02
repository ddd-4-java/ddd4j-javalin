package io.ddd4j.mq.ons;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 阿里云 ONS 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 *
 * <p>ONS 是阿里云 RocketMQ，提供原生 subscription 表达式 tag 过滤。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : OnsMQClient ###")
public class OnsMQClient implements MQClient {

    private final OnsProperties properties;
    private final List<com.aliyun.openservices.ons.api.Consumer> consumers = new CopyOnWriteArrayList<>();
    private volatile Producer producer;
    private volatile boolean producerStarted;

    /**
     * 构造 1：传入配置，{@link #initProducer}/{@link #initConsumer} 中通过 ONSFactory 创建原生客户端。
     *
     * @param properties ONS 配置
     */
    public OnsMQClient(OnsProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 构造 2：注入已初始化的原生 ONS {@link Producer}（用于 runtime 集成自动注入）。
     * 注：ONS 消费者侧仍需 {@link OnsProperties} 通过 ONSFactory 创建，因此构造 2 同时接受
     * {@link OnsProperties}（供消费者侧使用）。
     *
     * @param producer   原生 ONS producer（运行时构造预创建）
     * @param properties ONS 配置（消费者侧使用）
     */
    public OnsMQClient(Producer producer, OnsProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.producer = producer;
    }

    @Override
    public String impl() {
        return "ons";
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        try {
            Producer p = Objects.nonNull(producer) ? producer
                    : ONSFactory.createProducer(properties.sessionProperties(properties.getProducerId()));
            p.start();
            this.producer = p;
            this.producerStarted = true;
            return event -> {
                try {
                    String topic = resolveTopic(event, mqProperties);
                    String tag = event.getTag();
                    Message msg = new Message(topic, tag, event.getMsgId(),
                            serialization().serialize(event).toString().getBytes(StandardCharsets.UTF_8));
                    msg.setKey(event.getMsgId());
                    if (StrKit.isNotEmpty(event.getMsgId())) {
                        msg.putUserProperties(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
                    }
                    if (Objects.nonNull(event.getTenantId())) {
                        msg.putUserProperties(MessageHeaders.HEADER_TENANT_ID, event.getTenantId());
                    }
                    p.send(msg);
                    logger().info("Publish MQ [{}]: {}", topic, serialization().serialize(event));
                } catch (Exception ex) {
                    throw new IllegalStateException("Publish ONS event failed", ex);
                }
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Init ONS producer failed", ex);
        }
    }

    // ========================= 消费者 =========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        String group = StrKit.hasText(listener.getGroup()) ? listener.getGroup() : properties.getConsumerId();
        if (!StrKit.hasText(group)) {
            throw new IllegalStateException("OnsClient requires consumerId or @MQEventListener(group=...)");
        }
        String topic = StrKit.hasText(listener.getTopic()) ? listener.getTopic() : properties.getTopic();
        if (!StrKit.hasText(topic)) {
            throw new IllegalStateException("OnsClient requires topic");
        }
        // ONS 支持 subscription 表达式原生 tag 过滤
        String tagExpression = properties.subscriptionExpression(
                TagMatcher.findIncludes(listener.getTags()).stream().findFirst().orElse(null));
        com.aliyun.openservices.ons.api.Consumer consumer = ONSFactory.createConsumer(properties.sessionProperties(group));
        consumer.subscribe(topic, tagExpression, (msg, ctx) -> {
            try {
                if (!TagMatcher.match(msg.getTag(), listener.getTags())) {
                    return com.aliyun.openservices.ons.api.Action.CommitMessage;
                }
                String payload = new String(msg.getBody(), StandardCharsets.UTF_8);
                MQEvent event = serialization().deserialize(payload, listener.payloadType());
                if (Objects.isNull(event)) {
                    logger().warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(this.defaultConcat()));
                    return com.aliyun.openservices.ons.api.Action.CommitMessage;
                }
                if (Objects.nonNull(msg.getTag())) {
                    event.setTag(msg.getTag());
                }
                String messageId = messageId(msg);
                if (StrKit.isNotEmpty(messageId)) {
                    event.setMsgId(messageId);
                } else if (StrKit.isEmpty(event.getMsgId()) && StrKit.isNotEmpty(msg.getMsgID())) {
                    event.setMsgId(msg.getMsgID());
                }
                OnsAcknowledgment ack = new OnsAcknowledgment(ctx, msg);
                try {
                    consume(listener, event, ack);
                    if (!ack.isAcknowledged()) {
                        ack.ackSingle();
                    }
                } catch (Throwable ex) {
                    logger().error("Consume MQ [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
                    return com.aliyun.openservices.ons.api.Action.ReconsumeLater;
                }
                return ack.action();
            } catch (Throwable ex) {
                logger().error("Consume MQ [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
                return com.aliyun.openservices.ons.api.Action.ReconsumeLater;
            }
        });
        consumer.start();
        consumers.add(consumer);
        return true;
    }

    @Override
    public void start() {
        // ONS producer/consumer 在各自创建时已启动；此方法留作 future 扩展。
    }

    // ========================= 关闭 =========================

    @Override
    public void close() {
        for (com.aliyun.openservices.ons.api.Consumer c : new ArrayList<>(consumers)) {
            try {
                c.shutdown();
            } catch (Exception ex) {
                logger().warn("Shutdown ONS consumer failed", ex);
            }
        }
        consumers.clear();
        if (producerStarted && Objects.nonNull(producer)) {
            try {
                producer.shutdown();
            } catch (Exception ex) {
                logger().warn("Shutdown ONS producer failed", ex);
            }
            producerStarted = false;
        }
    }

    static String messageId(Message message) {
        String messageId = message.getUserProperties(MessageHeaders.HEADER_MESSAGE_ID);
        return StrKit.isNotEmpty(messageId)
                ? messageId
                : message.getUserProperties(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
    }
}
