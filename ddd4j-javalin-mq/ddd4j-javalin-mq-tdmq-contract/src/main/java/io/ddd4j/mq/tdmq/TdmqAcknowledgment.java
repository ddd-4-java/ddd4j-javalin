package io.ddd4j.mq.tdmq;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 腾讯云 TDMQ 消息确认实现。
 *
 * <p>TDMQ 是腾讯云基于 Pulsar 的托管服务。{@link #ackCallback} 暴露由业务侧 TDMQ 客户端传入的 ack 回调，
 * 由 ddd4j-MQ 的 consume 流程根据执行结果决定 ack/nack/requeue。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public final class TdmqAcknowledgment implements Acknowledgment {

    private final String messageId;
    private final String correlationId;
    private final long deliveryTag;
    private final java.util.function.Consumer<Boolean> ackCallback;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    public TdmqAcknowledgment(String messageId,
                              String correlationId,
                              long deliveryTag,
                              java.util.function.Consumer<Boolean> ackCallback) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.deliveryTag = deliveryTag;
        this.ackCallback = Objects.requireNonNull(ackCallback, "ackCallback");
    }

    @Override
    public long deliveryTag() {
        return deliveryTag;
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
        return !acknowledged.get();
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.TDMQ;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        ensureNotAcknowledged();
        ackCallback.accept(true);
        acknowledged.set(true);
        log.debug("TDMQ ack messageId={}, multiple={}", messageId, multiple);
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        ensureNotAcknowledged();
        ackCallback.accept(requeue);
        acknowledged.set(true);
        log.debug("TDMQ nack messageId={}, requeue={}, multiple={}", messageId, requeue, multiple);
    }

    @Override
    public void reject(boolean requeue) {
        nack(requeue);
    }

    @Override
    public void recover(boolean requeue) {
        nack(requeue);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        Objects.requireNonNull(nativeType, "nativeType");
        if (nativeType.isInstance(this)) {
            return Optional.of(nativeType.cast(this));
        }
        return Optional.empty();
    }

    private void ensureNotAcknowledged() {
        if (acknowledged.get()) {
            throw new IllegalStateException("TDMQ message already acknowledged, messageId=" + messageId);
        }
    }
}
