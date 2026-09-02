package io.ddd4j.mq.delivery;

/**
 * 单次 Outbox 调度的汇总结果。
 *
 * @param claimed 已领取数量
 * @param published 已确认发布数量
 * @param rescheduled 已安排重试数量
 * @param dead 预计进入死信数量
 * @param confirmationLost broker 已接收但租约确认丢失数量，后续可能重复投递
 */
public record MQOutboxDispatchResult(int claimed, int published, int rescheduled, int dead,
                                     int confirmationLost) {
}
