package io.ddd4j.guice.cqrs;

import io.ddd4j.core.cqrs.readmodel.ViewManager;
import io.ddd4j.core.cqrs.readmodel.ViewScheduler;
import io.ddd4j.kit.lang.StrKit;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Guice 环境默认的 CQRS 读侧视图管理器。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j
public class GuiceViewManager implements ViewManager, ViewScheduler, AutoCloseable {

    /**
     * 运行状态标志
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /**
     * 已调度的视图任务句柄集
     */
    private final ConcurrentMap<String, ScheduledFuture<?>> handles = new ConcurrentHashMap<>();
    /**
     * 调度线程池执行器
     */
    private ScheduledExecutorService executor;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            executor = Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "ddd4j-runtime-guice-view-manager");
                thread.setDaemon(true);
                return thread;
            });
            log.info("GuiceViewManager started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (Objects.nonNull(executor)) {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("ViewManager executor did not terminate in time");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                executor = null;
            }
            handles.clear();
            log.info("GuiceViewManager stopped");
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
        log.info("triggerOnce() should be implemented by concrete view logic");
    }

    @Override
    public ViewScheduleHandle schedule(String viewName, String cron, Runnable task) {
        ensureStarted();
        long period = parseCronToPeriodSeconds(cron);
        java.util.concurrent.ScheduledFuture<?> future =
                executor.scheduleAtFixedRate(task, period, period, TimeUnit.SECONDS);
        handles.put(viewName, future);
        log.info("View scheduled: {} period={}s", viewName, period);
        return new GuiceViewScheduleHandle(future);
    }

    private void ensureStarted() {
        if (!isRunning()) {
            start();
        }
    }

    private long parseCronToPeriodSeconds(String cron) {
        if (StrKit.isEmpty(cron)) {
            return 60L;
        }
        if (cron.startsWith("0/")) {
            try {
                return Long.parseLong(cron.substring(2).split("\\s+")[0]);
            } catch (NumberFormatException exception) {
                return 60L;
            }
        }
        return 60L;
    }

    private static class GuiceViewScheduleHandle implements ViewScheduleHandle {

        private final java.util.concurrent.ScheduledFuture<?> future;

        GuiceViewScheduleHandle(java.util.concurrent.ScheduledFuture<?> future) {
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
