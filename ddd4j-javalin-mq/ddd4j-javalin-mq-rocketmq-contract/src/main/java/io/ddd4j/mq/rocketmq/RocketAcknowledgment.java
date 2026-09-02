package io.ddd4j.mq.rocketmq;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RocketMQ acknowledgment state adapter.
 *
 * <p>RocketMQ returns consume status from listener instead of exposing broker-level ack calls.
 * This adapter records whether ddd4j wants the message consumed successfully or retried.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RocketAcknowledgment implements Acknowledgment {

    public static final String HEADER_ROCKET_MESSAGE = "ddd4j.rocket.message";

    private final MessageExt message;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);
    private volatile boolean reconsume;

    public RocketAcknowledgment(MessageExt message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public long deliveryTag() {
        return message.getQueueOffset();
    }

    @Override
    public String messageId() {
        return message.getMsgId();
    }

    @Override
    public String correlationId() {
        return message.getKeys();
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
        return BrokerType.ROCKET;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        complete(false);
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        complete(requeue);
    }

    @Override
    public void reject(boolean requeue) {
        complete(requeue);
    }

    @Override
    public void recover(boolean requeue) {
        complete(requeue);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        if (Objects.isNull(nativeType)) {
            return Optional.empty();
        }
        if (nativeType.isInstance(message)) {
            return Optional.of(nativeType.cast(message));
        }
        if (nativeType.isInstance(this)) {
            return Optional.of(nativeType.cast(this));
        }
        return Optional.empty();
    }

    public boolean shouldReconsume() {
        return reconsume;
    }

    private void complete(boolean reconsume) {
        if (!acknowledged.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("Message already acknowledged, msgId=" + message.getMsgId());
        }
        this.reconsume = reconsume;
    }
}
