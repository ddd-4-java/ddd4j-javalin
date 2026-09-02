package io.ddd4j.mq.redisstream.redisson;

import io.ddd4j.mq.redisstream.RedisStreamOperations;
import io.ddd4j.mq.redisstream.RedisStreamRecord;
import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.RedisException;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.*;

/**
 * 基于 Redisson 客户端的 Redis Stream 操作实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RedissonRedisStreamOperations implements RedisStreamOperations {

    private final RedissonClient redisson;
    private final boolean closeClient;

    public RedissonRedisStreamOperations(String redisUrl) {
        this(createClient(redisUrl), true);
    }

    public RedissonRedisStreamOperations(RedissonClient redisson) {
        this(redisson, false);
    }

    public RedissonRedisStreamOperations(RedissonClient redisson, boolean closeClient) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.closeClient = closeClient;
    }

    private static RedissonClient createClient(String redisUrl) {
        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);
        return Redisson.create(config);
    }

    private static StreamMessageId messageId(String entryId) {
        String[] parts = entryId.split("-", 2);
        long id0 = Long.parseLong(parts[0]);
        long id1 = parts.length > 1 ? Long.parseLong(parts[1]) : 0L;
        return new StreamMessageId(id0, id1);
    }

    @Override
    public String add(String stream, Map<String, String> fields) {
        return stream(stream).add(StreamAddArgs.entries(fields)).toString();
    }

    @Override
    public void createGroup(String stream, String group) {
        try {
            stream(stream).createGroup(StreamCreateGroupArgs.name(group)
                    .id(StreamMessageId.NEWEST)
                    .makeStream());
        } catch (RedisException ex) {
            if (Objects.isNull(ex.getMessage()) || !ex.getMessage().contains("BUSYGROUP")) {
                throw ex;
            }
        }
    }

    @Override
    public Map<String, List<RedisStreamRecord>> readGroup(
            String group,
            String consumer,
            int count,
            int blockMillis,
            Set<String> streams) {
        Map<String, List<RedisStreamRecord>> result = new HashMap<>();
        for (String stream : streams) {
            Map<StreamMessageId, Map<String, String>> records = stream(stream).readGroup(
                    group,
                    consumer,
                    StreamReadGroupArgs.neverDelivered()
                            .count(count)
                            .timeout(Duration.ofMillis(blockMillis)));
            if (Objects.isNull(records) || records.isEmpty()) {
                continue;
            }
            List<RedisStreamRecord> normalized = new ArrayList<>();
            records.forEach((id, fields) -> normalized.add(new RedisStreamRecord(stream, id.toString(), fields, id)));
            result.put(stream, normalized);
        }
        return result;
    }

    @Override
    public void ack(String stream, String group, String entryId) {
        stream(stream).ack(group, messageId(entryId));
    }

    @Override
    public Object nativeClient() {
        return redisson;
    }

    @Override
    public void close() {
        if (closeClient) {
            redisson.shutdown();
        }
    }

    private RStream<String, String> stream(String stream) {
        return redisson.getStream(stream);
    }
}
