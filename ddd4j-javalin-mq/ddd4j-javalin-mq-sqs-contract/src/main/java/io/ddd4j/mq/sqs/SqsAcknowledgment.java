package io.ddd4j.mq.sqs;

import io.ddd4j.mq.BrokerType;
import io.ddd4j.mq.message.Acknowledgment;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AWS SQS 手动确认映射实现。
 *
 * <p>SQS 没有原生 ack 语义：ack 等价于 {@code deleteMessage(receiptHandle)}，nack(requeue=true)
 * 等价于 {@code changeMessageVisibility(receiptHandle, 0)}（让消息立即可被另一消费者接收）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class SqsAcknowledgment implements Acknowledgment {

    /**
     * Header 键：SQS 客户端
     */
    public static final String HEADER_SQS_CLIENT = "ddd4j.sqs.client";
    /**
     * Header 键：SQS 消息体
     */
    public static final String HEADER_SQS_MESSAGE = "ddd4j.sqs.message";
    /**
     * Header 键：SQS 队列 URL
     */
    public static final String HEADER_SQS_QUEUE_URL = "ddd4j.sqs.queueUrl";

    /**
     * AWS SQS 客户端实例
     */
    private final SqsClient client;
    /**
     * SQS 消息实例
     */
    private final Message message;
    /**
     * 队列 URL
     */
    private final String queueUrl;
    /**
     * 消息 ID
     */
    private final String messageId;
    /**
     * 消息回执句柄
     */
    private final String receiptHandle;
    /**
     * 确认状态标记
     */
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);
    /**
     * nack 时是否允许重新入队
     */
    private final boolean requeueOnNack;

    /**
     * 构造 SQS 消息确认实例。
     *
     * @param client        AWS SQS 客户端
     * @param message       SQS 消息
     * @param queueUrl      队列 URL
     * @param requeueOnNack nack 时是否允许重新入队
     */
    public SqsAcknowledgment(SqsClient client, Message message, String queueUrl, boolean requeueOnNack) {
        this.client = client;
        this.message = message;
        this.queueUrl = queueUrl;
        this.messageId = message.messageId();
        this.receiptHandle = message.receiptHandle();
        this.requeueOnNack = requeueOnNack;
    }

    @Override
    public long deliveryTag() {
        return Objects.isNull(receiptHandle) ? 0L : (long) receiptHandle.hashCode();
    }

    @Override
    public String messageId() {
        return messageId;
    }

    @Override
    public String correlationId() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return Objects.nonNull(client);
    }

    @Override
    public boolean isAcknowledged() {
        return acknowledged.get();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.SQS;
    }

    @Override
    public void ack() {
        ack(false);
    }

    @Override
    public void ack(boolean multiple) {
        runOnce(() -> client.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl).receiptHandle(receiptHandle).build()));
    }

    @Override
    public void nack(boolean requeue) {
        nack(false, requeue);
    }

    @Override
    public void nack(boolean multiple, boolean requeue) {
        if (requeue && requeueOnNack) {
            runOnce(() -> client.changeMessageVisibility(b -> b
                    .queueUrl(queueUrl).receiptHandle(receiptHandle).visibilityTimeout(0)));
        } else {
            // 不重投：仍走 deleteMessage
            runOnce(() -> client.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl).receiptHandle(receiptHandle).build()));
        }
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
        if (type.isInstance(client)) {
            return Optional.of(type.cast(client));
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
            throw new UnsupportedOperationException("SQS message already ack'd, id=" + messageId);
        }
        try {
            op.run();
        } catch (Exception ex) {
            acknowledged.set(false);
            throw new IllegalStateException("SQS ack operation failed, id=" + messageId, ex);
        }
    }

    @FunctionalInterface
    private interface IoOperation {
        void run();
    }
}
