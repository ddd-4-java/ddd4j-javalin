package io.ddd4j.mq.sqs;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AWS SQS 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 *
 * <p>SQS 没有 topic/tag 概念：{@code MQListener.topic} 必须直接是 queueUrl。tag 通过
 * {@link MessageHeaders#HEADER_DESTINATION_TAG} 属性传递，仅用于客户端过滤。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : SqsMQClient ###")
public class SqsMQClient implements MQClient {

    private final SqsProperties properties;
    private final List<ScheduledExecutorService> pollers = new CopyOnWriteArrayList<>();
    private SqsClient client;

    /**
     * 构造 1：传入配置，{@link #initProducer} 时 lazy 创建 SqsClient。
     */
    public SqsMQClient(SqsProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 构造 2：注入已初始化的原生 {@link SqsClient}（用于 runtime 集成自动注入）。
     */
    public SqsMQClient(SqsClient client) {
        this.client = Objects.requireNonNull(client, "SqsClient");
        this.properties = new SqsProperties();
    }

    private static MessageAttributeValue attrValue(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value).build();
    }

    static String attr(Message message, String key) {
        if (Objects.isNull(message.messageAttributes())) {
            return null;
        }
        var v = message.messageAttributes().get(key);
        return Objects.isNull(v) ? null : v.stringValue();
    }

    static String messageId(Message message) {
        String messageId = attr(message, MessageHeaders.HEADER_MESSAGE_ID);
        if (Objects.isNull(messageId)) {
            messageId = attr(message, MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
        }
        return messageId;
    }

    // ========================= 生产者 =========================

    @Override
    public String impl() {
        return "sqs";
    }

    /**
     * SQS 无原生 tag selector 机制，tag 过滤只能在应用层用 {@link TagMatcher#match} 完成
     * （不匹配的消息直接 {@code deleteMessage} 丢弃，避免无限重投）。故覆写返回 false。
     */
    @Override
    public boolean supportsBrokerTagFilter() {
        return false;
    }

    // ========================= 消费者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        if (Objects.isNull(this.client)) {
            this.client = properties.client();
        }
        return event -> {
            try {
                String queueUrl = event.getTopic();
                Map<String, MessageAttributeValue> attrs = new HashMap<>();
                if (Objects.nonNull(queueUrl)) {
                    attrs.put(MessageHeaders.HEADER_DESTINATION_TOPIC, attrValue(queueUrl));
                }
                if (Objects.nonNull(event.getTenantId())) {
                    attrs.put(MessageHeaders.HEADER_TENANT_ID, attrValue(event.getTenantId()));
                }
                if (Objects.nonNull(event.getMsgId())) {
                    attrs.put(MessageHeaders.HEADER_MESSAGE_ID, attrValue(event.getMsgId()));
                }
                if (Objects.nonNull(event.getTag())) {
                    attrs.put(tagHeaderKey(), attrValue(event.getTag()));
                }
                client.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(serialization().serialize(event).toString())
                        .messageAttributes(attrs)
                        .build());
                logger().info("Publish MQ [{}]: {}", queueUrl, serialization().serialize(event));
            } catch (Exception ex) {
                throw new IllegalStateException("Publish SQS event failed", ex);
            }
        };
    }

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        String queueUrl = listener.getTopic();
        if (Objects.isNull(queueUrl) || !(queueUrl.startsWith("http://") || queueUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("SQS MQListener.topic must be a queueUrl (https://...). Got: " + queueUrl);
        }
        AtomicBoolean running = new AtomicBoolean(true);
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sqs-consumer-" + listener.getRouteExpression(this.defaultConcat()));
            t.setDaemon(true);
            return t;
        });
        exec.scheduleWithFixedDelay(() -> {
            if (!running.get()) {
                return;
            }
            try {
                List<Message> messages = client.receiveMessage(ReceiveMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .maxNumberOfMessages(properties.getMaxNumberOfMessages())
                                .waitTimeSeconds(properties.getWaitTimeSeconds())
                                .messageAttributeNames("All")
                                .build())
                        .messages();
                for (Message message : messages) {
                    handleMessage(message, queueUrl, listener);
                }
            } catch (Exception ex) {
                logger().warn("SQS receive failed: binding={}", listener.getRouteExpression(this.defaultConcat()), ex);
            }
        }, 0, properties.getPollIntervalMs(), TimeUnit.MILLISECONDS);
        pollers.add(exec);
        return true;
    }

    private void handleMessage(Message message, String queueUrl, MQListener listener) {
        String tag = attr(message, tagHeaderKey());
        if (!TagMatcher.match(tag, listener.getTags())) {
            try {
                client.deleteMessage(b -> b.queueUrl(queueUrl).receiptHandle(message.receiptHandle()));
            } catch (Exception ex) {
                logger().warn("SQS deleteMessage failed: binding={}", listener.getRouteExpression(this.defaultConcat()), ex);
            }
            return;
        }
        try {
            SqsAcknowledgment ack = new SqsAcknowledgment(client, message, queueUrl, properties.isRequeueOnNack());
            String tenantId = attr(message, MessageHeaders.HEADER_TENANT_ID);
            String payload = message.body();
            MQEvent event = serialization().deserialize(payload, listener.payloadType());
            if (Objects.isNull(event)) {
                logger().warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(this.defaultConcat()));
                ack.ackSingle();
                return;
            }
            if (Objects.nonNull(tenantId)) {
                event.setTenantId(tenantId);
            }
            String msgId = messageId(message);
            if (Objects.nonNull(msgId)) {
                event.setMsgId(msgId);
            }
            if (Objects.nonNull(tag)) {
                event.setTag(tag);
            }
            try {
                consume(listener, event, ack);
                if (!ack.isAcknowledged()) {
                    ack.ackSingle();
                }
            } catch (Throwable ex) {
                logger().error("Consume MQ [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
                try {
                    client.changeMessageVisibility(b -> b.queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .visibilityTimeout(0));
                } catch (Exception ignore) {
                }
            }
        } catch (Throwable ex) {
            logger().error("Consume MQ [{}] failed", listener.getRouteExpression(this.defaultConcat()), ex);
            try {
                client.changeMessageVisibility(b -> b.queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .visibilityTimeout(0));
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void start() {
        // SQS 守护线程在 initConsumer 已 scheduleWithFixedDelay
    }

    // ========================= 关闭 =========================

    @Override
    public void close() {
        for (ScheduledExecutorService exec : pollers) {
            exec.shutdownNow();
        }
        pollers.clear();
        if (Objects.nonNull(client)) {
            client.close();
        }
    }
}
