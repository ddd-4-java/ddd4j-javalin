package io.ddd4j.mq.redisstream;

/**
 * Redis Stream 适配器使用的 Redis 客户端后端类型枚举。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum RedisStreamClientType {

    JEDIS,
    REDISSON,
    LETTUCE
}
