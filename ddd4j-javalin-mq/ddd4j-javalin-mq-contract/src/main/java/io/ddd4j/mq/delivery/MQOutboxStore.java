package io.ddd4j.mq.delivery;

import java.time.Instant;
import java.util.List;

/**
 * Outbox 持久化端口。
 *
 * <p>实现必须原子领取 {@code PENDING} 或已过期 {@code LEASED} 记录，并且只能由持有租约的实例确认发布或重试。
 */
public interface MQOutboxStore {

    /**
     * 在业务事务内追加待发布消息。
     *
     * @param record 待发布消息
     */
    void append(MQOutboxRecord record);

    /**
     * 原子领取当前可投递的消息，并写入发布实例的短期租约。
     *
     * <p>实现必须在领取时将 {@link MQOutboxRecord#attempts()} 加一，并返回更新后的记录；
     * 因而调度器可根据同一数值判断重试或死信。
     *
     * @param leaseOwner 当前发布实例标识
     * @param now 当前时间
     * @param limit 最多领取数量
     * @param policy 投递策略
     * @return 已领取的消息
     */
    List<MQOutboxRecord> claim(String leaseOwner, Instant now, int limit, MQDeliveryPolicy policy);

    /**
     * 仅当租约仍属于当前发布实例时确认消息已发布。
     *
     * @param messageId 消息标识
     * @param leaseOwner 租约持有者
     * @param publishedAt 发布确认时间
     * @return 是否成功确认
     */
    boolean markPublished(String messageId, String leaseOwner, Instant publishedAt);

    /**
     * 仅当租约仍属于当前发布实例时登记发送失败；实现按策略转为 {@code PENDING} 或 {@code DEAD}。
     *
     * @param messageId 消息标识
     * @param leaseOwner 租约持有者
     * @param failedAt 失败时间
     * @param lastError 可诊断失败原因
     * @param policy 投递策略
     * @return 是否成功更新
     */
    boolean reschedule(String messageId, String leaseOwner, Instant failedAt, String lastError,
                       MQDeliveryPolicy policy);

    /**
     * 将死信显式重放为待投递状态。
     *
     * @param messageId 消息标识
     * @param availableAt 重放可投递时间
     * @return 是否成功重放
     */
    boolean replay(String messageId, Instant availableAt);
}
