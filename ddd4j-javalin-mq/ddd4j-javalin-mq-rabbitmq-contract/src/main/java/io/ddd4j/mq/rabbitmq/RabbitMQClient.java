package io.ddd4j.mq.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * RabbitMQ 客户端实现（对齐 base-mq RabbitClient，纯 Java 零 Spring 依赖）。
 *
 * <p>命名 {@code RabbitMQClient}。
 *
 * <p>双构造：
 * <ul>
 *   <li>{@link #RabbitMQClient(Connection)} —— 注入已初始化的原生 connection（runtime 自动装配用）</li>
 *   <li>{@link #RabbitMQClient(RabbitMQProperties)} —— 自行根据 properties 构造 connection（lazy）</li>
 * </ul>
 *
 * <p>路由键（routingKey）通过 {@link MQClient#resolveTopic(MQEvent, MQProperties)} 解析，
 * 队列名（queue）= {@code group.namespace.className.methodName}。
 *
 * <p>RabbitMQ topic exchange 用 {@code *}/{@code #} 模式，与 ddd4j 的 {@code *}/{@code ||}/{@code -}
 * 标签表达式不兼容，故 broker 端 tag 过滤走应用层 {@link TagMatcher#match}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : rabbitMQClient ###")
public class RabbitMQClient implements MQClient {

    /**
     * 已注入或懒构造的 RabbitMQ connection
     */
    private final AtomicReference<Connection> connectionRef = new AtomicReference<>();
    /**
     * 懒构造使用的配置（构造方法 2 传入）
     */
    private final RabbitMQProperties properties;

    /**
     * 构造方法 1：注入原生 connection（runtime 自动装配用）。
     */
    public RabbitMQClient(Connection connection) {
        this.connectionRef.set(Objects.requireNonNull(connection, "RabbitMQ Connection is required"));
        this.properties = null;
    }

    /**
     * 构造方法 2：自行根据 properties 构造 connection（lazy）。
     */
    public RabbitMQClient(RabbitMQProperties properties) {
        this.connectionRef.set(null);
        this.properties = Objects.requireNonNull(properties, "RabbitMQ Properties is required");
    }

    @Override
    public String impl() {
        return "rabbit";
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        try {
            Channel channel = connection().createChannel();
            String exchange = mqProperties.getExchange();
            return event -> {
                String payload = serialization().serialize(event);
                String topic = resolveTopic(event, mqProperties);
                try {
                    Map<String, Object> headers = new HashMap<>();
                    if (Objects.nonNull(event.getMsgId())) {
                        headers.put(MessageHeaders.HEADER_MESSAGE_ID, event.getMsgId());
                    }
                    if (Objects.nonNull(event.getTenantId())) {
                        headers.put(MessageHeaders.HEADER_TENANT_ID, event.getTenantId());
                    }
                    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                            .messageId(event.getMsgId())
                            .headers(headers)
                            .build();
                    channel.basicPublish(exchange, topic, properties, payload.getBytes(StandardCharsets.UTF_8));
                    log.info("Publish MQ [{}]: {}", topic, payload);
                } catch (Exception e) {
                    log.error("Publish MQ [{}]: {} failed!", topic, payload, e);
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException("Init RabbitMQ producer failed", e);
        }
    }

    // ========================= 消费者 =========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        Connection connection = connection();
        // 消费者使用独立 channel（长生命周期，basicConsume 需要保持打开）
        Channel channel = connection.createChannel();
        // 队列名=group.namespace.className.methodName
        String queue = listener.getGroup() + "." + listener.getNamespace() + "."
                + listener.getMethod().getDeclaringClass().getSimpleName() + "."
                + listener.getMethod().getName();
        // 解析订阅 routingKey（监听器侧，与生产者同源）
        String subscribeRoutingKey = resolveTopic(listener, mqProperties);
        // 应用层过滤：保留全部正向 tag（作为额外 routing key 订阅），
        // 主订阅由 resolveTopic 取首个正向 tag 拼出。
        List<String> routingKeys = new ArrayList<>();
        routingKeys.add(subscribeRoutingKey);
        if (StrKit.isNotEmpty(listener.getTags())) {
            Set<String> tags = TagMatcher.findIncludes(listener.getTags());
            for (String tag : tags) {
                String rk = resolveTopic(namespace(listener.getNamespace(), mqProperties),
                        listener.getTopic(), tag, concat(null));
                if (!routingKeys.contains(rk)) {
                    routingKeys.add(rk);
                }
            }
        }
        channel.queueDeclare(queue, true, false, false, null);
        String exchange = mqProperties.getExchange();
        for (String routingKey : routingKeys) {
            channel.queueBind(queue, exchange, routingKey);
        }
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            MQEvent event;
            try {
                event = serialization().deserialize(payload, listener.payloadType());
            } catch (Throwable ex) {
                log.error("Consume MQ [{}] deserialize failed: {}", listener.getRouteExpression(this.defaultConcat()), payload, ex);
                if (!mqProperties.isAutoAck()) {
                    try {
                        channel.basicAck(deliveryTag, false);
                    } catch (IOException ignore) {
                    }
                }
                return;
            }
            if (Objects.isNull(event)) {
                log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(this.defaultConcat()));
                if (!mqProperties.isAutoAck()) {
                    try {
                        channel.basicAck(deliveryTag, false);
                    } catch (IOException ignore) {
                    }
                }
                return;
            }
            String messageId = messageId(delivery.getProperties());
            if (Objects.nonNull(messageId)) {
                event.setMsgId(messageId);
            }
            // 应用层 tag 过滤（RabbitMQ topic exchange 模式与 ddd4j 表达式不兼容，broker 端不可用）
            if (!TagMatcher.match(event.getTag(), listener.getTags())) {
                if (!mqProperties.isAutoAck()) {
                    try {
                        channel.basicAck(deliveryTag, false);
                    } catch (IOException ignore) {
                    }
                }
                return;
            }
            RabbitAcknowledgment ack = new RabbitAcknowledgment(channel, deliveryTag, event.getMsgId(), null);
            try {
                consume(listener, event, ack);
                if (!mqProperties.isAutoAck() && !ack.isAcknowledged()) {
                    ack.ackSingle();
                }
            } catch (Throwable e) {
                log.error("Consume MQ [{}] failed: {}", listener.getRouteExpression(this.defaultConcat()),
                        serialization().serialize(event), e);
                if (!mqProperties.isAutoAck() && !ack.isAcknowledged()) {
                    try {
                        channel.basicNack(deliveryTag, false, true);
                    } catch (IOException ignore) {
                    }
                }
            }
        };
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ddd4j-rabbit-" + listener.getMethod().getName());
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            try {
                channel.basicConsume(queue, mqProperties.isAutoAck(), deliverCallback, consumerTag -> {
                });
            } catch (Exception e) {
                log.error("Consume MQ [{}] basicConsume failed", listener.getRouteExpression(this.defaultConcat()), e);
            }
        });
        return true;
    }

    /**
     * 优先读取 ddd4j 标准消息 ID，兼容旧键，最后回退到 AMQP 原生 messageId。
     */
    static String messageId(AMQP.BasicProperties properties) {
        Map<String, Object> headers = properties.getHeaders();
        if (Objects.nonNull(headers)) {
            Object headerValue = headers.get(MessageHeaders.HEADER_MESSAGE_ID);
            if (Objects.isNull(headerValue)) {
                headerValue = headers.get(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
            }
            if (Objects.nonNull(headerValue)) {
                return headerValue.toString();
            }
        }
        return properties.getMessageId();
    }

    // ========================= 连接管理（双构造共享的最小辅助）=========================

    private Connection connection() {
        Connection c = connectionRef.get();
        if (Objects.isNull(c)) {
            ConnectionFactory factory = properties.connectionFactory();
            try {
                Connection nc = factory.newConnection();
                if (connectionRef.compareAndSet(null, nc)) {
                    c = nc;
                } else {
                    c = connectionRef.get();
                    try {
                        nc.close();
                    } catch (IOException ignore) {
                    }
                }
            } catch (IOException | TimeoutException e) {
                throw new IllegalStateException("Open RabbitMQ connection failed", e);
            }
        }
        return c;
    }
}
