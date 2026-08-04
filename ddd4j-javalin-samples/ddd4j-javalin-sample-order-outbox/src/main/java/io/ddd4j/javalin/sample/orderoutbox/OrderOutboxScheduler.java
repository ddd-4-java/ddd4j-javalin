package io.ddd4j.javalin.sample.orderoutbox;

import io.ddd4j.sample.order.application.OutboxDispatchResult;
import io.ddd4j.sample.order.application.OutboxPublisher;
import io.ddd4j.sample.order.jdbc.TransactionalOutboxPublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 周期性发送事务 Outbox 中仍待发布的订单集成事件。
 *
 * <p>对齐 ddd4j-boot 的 {@code OrderOutboxScheduler}（Spring {@code @Scheduled} 的 javalin 等价物）：
 * javalin 无调度注解，这里用 {@link ScheduledExecutorService} 以固定延迟周期执行
 * {@link #publishPending()}，并记录 published / failed 统计。
 *
 * <p>两个构造函数分别适配内存装配（{@link OutboxPublisher}）与 PostgreSQL 装配
 * （{@link TransactionalOutboxPublisher}，publish 在单个 JDBC 事务内完成）。
 */
@Slf4j
public final class OrderOutboxScheduler {

    private final OutboxDispatch dispatch;
    private final OrderOutboxSampleProperties properties;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> handle;

    public OrderOutboxScheduler(OutboxPublisher publisher, OrderOutboxSampleProperties properties) {
        this(publisher::dispatchPending, properties);
    }

    public OrderOutboxScheduler(TransactionalOutboxPublisher publisher, OrderOutboxSampleProperties properties) {
        this(publisher::publishPending, properties);
    }

    private OrderOutboxScheduler(OutboxDispatch dispatch, OrderOutboxSampleProperties properties) {
        this.dispatch = Objects.requireNonNull(dispatch, "dispatch must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 启动周期调度（幂等；重复调用无副作用）。
     */
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "order-outbox-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        handle = executor.scheduleWithFixedDelay(this::publishPending,
                properties.getOutboxDelayMillis(), properties.getOutboxDelayMillis(), TimeUnit.MILLISECONDS);
        log.info("Order Outbox scheduler started (delay={}ms, batchSize={})",
                properties.getOutboxDelayMillis(), properties.getOutboxBatchSize());
    }

    /**
     * 手动触发一轮发布（也用于集成测试中的确定性断言）。
     *
     * @return 本轮发布结果
     */
    public OutboxDispatchResult publishPending() {
        OutboxDispatchResult result = dispatch.publishPending(properties.getOutboxBatchSize());
        if (result.failed() > 0) {
            log.warn("Order Outbox dispatch retained {} failed messages for retry", result.failed());
        }
        if (result.published() > 0) {
            log.info("Order Outbox published {}/{} messages", result.published(), result.attempted());
        }
        return result;
    }

    /**
     * 停止周期调度（幂等）。
     */
    public synchronized void stop() {
        if (handle != null) {
            handle.cancel(false);
            handle = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @FunctionalInterface
    private interface OutboxDispatch {

        OutboxDispatchResult publishPending(int limit);
    }
}
