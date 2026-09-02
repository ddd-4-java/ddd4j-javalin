package io.ddd4j.mq.activemq;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ActiveMQ Classic (JMS) 手动确认映射实现。
 *
 * <p>包装了 {@link Session} 和 JMS 消息；{@link #ack()} / {@link #nack(boolean)} 方法映射到
 * {@code Session.recover()} 和 JMS {@code Message.acknowledge()} 操作。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class ActiveMQAcknowledgment implements Acknowledgment {

    /**
     * Header 键：ActiveMQ JMS Session
     */
    public static final String HEADER_AMQ_SESSION = "ddd4j.activemq.session";
    /**
     * Header 键：ActiveMQ JMS Message
     */
    public static final String HEADER_AMQ_MESSAGE = "ddd4j.activemq.message";
    /**
     * Header 键：ActiveMQ 投递 ID
     */
    public static final String HEADER_AMQ_DELIVERY_ID = "ddd4j.activemq.deliveryId";

    /**
     * JMS Session 实例
     */
    private final Session session;
    /**
     * JMS 消息实例
     */
    private final Message message;
    /**
     * 投递 ID
     */
    private final long deliveryId;
    /**
     * 消息 ID
     */
    private final String messageId;
    /**
     * 关联 ID
     */
    private final String correlationId;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    /**
     * 构造 ActiveMQ 消息确认实例。
     *
     * @param session       JMS Session
     * @param message       JMS 消息
     * @param deliveryId    投递 ID
     * @param messageId     消息 ID
     * @param correlationId 关联 ID
     */
    public ActiveMQAcknowledgment(Session session, Message message,
                                  long deliveryId, String messageId, String correlationId) {
        this.session = session;
        this.message = message;
        this.deliveryId = deliveryId;
        this.messageId = messageId;
        this.correlationId = correlationId;
    }

    @Override
    public long deliveryTag() {
        return deliveryId;
    }

    @Override
    public String messageId() {
        return messageId;
    }

    @Override
    public String correlationId() {
        return correlationId;
    }

    @Override
    public boolean isOpen() {
        return Objects.nonNull(session) && !acknowledged.get();
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.ACTIVEMQ;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        runOnce(message::acknowledge);
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        // JMS 没有 native 的 nack(requeue)，通过 Session.recover() 把已消费但未 ack 的消息重投
        if (requeue) {
            runOnce(session::recover);
        } else {
            // requeue=false 等价于 ack（不重新入队，通常进 DLQ）
            runOnce(message::acknowledge);
        }
    }

    @Override
    public void reject(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void recover(boolean requeue) {
        runOnce(session::recover);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        if (Objects.isNull(nativeType)) {
            return Optional.empty();
        }
        if (nativeType.isInstance(session)) {
            return Optional.of(nativeType.cast(session));
        }
        if (nativeType.isInstance(message)) {
            return Optional.of(nativeType.cast(message));
        }
        if (nativeType.isInstance(this)) {
            return Optional.of(nativeType.cast(this));
        }
        return Optional.empty();
    }

    /**
     * 确保确认操作只执行一次，防止重复 ack/nack。
     *
     * @param op 要执行的 IO 操作
     * @throws UnsupportedOperationException 如果消息已被确认
     * @throws IllegalStateException         如果确认操作失败
     */
    private void runOnce(IoOperation op) {
        if (!acknowledged.compareAndSet(false, true)) {
            throw new UnsupportedOperationException(
                    "ActiveMQ message already acknowledged, deliveryId=" + deliveryId);
        }
        try {
            op.run();
        } catch (JMSException ex) {
            acknowledged.set(false);
            throw new IllegalStateException(
                    "ActiveMQ acknowledgment failed, deliveryId=" + deliveryId, ex);
        }
    }

    /**
     * JMS IO 操作函数式接口，用于包装可能抛出 {@link JMSException} 的操作。
     */
    @FunctionalInterface
    private interface IoOperation {
        /**
         * 执行 IO 操作。
         *
         * @throws JMSException 如果操作失败
         */
        void run() throws JMSException;
    }
}
