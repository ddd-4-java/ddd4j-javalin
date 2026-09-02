package io.ddd4j.mq.mqtt;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Eclipse Paho MQTT 手动确认映射实现。
 *
 * <p>MQTT 没有原生 broker-side ack（仅 QoS 1/2 协议层的 PUBACK/PUBCOMP）；
 * Paho 客户端在 QoS &gt; 0 时收到消息需要 {@link org.eclipse.paho.client.mqttv3.IMqttDeliveryToken#waitForCompletion(long)} 确认。
 * 这里把 {@code ack()} 映射为标记已处理，{@code nack(requeue=true)} 映射为发布一条重发消息
 * （业务侧需自行实现 DLQ 策略）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class MqttAcknowledgment implements Acknowledgment {

    /**
     * Header 键：MQTT 消息体
     */
    public static final String HEADER_MQTT_MESSAGE = "ddd4j.mqtt.message";
    /**
     * Header 键：MQTT 主题
     */
    public static final String HEADER_MQTT_TOPIC = "ddd4j.mqtt.topic";

    /**
     * Paho MQTT 消息实例
     */
    private final MqttMessage message;
    /**
     * MQTT 主题
     */
    private final String topic;
    /**
     * 消息 ID
     */
    private final int messageId;
    /**
     * 确认状态标记
     */
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);

    public MqttAcknowledgment(MqttMessage message, String topic) {
        this.message = message;
        this.topic = topic;
        this.messageId = message.getId();
    }

    @Override
    public long deliveryTag() {
        return messageId;
    }

    @Override
    public String messageId() {
        return Integer.toString(messageId);
    }

    @Override
    public String correlationId() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return Objects.nonNull(message);
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.MQTT;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        runOnce(() -> { /* MQTT 协议层已自动 PUBACK */ });
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        // MQTT 没有原生 nack；仅标记本地为未处理（业务侧可订阅 DLQ topic 重新发布）
        runOnce(() -> {
            throw new UnsupportedOperationException("MQTT has no native nack; use a DLQ topic");
        });
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
        if (type.isInstance(message)) {
            return Optional.of(type.cast(message));
        }
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }
        return Optional.empty();
    }

    /**
     * 返回 Paho MQTT 主题。
     */
    public String topic() {
        return topic;
    }

    private void runOnce(IoOperation op) {
        if (!acknowledged.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("MQTT message already acknowledged, id=" + messageId);
        }
        op.run();
    }

    @FunctionalInterface
    private interface IoOperation {
        void run();
    }
}
