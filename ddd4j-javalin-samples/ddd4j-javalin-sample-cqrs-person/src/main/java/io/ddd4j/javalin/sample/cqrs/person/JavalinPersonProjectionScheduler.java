package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.Inject;
import io.ddd4j.core.cqrs.readmodel.ProjectionRunner;
import io.ddd4j.core.cqrs.readmodel.ViewScheduler;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.query.PersonListView;

import java.util.Objects;

/**
 * 人员读侧投影调度器，周期性刷新读模型。
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 7.1.x
 */
public class JavalinPersonProjectionScheduler {

    private final PersonListView view;

    private final ProjectionRunner<PersonEvent> runner;

    private final ViewScheduler scheduler;

    private ViewScheduler.ViewScheduleHandle handle;

    /**
     * 构造 JavalinPersonProjectionScheduler 实例并初始化依赖。
     * @param view 人员读模型视图。
     * @param runner 投影执行器。
     * @param scheduler 视图调度器。
     */
    @Inject
    public JavalinPersonProjectionScheduler(
            PersonListView view,
            ProjectionRunner<PersonEvent> runner,
            ViewScheduler scheduler
    ) {
        this.view = view;
        this.runner = runner;
        this.scheduler = scheduler;
    }

    /**
     * 启动读侧投影调度。
     */
    public void start() {
        handle = scheduler.schedule(view.getName(), view.getCron(), () -> runner.runOnce(view));
    }

    /**
     * 停止读侧投影调度。
     */
    public void stop() {
        if (Objects.nonNull(handle)) {
            handle.cancel();
        }
    }
}
