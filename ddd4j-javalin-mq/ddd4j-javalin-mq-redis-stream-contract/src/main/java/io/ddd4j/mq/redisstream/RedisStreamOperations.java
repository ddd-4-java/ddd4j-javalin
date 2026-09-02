package io.ddd4j.mq.redisstream;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis Stream 命令端口抽象，MQ 适配器使用的最小化接口。
 *
 * <p>屏蔽 Jedis、Lettuce、Redisson 三种客户端的差异，提供统一的 Redis Stream 操作。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface RedisStreamOperations extends AutoCloseable {

    /**
     * 向 Redis Stream 添加消息。
     *
     * @param stream Stream 名称
     * @param fields 消息字段
     * @return 消息 ID
     */
    String add(String stream, Map<String, String> fields);

    /**
     * 创建消费者组（如果已存在则忽略）。
     *
     * @param stream Stream 名称
     * @param group  消费者组名
     */
    void createGroup(String stream, String group);

    /**
     * 从消费者组读取消息。
     *
     * @param group       消费者组名
     * @param consumer    消费者名称
     * @param count       每次读取的最大条数
     * @param blockMillis 阻塞等待毫秒数
     * @param streams     要读取的 Stream 集合
     * @return Stream 到消息记录列表的映射
     */
    Map<String, List<RedisStreamRecord>> readGroup(
            String group,
            String consumer,
            int count,
            int blockMillis,
            Set<String> streams);

    /**
     * 确认消息已被消费。
     *
     * @param stream  Stream 名称
     * @param group   消费者组名
     * @param entryId 消息条目 ID
     */
    void ack(String stream, String group, String entryId);

    /**
     * 返回底层 Redis 客户端原生对象。
     *
     * @return 原生客户端实例
     */
    Object nativeClient();

    @Override
    default void close() {
    }
}
