package io.javalin.sample.tasks;

import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.task.Task;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.snapshots.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
abstract public class AbstractTask implements Task {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final Counter taskRunTimes = Counter.builder()
        .name("task_run_times_total")
        .help("task run times for success/failed")
        .labelNames("name", "result")
        .register();

    private static final Histogram taskRunDuration = Histogram.builder()
        .name("task_run_duration")
        .help("task run duration")
        .labelNames("name")
        .unit(Unit.SECONDS)
        .register();

    @Override
    public void execute() {
        var className = this.getClass().getSimpleName();

        if (running.get()) {
            log.info("Task is running, ignore: {}", className);
            return;
        }

        var startTs = DateUtil.current();
        try {
            log.info("Task start: {}", className);
            running.set(true);
            run();
            taskRunTimes.labelValues(className, "success").inc();
        }
        catch (Throwable e) {
            log.warn("Task [{}] error: {}",  className, e.getMessage(), e);
            taskRunTimes.labelValues(className, "failed").inc();
        }
        finally {
            log.info("Task done: {}", className);
            running.set(false);
            taskRunDuration.labelValues(className).observe(Unit.millisToSeconds(DateUtil.current() - startTs));
        }
    }

    public abstract void run();
}
