package io.ddd4j.mq.kafka;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import io.ddd4j.mq.message.MessageHeaders;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka manual acknowledgment mapping.
 *
 * <p>复用 {@link MessageHeaders} 公共常量（{@code ddd4j.message.id} / {@code ddd4j.correlation.id}），
 * 与 core Producer 写入的 header key 对齐。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class KafkaMessageAcknowledgment implements Acknowledgment {

    /**
     * Header 键：Kafka Consumer 实例（用于 unwrap）。
     */
    public static final String HEADER_KAFKA_CONSUMER = "ddd4j.kafka.consumer";
    /**
     * Header 键：Kafka ConsumerRecord 实例（用于 unwrap）。
     */
    public static final String HEADER_KAFKA_RECORD = "ddd4j.kafka.record";

    private final Consumer<?, ?> consumer;
    private final ConsumerRecord<?, ?> record;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    public KafkaMessageAcknowledgment(Consumer<?, ?> consumer, ConsumerRecord<?, ?> record) {
        this.consumer = Objects.requireNonNull(consumer, "Kafka Consumer is required");
        this.record = Objects.requireNonNull(record, "ConsumerRecord is required");
    }

    @Override
    public long deliveryTag() {
        return record.offset();
    }

    @Override
    public String messageId() {
        String header = header(MessageHeaders.HEADER_MESSAGE_ID);
        if (Objects.isNull(header)) {
            header = header(MessageHeaders.LEGACY_HEADER_MESSAGE_ID);
        }
        return Objects.isNull(header) ? record.topic() + "-" + record.partition() + "-" + record.offset() : header;
    }

    @Override
    public String correlationId() {
        return header(MessageHeaders.HEADER_CORRELATION_ID);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.KAFKA;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        if (acknowledged.compareAndSet(false, true)) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            OffsetAndMetadata offset = new OffsetAndMetadata(record.offset() + 1);
            consumer.commitSync(Map.of(partition, offset));
        }
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        if (!requeue) {
            ack(multiple);
        }
    }

    @Override
    public void reject(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void recover(boolean requeue) {
        if (!requeue) {
            throw new UnsupportedOperationException(BrokerType.KAFKA + " recover(false) is not supported");
        }
        consumer.seek(new TopicPartition(record.topic(), record.partition()), record.offset());
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        if (Objects.isNull(nativeType)) {
            return Optional.empty();
        }
        if (nativeType.isInstance(consumer)) {
            return Optional.of(nativeType.cast(consumer));
        }
        if (nativeType.isInstance(record)) {
            return Optional.of(nativeType.cast(record));
        }
        return Optional.empty();
    }

    private String header(String key) {
        Header header = record.headers().lastHeader(key);
        if (Objects.isNull(header) || Objects.isNull(header.value())) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
