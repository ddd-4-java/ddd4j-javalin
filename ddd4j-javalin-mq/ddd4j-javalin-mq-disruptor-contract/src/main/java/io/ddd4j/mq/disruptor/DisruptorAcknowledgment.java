package io.ddd4j.mq.disruptor;

import com.lmax.disruptor.RingBuffer;
import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Disruptor 本地消息确认实现：ack 为消费完成；requeue 重新发布到 RingBuffer。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class DisruptorAcknowledgment implements Acknowledgment {

    private final DisruptorEvent event;
    private final RingBuffer<DisruptorEvent> ringBuffer;
    private final long deliveryTag;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    /**
     * @param event       当前 ddd4j Disruptor 事件
     * @param ringBuffer  RingBuffer（requeue 用）
     * @param deliveryTag 投递序号
     */
    public DisruptorAcknowledgment(
            DisruptorEvent event,
            RingBuffer<DisruptorEvent> ringBuffer,
            long deliveryTag) {
        this.event = event;
        this.ringBuffer = ringBuffer;
        this.deliveryTag = deliveryTag;
    }

    @Override
    public long deliveryTag() {
        return deliveryTag;
    }

    @Override
    public String messageId() {
        return event.getMessageId();
    }

    @Override
    public String correlationId() {
        return Objects.nonNull(event.getMessageId()) ? event.getMessageId() : null;
    }

    @Override
    public boolean isOpen() {
        return Objects.nonNull(ringBuffer);
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.DISRUPTOR;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        acknowledged.set(true);
    }

    @Override
    public void nack(boolean requeue) {
        if (requeue && Objects.nonNull(ringBuffer) && !acknowledged.get()) {
            republish();
        }
        acknowledged.set(true);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        nack(requeue);
    }

    @Override
    public void reject(boolean requeue) {
        nack(requeue);
    }

    @Override
    public void recover(boolean requeue) {
        if (!requeue) {
            throw new UnsupportedOperationException("Disruptor does not support recover(false)");
        }
        nack(true);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        if (nativeType.isInstance(event)) {
            return Optional.of(nativeType.cast(event));
        }
        return Optional.empty();
    }

    /**
     * 将当前事件重新发布到 RingBuffer（本地 requeue）。
     */
    private void republish() {
        DisruptorEvent src = this.event;
        ringBuffer.publishEvent((slot, sequence) -> {
            slot.setTopic(src.getTopic());
            slot.setTag(src.getTag());
            slot.setNamespace(src.getNamespace());
            slot.setMessageId(src.getMessageId());
            slot.setPayload(src.getPayload());
            slot.setSequence(sequence);
        });
    }
}
