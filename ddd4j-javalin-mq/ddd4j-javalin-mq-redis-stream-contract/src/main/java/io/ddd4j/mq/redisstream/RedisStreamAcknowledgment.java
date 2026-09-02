package io.ddd4j.mq.redisstream;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import io.ddd4j.mq.redisstream.jedis.JedisRedisStreamOperations;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.UnifiedJedis;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream manual acknowledgment mapping.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RedisStreamAcknowledgment implements Acknowledgment {

    public static final String HEADER_REDIS_STREAM = "ddd4j.redis.stream";
    public static final String HEADER_REDIS_GROUP = "ddd4j.redis.group";
    public static final String HEADER_REDIS_ENTRY_ID = "ddd4j.redis.entryId";

    private final RedisStreamOperations operations;
    private final String stream;
    private final String group;
    private final String entryId;
    private final Object nativeEntryId;
    private final String messageId;
    private final String correlationId;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    public RedisStreamAcknowledgment(
            UnifiedJedis jedis,
            String stream,
            String group,
            StreamEntryID entryId,
            String messageId,
            String correlationId) {
        this(new JedisRedisStreamOperations(jedis), stream, group, entryId.toString(), entryId, messageId, correlationId);
    }

    public RedisStreamAcknowledgment(
            RedisStreamOperations operations,
            String stream,
            String group,
            String entryId,
            Object nativeEntryId,
            String messageId,
            String correlationId) {
        this.operations = Objects.requireNonNull(operations, "operations");
        this.stream = Objects.requireNonNull(stream, "stream");
        this.group = Objects.requireNonNull(group, "group");
        this.entryId = Objects.requireNonNull(entryId, "entryId");
        this.nativeEntryId = nativeEntryId;
        this.messageId = Objects.isNull(messageId) ? entryId : messageId;
        this.correlationId = correlationId;
    }

    @Override
    public long deliveryTag() {
        return RedisStreamIds.deliveryTag(entryId);
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
        return true;
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.REDIS_STREAM;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        if (acknowledged.compareAndSet(false, true)) {
            operations.ack(stream, group, entryId);
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
            ack(false);
        }
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> nativeType) {
        if (Objects.isNull(nativeType)) {
            return Optional.empty();
        }
        if (nativeType.isInstance(entryId)) {
            return Optional.of(nativeType.cast(entryId));
        }
        if (Objects.nonNull(nativeEntryId) && nativeType.isInstance(nativeEntryId)) {
            return Optional.of(nativeType.cast(nativeEntryId));
        }
        Object nativeClient = operations.nativeClient();
        if (Objects.nonNull(nativeClient) && nativeType.isInstance(nativeClient)) {
            return Optional.of(nativeType.cast(nativeClient));
        }
        return Optional.empty();
    }
}
