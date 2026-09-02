package io.ddd4j.mq.activemq;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.activemq.util.ActivemqKit;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.message.MessageHeaders;
import jakarta.jms.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.lang.IllegalStateException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * ActiveMQ (JMS) 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>主线只有 {@link #initProducer} 与 {@link #initConsumer}，核心业务逻辑全部内联。
 * 工具逻辑（payload 提取、messageId 读取、物理地址拼接、createQueue/createTopic 分支）下沉到
 * 无状态的 {@link ActivemqKit}，ack 映射下沉到 {@link ActiveMQAcknowledgment}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : ActiveMQClient ###")
public class ActiveMQClient implements MQClient {

    private final ActiveMQProperties properties;
    /**
     * 延迟初始化的 ActiveMQ 连接工厂（构造 1 注入时不创建）
     */
    private final ActiveMQConnectionFactory connectionFactory;
    /**
     * 共享 connection（第一个 init* 时懒构造）
     */
    private final AtomicReference<Connection> connectionRef = new AtomicReference<>();

    /**
     * 双构造 1：注入外部已配置好的 ActiveMQConnectionFactory（runtime 集成用）。
     * properties 默认为空，因为 properties 仅用于 publish 时的 message property 写入。
     */
    public ActiveMQClient(ActiveMQConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "ActiveMQ ConnectionFactory is required");
        this.properties = new ActiveMQProperties();
    }

    /**
     * 双构造 2：从 properties 自建 ActiveMQConnectionFactory。
     */
    public ActiveMQClient(ActiveMQProperties properties) {
        this.properties = Objects.requireNonNull(properties, "ActiveMQ Properties is required");
        this.connectionFactory = buildFactory(properties);
        log.info("Init ActiveMQ client with {}", properties);
    }

    /**
     * 从 properties 自建 ActiveMQConnectionFactory（构造 2 用）。
     */
    private static ActiveMQConnectionFactory buildFactory(ActiveMQProperties properties) {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(properties.getBrokerUrl());
        if (Objects.nonNull(properties.getUsername()) && StrKit.isNotBlank(properties.getUsername())) {
            factory.setUser(properties.getUsername());
        }
        if (Objects.nonNull(properties.getPassword()) && StrKit.isNotBlank(properties.getPassword())) {
            factory.setPassword(properties.getPassword());
        }
        factory.setClientID(Objects.requireNonNullElse(properties.getClientIdPrefix(), "ddd4j-mq-"));
        return factory;
    }

    // ========================= 生产者 =========================

    @Override
    public String impl() {
        return "activemq";
    }

    // ========================= 消费者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        try {
            // Producer-only session：AUTO_ACKNOWLEDGE 参数对 producer 无意义（JMS ack 模式仅对 consumer 生效），
            // 避免 ACK 语义被框架自动接管导致手动 ack 失效。
            final Session session = getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
            return mqEvent -> {
                String payload = serialization().serialize(mqEvent);
                String topic = resolveTopic(mqEvent, mqProperties);
                try (MessageProducer producer = session.createProducer(ActivemqKit.createDestination(session, topic))) {
                    producer.setDeliveryMode(properties.isDurable() ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
                    BytesMessage message = session.createBytesMessage();
                    message.writeBytes(payload.getBytes(StandardCharsets.UTF_8));
                    message.setStringProperty(jmsProperty(MessageHeaders.HEADER_DESTINATION_TOPIC), mqEvent.getTopic());
                    if (Objects.nonNull(mqEvent.getTag())) {
                        // tag header 与 broker 端 selector property name 一致（无 .，合法 SQL-92）
                        message.setStringProperty(jmsProperty(tagHeaderKey()), mqEvent.getTag());
                    }
                    if (Objects.nonNull(mqEvent.getTenantId())) {
                        message.setStringProperty(jmsProperty(MessageHeaders.HEADER_TENANT_ID), mqEvent.getTenantId());
                    }
                    if (Objects.nonNull(mqEvent.getMsgId())) {
                        // JMSMessageID 必须以 "ID:" 开头（Artemis checkProperty 校验）
                        message.setJMSMessageID("ID:" + mqEvent.getMsgId());
                        message.setStringProperty(jmsProperty(MessageHeaders.HEADER_MESSAGE_ID), mqEvent.getMsgId());
                    }
                    producer.send(message);
                } catch (JMSException ex) {
                    log.error("Publish MQ [{}]: {} failed!", topic, payload, ex);
                    throw new IllegalStateException("Publish ActiveMQ event failed", ex);
                }
                log.info("Publish MQ [{}]: {}", topic, payload);
            };
        } catch (JMSException ex) {
            throw new IllegalStateException("Init ActiveMQ producer failed", ex);
        }
    }

    // ========================= 连接管理（双构造共享的最小辅助）=========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) throws Exception {
        final Session session = getConnection().createSession(false, Session.CLIENT_ACKNOWLEDGE);
        String topic = resolveTopic(listener, mqProperties);
        // broker 端 tag 过滤：把 MQEventListener.tags 表达式翻译成 JMS Message Selector，
        // 不匹配的消息在 broker 端就过滤掉，不投递到 listener（broker 端精确过滤）。
        String selector = tagsToSelector(listener.getTags());
        MessageConsumer consumer = session.createConsumer(ActivemqKit.createDestination(session, topic), selector);
        consumer.setMessageListener(message -> {
            try {
                MQEvent event = serialization().deserialize(ActivemqKit.extractPayload(message), listener.payloadType());
                if (Objects.isNull(event)) {
                    log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(defaultConcat()));
                    return;
                }
                String messageId = messageId(message);
                if (Objects.nonNull(messageId)) {
                    event.setMsgId(messageId);
                }
                ActiveMQAcknowledgment ack = new ActiveMQAcknowledgment(
                        session, message,
                        ActivemqKit.messageIdHash(message),
                        ActivemqKit.messageIdOf(message),
                        ActivemqKit.correlationIdOf(message));
                this.consume(listener, event, ack);
                if (!ack.isAcknowledged()) {
                    ack.ackSingle();
                }
            } catch (Throwable ex) {
                log.error("Consume MQ [{}] failed", listener.getRouteExpression(defaultConcat()), ex);
                try {
                    session.recover();
                } catch (JMSException ignore) {
                }
            }
        });
        return true;
    }

    private synchronized Connection getConnection() {
        Connection connection = connectionRef.get();
        if (Objects.isNull(connection)) {
            try {
                Connection nc = connectionFactory.createConnection();
                nc.start();
                connectionRef.set(nc);
                connection = nc;
            } catch (JMSException ex) {
                throw new IllegalStateException("Open ActiveMQ connection failed", ex);
            }
        }
        log.info("Get ActiveMQ connection: {}", connection);
        return connection;
    }

    /**
     * 优先读取 ddd4j 标准消息 ID，兼容升级期旧键并最终回退到 JMS 原生 ID。
     */
    static String messageId(Message message) throws JMSException {
        String messageId = message.getStringProperty(jmsProperty(MessageHeaders.HEADER_MESSAGE_ID));
        if (Objects.isNull(messageId)) {
            messageId = message.getStringProperty(jmsProperty(MessageHeaders.LEGACY_HEADER_MESSAGE_ID));
        }
        return Objects.nonNull(messageId) ? messageId : ActivemqKit.messageIdOf(message);
    }
    /**
     * ddd4j 消息头统一使用 {@code ddd4j.xxx.yyy} 命名，JMS 场景需替换为 '_'。
     */
    static String jmsProperty(String name) {
        // JMS 属性名必须是合法 Java 标识符：'.' 与 '-' 均非法，统一替换为 '_'
        return name.replace('.', '_').replace('-', '_');
    }
}
