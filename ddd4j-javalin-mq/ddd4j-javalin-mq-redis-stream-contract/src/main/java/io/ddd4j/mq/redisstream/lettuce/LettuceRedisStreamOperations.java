package io.ddd4j.mq.redisstream.lettuce;

import io.ddd4j.mq.redisstream.RedisStreamOperations;
import io.ddd4j.mq.redisstream.RedisStreamRecord;
import io.lettuce.core.*;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.*;

/**
 * 基于 Lettuce 客户端的 Redis Stream 操作实现。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class LettuceRedisStreamOperations implements RedisStreamOperations {

    private final RedisCommands<String, String> commands;
    private final AutoCloseable closeTarget;
    private final Object nativeClient;

    public LettuceRedisStreamOperations(String redisUrl) {
        this(createClient(redisUrl));
    }

    public LettuceRedisStreamOperations(RedisCommands<String, String> commands) {
        this(commands, null, commands);
    }

    public LettuceRedisStreamOperations(StatefulRedisConnection<String, String> connection) {
        this(connection.sync(), connection, connection);
    }

    private LettuceRedisStreamOperations(RedisClient client) {
        StatefulRedisConnection<String, String> connection = client.connect();
        this.commands = connection.sync();
        this.closeTarget = () -> {
            connection.close();
            client.shutdown();
        };
        this.nativeClient = client;
    }

    private LettuceRedisStreamOperations(
            RedisCommands<String, String> commands,
            AutoCloseable closeTarget,
            Object nativeClient) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.closeTarget = closeTarget;
        this.nativeClient = Objects.isNull(nativeClient) ? commands : nativeClient;
    }

    private static RedisClient createClient(String redisUrl) {
        return RedisClient.create(redisUrl);
    }

    @Override
    public String add(String stream, Map<String, String> fields) {
        return commands.xadd(stream, new XAddArgs().id("*"), fields);
    }

    @Override
    public void createGroup(String stream, String group) {
        try {
            commands.xgroupCreate(
                    StreamOffset.from(stream, "$"),
                    group,
                    new XGroupCreateArgs().mkstream(true));
        } catch (RedisCommandExecutionException ex) {
            if (Objects.isNull(ex.getMessage()) || !ex.getMessage().contains("BUSYGROUP")) {
                throw ex;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<RedisStreamRecord>> readGroup(
            String group,
            String consumer,
            int count,
            int blockMillis,
            Set<String> streams) {
        XReadArgs args = new XReadArgs().count(count).block(blockMillis);
        StreamOffset<String>[] offsets = streams.stream()
                .map(StreamOffset::lastConsumed)
                .toArray(StreamOffset[]::new);
        List<StreamMessage<String, String>> records = commands.xreadgroup(Consumer.from(group, consumer), args, offsets);
        Map<String, List<RedisStreamRecord>> result = new HashMap<>();
        if (Objects.isNull(records) || records.isEmpty()) {
            return result;
        }
        for (StreamMessage<String, String> record : records) {
            result.computeIfAbsent(record.getStream(), key -> new ArrayList<>())
                    .add(new RedisStreamRecord(record.getStream(), record.getId(), record.getBody(), record));
        }
        return result;
    }

    @Override
    public void ack(String stream, String group, String entryId) {
        commands.xack(stream, group, entryId);
    }

    @Override
    public Object nativeClient() {
        return nativeClient;
    }

    @Override
    public void close() {
        if (Objects.isNull(closeTarget)) {
            return;
        }
        try {
            closeTarget.close();
        } catch (Exception ex) {
            throw new IllegalStateException("Close Lettuce Redis client failed", ex);
        }
    }
}
