package io.ddd4j.mq.pulsar;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apache Pulsar 手动确认映射实现。
 *
 * <p>包装了 {@link Consumer} 和收到的 {@link Message}（Pulsar 消息，非 JMS 消息）。
 * Pulsar nack 语义：{@link Consumer#negativeAcknowledge(Message)} 重新投递消息（带可选延迟），
 * {@link Consumer#acknowledge(Message)} 确认消息。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class PulsarAcknowledgment implements Acknowledgment {

    /**
     * Header 键：Pulsar 消费者
     */
    public static final String HEADER_PULSAR_CONSUMER = "ddd4j.pulsar.consumer";
    /**
     * Header 键：Pulsar 消息
     */
    public static final String HEADER_PULSAR_MESSAGE = "ddd4j.pulsar.message";
    /**
     * Header 键：Pulsar 消息 ID
     */
    public static final String HEADER_PULSAR_MESSAGE_ID = "ddd4j.pulsar.messageId";

    /**
     * Pulsar 消费者实例
     */
    private final Consumer<?> consumer;
    /**
     * Pulsar 消息实例
     */
    private final Message<?> message;
    /**
     * 消息 ID
     */
    private final String messageId;
    /**
     * 关联 ID
     */
    private final String correlationId;
    /**
     * 投递 ID
     */
    private final long deliveryId;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    public PulsarAcknowledgment(Consumer<?> consumer, Message<?> message,
                                String messageId, String correlationId) {
        this.consumer = consumer;
        this.message = message;
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.deliveryId = Objects.isNull(messageId) ? 0L : Math.abs((long) messageId.hashCode());
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
        return Objects.nonNull(consumer) && consumer.isConnected();
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.PULSAR;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void ack(boolean multiple) {
        runOnce(() -> {
            if (multiple) {
                ((Consumer) consumer).acknowledgeCumulative(message.getMessageId());
            } else {
                ((Consumer) consumer).acknowledge(message);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void nack(boolean multiple, boolean requeue) {
        runOnce(() -> ((Consumer) consumer).negativeAcknowledge(message));
    }

    @Override
    public void reject(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void recover(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (Objects.isNull(type)) {
            return Optional.empty();
        }
        if (type.isInstance(consumer)) {
            return Optional.of(type.cast(consumer));
        }
        if (type.isInstance(message)) {
            return Optional.of(type.cast(message));
        }
        if (type.isInstance(messageId())) {
            return Optional.of(type.cast(messageId()));
        }
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }
        return Optional.empty();
    }

    private void runOnce(IoOperation op) {
        if (!acknowledged.compareAndSet(false, true)) {
            throw new UnsupportedOperationException(
                    "Pulsar message already acknowledged, id=" + messageId);
        }
        try {
            op.run();
        } catch (Exception ex) {
            acknowledged.set(false);
            throw new IllegalStateException("Pulsar ack failed, id=" + messageId, ex);
        }
    }

    @FunctionalInterface
    private interface IoOperation {
        void run() throws Exception;
    }
}
