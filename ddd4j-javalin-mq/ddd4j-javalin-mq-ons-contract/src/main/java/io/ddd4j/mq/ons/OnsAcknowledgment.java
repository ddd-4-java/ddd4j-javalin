package io.ddd4j.mq.ons;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 阿里云 ONS 手动确认映射实现。
 *
 * <p>ONS 消费通过监听器返回的 {@link Action} 来确认消息。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class OnsAcknowledgment implements Acknowledgment {

    /**
     * Header 键：ONS 消息体
     */
    public static final String HEADER_ONS_MESSAGE = "ddd4j.ons.message";
    /**
     * Header 键：ONS 消费上下文
     */
    public static final String HEADER_ONS_CONTEXT = "ddd4j.ons.context";

    /**
     * ONS 消费上下文
     */
    private final ConsumeContext context;
    /**
     * ONS 消息实例
     */
    private final Message message;
    /**
     * 消息 ID
     */
    private final String messageId;
    /**
     * 消息 Key
     */
    private final String key;
    /**
     * 消息偏移量
     */
    private final long offset;
    /**
     * 确认状态标记
     */
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);
    /**
     * 确认动作（提交/稍后重试）
     */
    private volatile Action action = Action.CommitMessage;

    /**
     * 构造 ONS 消息确认实例。
     *
     * @param context ONS 消费上下文
     * @param message ONS 消息
     */
    public OnsAcknowledgment(ConsumeContext context, Message message) {
        this.context = context;
        this.message = message;
        this.messageId = message.getMsgID();
        this.key = message.getKey();
        this.offset = message.getOffset();
    }

    @Override
    public long deliveryTag() {
        return offset;
    }

    @Override
    public String messageId() {
        return messageId;
    }

    @Override
    public String correlationId() {
        return key;
    }

    @Override
    public boolean isOpen() {
        return Objects.nonNull(context);
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.ONS;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        runOnce(() -> action = Action.CommitMessage);
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        runOnce(() -> {
            if (requeue) {
                action = Action.ReconsumeLater;
            } else {
                action = Action.CommitMessage; // no native reject-without-commit path
            }
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
        if (type.isInstance(context)) {
            return Optional.of(type.cast(context));
        }
        if (type.isInstance(message)) {
            return Optional.of(type.cast(message));
        }
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }
        return Optional.empty();
    }

    private void runOnce(IoOperation op) {
        if (!acknowledged.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("ONS message already ack'd, id=" + messageId);
        }
        op.run();
    }

    public Action action() {
        return action;
    }

    @FunctionalInterface
    private interface IoOperation {
        void run();
    }
}
