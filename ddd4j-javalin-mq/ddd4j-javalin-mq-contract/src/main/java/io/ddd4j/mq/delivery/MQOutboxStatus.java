package io.ddd4j.mq.delivery;

/**
 * Outbox 消息的可持久化生命周期状态。
 */
public enum MQOutboxStatus {

    /** 等待领取或下一次重试。 */
    PENDING,
    /** 已被某个发布实例短期租约领取。 */
    LEASED,
    /** 已由 broker 确认接收。 */
    PUBLISHED,
    /** 重试次数耗尽，等待人工诊断或重放。 */
    DEAD
}
