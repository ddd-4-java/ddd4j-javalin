package io.ddd4j.mq.nats;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import io.nats.client.*;
import io.nats.client.impl.Headers;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * NATS JetStream 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 *
 * <p>NATS 无原生 broker-side tag filter（仅 subject 通配），应用层 {@link TagMatcher} tag 过滤保留。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : NatsMQClient ###")
public class NatsMQClient implements MQClient {

    private final NatsProperties properties;
    private final AtomicReference<Connection> connectionRef = new AtomicReference<>();

    /**
     * 双构造 1：注入已初始化的原生 NATS {@link Connection}（用于 runtime 集成自动注入）。
     *
     * @param connection 原生 NATS 连接
     */
    public NatsMQClient(Connection connection) {
        this.properties = new NatsProperties();
        if (Objects.nonNull(connection)) {
            this.connectionRef.set(connection);
        }
    }

    /**
     * 双构造 2：传入配置，{@link #connection()} 时 lazy 构造原生 NATS 连接。
     *
     * @param properties NATS 配置
     */
    public NatsMQClient(NatsProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String impl() {
        return "nats";
    }

    /**
     * NATS 无原生 broker-side tag selector，仅 subject 通配 → 强制应用层 {@link TagMatcher} 过滤。
     */
    @Override
    public boolean supportsBrokerTagFilter() {
        return false;
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        Connection conn = connection();
        return event -> {
            String payload = serialization().serialize(event);
            String subject = resolveTopic(event, mqProperties);
            try {
                byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                Headers headers = messageHeaders(event);
                // JetStream 优先，失败回落 core NATS（如未启用 JetStream）
                try {
                    JetStream jetStream = conn.jetStream();
                    jetStream.publish(subject, headers, body);
                } catch (IOException | JetStreamApiException ex) {
                    conn.publish(subject, headers, body);
                }
            } catch (Exception ex) {
                log.error("Publish NATS [{}]: {} failed!", subject, payload, ex);
                throw new IllegalStateException("Publish NATS event failed", ex);
            }
            log.info("Publish MQ [{}]: {}", subject, payload);
        };
    }

    // ========================= 消费者 =========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        Connection conn = connection();
        String subject = resolveTopic(listener, mqProperties);
        try {
            JetStream jetStream = conn.jetStream();
            Dispatcher dispatcher = conn.createDispatcher(msg -> {
            });
            PushSubscribeOptions options = PushSubscribeOptions.builder()
                    .durable(listener.getGroup())
                    .build();
            jetStream.subscribe(subject, dispatcher, msg -> onMessage(msg, listener), false, options);
            log.info("Registered NATS JetStream listener: subject={}, durable={}", subject, listener.getGroup());
        } catch (Exception ex) {
            log.warn("JetStream subscribe failed for subject={}, falling back to core NATS: {}",
                    subject, ex.getMessage());
            Dispatcher dispatcher = conn.createDispatcher(msg -> onMessage(msg, listener));
            dispatcher.subscribe(subject);
            log.info("Registered NATS core listener: subject={}", subject);
        }
        return true;
    }

    private void onMessage(Message natsMessage, MQListener listener) {
        try {
            String payload = new String(natsMessage.getData(), StandardCharsets.UTF_8);
            MQEvent event = serialization().deserialize(payload, listener.payloadType());
            if (Objects.isNull(event)) {
                log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(defaultConcat()));
                return;
            }
            restoreMessageId(event, messageId(natsMessage.getHeaders()));
            // 应用层 tag 过滤（tag 取 subject 末段）
            String tag = null;
            String subject = natsMessage.getSubject();
            int lastDot = subject.lastIndexOf('.');
            if (lastDot >= 0) {
                tag = subject.substring(lastDot + 1);
            }
            if (!TagMatcher.match(tag, listener.getTags())) {
                return;
            }
            NatsAcknowledgment ack = new NatsAcknowledgment(natsMessage);
            consume(listener, event, ack);
            if (!ack.isAcknowledged()) {
                ack.ackSingle();
            }
        } catch (Throwable ex) {
            log.error("NATS consumer failed: subject={}", natsMessage.getSubject(), ex);
            if (Objects.nonNull(natsMessage.metaData())) {
                try {
                    natsMessage.nak();
                } catch (Exception nakEx) {
                    log.warn("Failed to nak NATS message after error", nakEx);
                }
            }
        }
    }

    // ========================= 连接管理 =========================

    private synchronized Connection connection() {
        Connection c = connectionRef.get();
        if (Objects.isNull(c)) {
            c = properties.connect();
            connectionRef.set(c);
        }
        return c;
    }

    static Headers messageHeaders(MQEvent event) {
        Headers headers = new Headers();
        if (StrKit.isNotEmpty(event.getMsgId())) {
            headers.put(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
        }
        return headers;
    }

    static String messageId(Headers headers) {
        if (Objects.isNull(headers)) {
            return null;
        }
        String messageId = headers.getFirst(MessageHeaders.HEADER_MESSAGE_ID);
        return StrKit.isNotEmpty(messageId)
                ? messageId
                : headers.getFirst(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
    }

    private static void restoreMessageId(MQEvent event, String messageId) {
        if (StrKit.isNotEmpty(messageId)) {
            event.setMsgId(messageId);
        }
    }
}
