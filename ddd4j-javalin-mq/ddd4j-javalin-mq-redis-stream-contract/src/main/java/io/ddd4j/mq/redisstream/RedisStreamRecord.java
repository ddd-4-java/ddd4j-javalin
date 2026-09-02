package io.ddd4j.mq.redisstream;

import java.util.Map;

/**
 * 跨 Jedis、Redisson 和 Lettuce 的统一 Redis Stream 记录模型。
 *
 * @param stream        所属 Stream 名称
 * @param id            消息条目 ID
 * @param fields        消息字段
 * @param nativeMessage 底层原生消息对象
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record RedisStreamRecord(String stream, String id, Map<String, String> fields, Object nativeMessage) {
}
