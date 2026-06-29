package io.ddd4j.javalin.core.cqrs;

import io.ddd4j.core.cqrs.projection.ViewManager;
import io.ddd4j.core.cqrs.projection.ViewScheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Javalin CQRS 读侧视图管理器（基于 {@link ScheduledExecutorService}）。
 *
 * <p>实现 ddd4j-core 的 {@link ViewManager} SPI。Javalin 是轻量级框架，
 * 不内置 CRON 调度器，故使用 JDK 自带的 {@link ScheduledExecutorService}。
 *
 * <p>由 {@code ddd4j-javalin-guice} 注册为 Guice 单例 Bean，业务方无需关心。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-JAVALIN : ViewManager ###")
public class JavalinViewManager implements ViewManager, ViewScheduler, AutoCloseable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentMap<String, java.util.concurrent.ScheduledFuture<?>> handles = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            executor = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "ddd4j-javalin-view-manager");
                t.setDaemon(true);
                return t;
            });
            log.info("JavalinViewManager started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (executor != null) {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("ViewManager executor did not terminate in time");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                executor = null;
            }
            handles.clear();
            log.info("JavalinViewManager stopped");
        }
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void triggerOnce() {
        log.info("triggerOnce() - 业务方应在 View 子类 override");
    }

    @Override
    public ViewScheduleHandle schedule(String viewName, String cron, Runnable task) {
        // Javalin 场景简化：用 scheduleAtFixedRate 代替 cron 表达式
        // 业务方应在上层解析 CRON 后传入 periodSeconds
        long period = parseCronToPeriodSeconds(cron);
        java.util.concurrent.ScheduledFuture<?> future =
                executor.scheduleAtFixedRate(task, period, period, TimeUnit.SECONDS);
        handles.put(viewName, future);
        log.info("View scheduled: {} period={}s", viewName, period);
        return new JavalinViewScheduleHandle(future);
    }

    /**
     * 极简 CRON 解析：仅支持 {@code 0/5 * * * * ?} 这类 "秒级" 表达式。
     * 复杂 CRON 建议业务方预解析后传入固定 period（秒）。
     */
    private long parseCronToPeriodSeconds(String cron) {
        if (cron == null || cron.isEmpty()) {
            return 60L;
        }
        if (cron.startsWith("0/")) {
            try {
                return Long.parseLong(cron.substring(2).split("\\s+")[0]);
            } catch (NumberFormatException ignore) {
                return 60L;
            }
        }
        return 60L;
    }

    private static class JavalinViewScheduleHandle implements ViewScheduleHandle {
        private final java.util.concurrent.ScheduledFuture<?> future;

        JavalinViewScheduleHandle(java.util.concurrent.ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            future.cancel(false);
        }

        @Override
        public boolean isActive() {
            return !future.isCancelled() && !future.isDone();
        }
    }
}
