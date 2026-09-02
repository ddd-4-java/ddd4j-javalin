package io.ddd4j.mq.delivery;

import java.time.Instant;

/**
 * 消费端 Inbox 去重端口。
 *
 * <p>实现必须以 {@code consumerId + messageId} 作为唯一键，并与消费者业务写操作处于同一事务边界：
 * 成功写入代表本次消息可安全 ACK，业务失败时事务必须回滚该写入以便重试。
 */
public interface MQInboxStore {

    /**
     * 记录一条尚未处理的消息。
     *
     * @param consumerId 稳定消费者标识
     * @param messageId 生产端传入的稳定消息标识
     * @param processedAt 处理开始时间
     * @return {@code true} 表示首次处理；{@code false} 表示重复消息，应直接 ACK
     */
    boolean recordIfAbsent(String consumerId, String messageId, Instant processedAt);
}
