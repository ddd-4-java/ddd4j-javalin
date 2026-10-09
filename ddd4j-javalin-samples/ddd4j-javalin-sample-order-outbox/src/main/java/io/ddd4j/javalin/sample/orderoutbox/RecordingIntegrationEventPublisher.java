package io.ddd4j.javalin.sample.orderoutbox;

import io.ddd4j.sample.order.application.IntegrationEventPublisher;
import io.ddd4j.sample.order.application.OutboxMessage;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存版集成事件发布器：记录每条已确认发布的 Outbox 消息，供示例与集成测试断言。
 *
 * <p>javalin sample 不引入 Kafka/Redis 依赖；生产运行时请替换为
 * {@code KafkaIntegrationEventPublisher}（见 ddd4j-boot-sample-order 的 Postgres 装配）。
 */
public final class RecordingIntegrationEventPublisher implements IntegrationEventPublisher {

    private final List<OutboxMessage> published = new CopyOnWriteArrayList<>();

    /**
     * 默认构造方法：以空记录列表初始化。
     */
    public RecordingIntegrationEventPublisher() {
    }

    /**
     * 记录一条已发布的集成事件。
     *
     * @param message Outbox 消息，不可为 {@code null}
     * @throws NullPointerException 当 {@code message} 为 {@code null}
     */
    @Override
    public void publish(OutboxMessage message) {
        published.add(Objects.requireNonNull(message, "message must not be null"));
    }

    /**
     * 返回已发布消息的不可变快照。
     *
     * @return 已发布消息的不可变快照
     */
    public List<OutboxMessage> published() {
        return List.copyOf(published);
    }

    /** 清空已发布记录（测试隔离用）。 */
    public void clear() {
        published.clear();
    }
}
