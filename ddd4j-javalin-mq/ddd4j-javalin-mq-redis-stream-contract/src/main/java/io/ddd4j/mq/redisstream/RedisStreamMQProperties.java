package io.ddd4j.mq.redisstream;

import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.redisstream.jedis.JedisRedisStreamOperations;
import io.ddd4j.mq.redisstream.lettuce.LettuceRedisStreamOperations;
import io.ddd4j.mq.redisstream.redisson.RedissonRedisStreamOperations;
import lombok.Data;
import lombok.EqualsAndHashCode;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.UnifiedJedis;

import java.util.Objects;

/**
 * Redis Stream adapter configuration.
 *
 * <p>{@link RedisStreamMQProperties} extends {@link MQProperties} —— 复用通用字段（namespace / defaultTopic /
 * autoAck / persist / retries / username / password / database 等），仅声明 Redis Stream 专属字段。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RedisStreamMQProperties extends MQProperties {

    /**
     * Redis 连接 URL（例：{@code redis://localhost:6379} 或 {@code redis://:pwd@host:6379/0}）。
     */
    private String url = "redis://localhost:6379";
    /**
     * 消费者名称
     */
    private String consumerName = "ddd4j";
    /**
     * 单次 XREADGROUP 拉取条数
     */
    private int count = 10;
    /**
     * XREADGROUP 阻塞毫秒数
     */
    private int blockMillis = 1000;
    /**
     * 是否自动创建消费组
     */
    private boolean autoCreateGroup = true;
    /**
     * 是否自动启动消费者
     */
    private boolean autoStartConsumers = true;
    /**
     * Redis 客户端后端类型
     */
    private RedisStreamClientType clientType = RedisStreamClientType.JEDIS;

    /**
     * 基于 {@link #url} 创建 Jedis（UnifiedJedis 形态）。
     */
    public UnifiedJedis newJedis() {
        return RedisClient.create(url);
    }

    /**
     * 基于 {@link #clientType} 创建对应后端的 Stream 操作适配。
     */
    public RedisStreamOperations newOperations() {
        return switch (clientType) {
            case JEDIS -> new JedisRedisStreamOperations(newJedis());
            case REDISSON -> new RedissonRedisStreamOperations(url);
            case LETTUCE -> new LettuceRedisStreamOperations(url);
        };
    }

    /**
     * 设置客户端类型（null 兜底为 JEDIS）。
     */
    public void setClientType(RedisStreamClientType clientType) {
        this.clientType = Objects.isNull(clientType) ? RedisStreamClientType.JEDIS : clientType;
    }
}
