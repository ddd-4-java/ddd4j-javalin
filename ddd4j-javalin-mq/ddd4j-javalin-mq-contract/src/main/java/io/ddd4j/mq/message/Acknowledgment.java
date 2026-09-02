package io.ddd4j.mq.message;

import io.ddd4j.mq.BrokerType;

import java.util.Optional;

/**
 * 消息确认端口。以 AMQP {@code Channel} 语义为基准；其他 Broker 由 Adapter 映射。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface Acknowledgment {

    /**
     * 返回当前消息的投递标签。
     */
    long deliveryTag();

    /**
     * 返回消息 ID（若 Broker 提供）。
     */
    String messageId();

    /**
     * 返回关联 ID（若 Broker 提供）。
     */
    String correlationId();

    /**
     * 底层连接/通道是否仍可用。
     */
    boolean isOpen();

    /**
     * 当前消息是否已被确认。
     */
    boolean isAcknowledged();

    /**
     * 当前确认实现对应的 Broker 类型。
     */
    BrokerType brokerType();

    /**
     * 确认单条消息成功消费（等价于 {@code basicAck(tag, false)}）。
     */
    void ack();

    /**
     * 确认消息，支持批量确认语义。
     *
     * @param multiple 是否批量确认至当前 tag
     */
    void ack(boolean multiple);

    /**
     * 否定确认，可控制是否重新入队。
     *
     * @param requeue 是否重新入队
     */
    void nack(boolean requeue);

    /**
     * 否定确认，支持批量与重入队控制。
     *
     * @param multiple 是否批量否定
     * @param requeue  是否重新入队
     */
    void nack(boolean multiple, boolean requeue);

    /**
     * 拒绝单条消息。
     *
     * @param requeue 是否重新入队
     */
    void reject(boolean requeue);

    /**
     * 恢复消息投递（Rabbit 专属语义，其他 Broker 可抛 {@link UnsupportedAckOperationException}）。
     *
     * @param requeue 是否重新入队
     */
    void recover(boolean requeue);

    /**
     * 确认单条消息的便捷方法。
     */
    default void ackSingle() {
        ack(false);
    }

    /**
     * 丢弃消息（不重新入队，通常进入 DLQ）。
     */
    default void discard() {
        nack(false);
    }

    /**
     * 将消息重新入队等待再次消费。
     */
    default void requeue() {
        nack(true);
    }

    /**
     * 通过 recover 语义重新入队。
     */
    default void requeueViaRecover() {
        recover(true);
    }

    /**
     * 获取底层 Broker 原生对象（如 {@code Channel}）。
     *
     * @param nativeType 原生类型
     * @param <T>        类型参数
     * @return 匹配的原生对象
     */
    <T> Optional<T> unwrap(Class<T> nativeType);
}
