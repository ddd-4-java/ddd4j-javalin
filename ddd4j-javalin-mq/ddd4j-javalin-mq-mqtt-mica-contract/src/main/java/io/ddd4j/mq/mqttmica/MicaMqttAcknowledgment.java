package io.ddd4j.mq.mqttmica;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * mica-mqtt 手动确认映射实现。
 *
 * <p>mica-mqtt 在 QoS &gt; 0 时由内部 PUBACK 链路自动处理；本 ack 端口仅作为"已处理"标记位。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class MicaMqttAcknowledgment implements Acknowledgment {

    /**
     * Header 键：mica-mqtt 消息 ID
     */
    public static final String HEADER_MICA_MESSAGE_ID = "ddd4j.mica.messageId";
    /**
     * Header 键：mica-mqtt 主题
     */
    public static final String HEADER_MICA_TOPIC = "ddd4j.mica.topic";

    /**
     * mica-mqtt 消息 ID
     */
    private final long messageId;
    /**
     * mica-mqtt 主题
     */
    private final String topic;
    /**
     * 关联 ID
     */
    private final String correlationId;
    /**
     * 确认状态标记
     */
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    /**
     * 构造 mica-mqtt 消息确认实例。
     *
     * @param messageId     消息 ID
     * @param topic         主题
     * @param correlationId 关联 ID
     */
    public MicaMqttAcknowledgment(long messageId, String topic, String correlationId) {
        this.messageId = messageId;
        this.topic = topic;
        this.correlationId = correlationId;
    }

    @Override
    public long deliveryTag() {
        return messageId;
    }

    @Override
    public String messageId() {
        return Long.toString(messageId);
    }

    @Override
    public String correlationId() {
        return correlationId;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.MQTT_MICA;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        runOnce();
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        // mica 客户端层无原生 nack；仅标记已处理
        runOnce();
    }

    @Override
    public void reject(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void recover(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (Objects.isNull(type)) {
            return Optional.empty();
        }
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }
        return Optional.empty();
    }

    /**
     * 返回 mica-mqtt 主题。
     */
    public String topic() {
        return topic;
    }

    private void runOnce() {
        if (!acknowledged.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("mica-mqtt message already ack'd, id=" + messageId);
        }
    }
}
