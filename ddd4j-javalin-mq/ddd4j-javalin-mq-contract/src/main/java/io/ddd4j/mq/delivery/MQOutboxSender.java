package io.ddd4j.mq.delivery;

/**
 * 将已领取 Outbox 记录发送到底层 broker 的端口。
 */
@FunctionalInterface
public interface MQOutboxSender {

    /**
     * 发送一条已领取消息。
     *
     * @param record 已携带稳定消息头的 Outbox 记录
     */
    void send(MQOutboxRecord record);
}
