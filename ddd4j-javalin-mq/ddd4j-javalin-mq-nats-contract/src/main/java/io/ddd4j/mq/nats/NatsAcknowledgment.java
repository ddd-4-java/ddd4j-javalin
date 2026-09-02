package io.ddd4j.mq.nats;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 NATS JetStream {@link Message} 的消息确认实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public final class NatsAcknowledgment implements Acknowledgment {

    private final Message message;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    /**
     * 构造 NATS 确认对象。
     *
     * @param message JetStream 消息
     */
    public NatsAcknowledgment(Message message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public long deliveryTag() {
        return Objects.isNull(message.metaData()) ? 0L : message.metaData().consumerSequence();
    }

    @Override
    public String messageId() {
        return message.getSID();
    }

    @Override
    public String correlationId() {
        return message.getReplyTo();
    }

    @Override
    public boolean isOpen() {
        return !acknowledged.get();
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.NATS;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        ensureNotAcknowledged();
        if (multiple) {
            log.debug("NATS ignores multiple ack flag, acknowledging sequence={}", deliveryTag());
        }
        // 逻辑块：JetStream 确认成功消费
        message.ack();
        acknowledged.set(true);
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        ensureNotAcknowledged();
        if (multiple) {
            log.debug("NATS ignores multiple nack flag, sequence={}", deliveryTag());
        }
        if (!requeue) {
            throw new UnsupportedOperationException(
                    "NATS JetStream nack without requeue is not supported; use ack or nak with requeue");
        }
        // 逻辑块：否定确认并重新投递
        message.nak();
        acknowledged.set(true);
    }

    @Override
    public void reject(boolean requeue) {
        nack(requeue);
    }

    @Override
    public void recover(boolean requeue) {
        throw new UnsupportedOperationException("NATS does not support basicRecover semantics");
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        Objects.requireNonNull(nativeType, "nativeType");
        if (Message.class.isAssignableFrom(nativeType)) {
            return Optional.of(nativeType.cast(message));
        }
        if (NatsAcknowledgment.class.isAssignableFrom(nativeType)) {
            return Optional.of(nativeType.cast(this));
        }
        return Optional.empty();
    }

    /**
     * 返回底层 NATS 消息。
     */
    public Message message() {
        return message;
    }

    /**
     * 防止重复确认导致 JetStream 状态异常。
     */
    private void ensureNotAcknowledged() {
        if (acknowledged.get()) {
            throw new IllegalStateException("NATS message already acknowledged, sequence=" + deliveryTag());
        }
    }
}
