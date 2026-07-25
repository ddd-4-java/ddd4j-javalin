package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.Inject;
import io.ddd4j.core.cqrs.readmodel.ProjectionRunner;
import io.ddd4j.core.cqrs.readmodel.ViewScheduler;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.query.PersonListView;

import java.util.Objects;

public class JavalinPersonProjectionScheduler {

    private final PersonListView view;

    private final ProjectionRunner<PersonEvent> runner;

    private final ViewScheduler scheduler;

    private ViewScheduler.ViewScheduleHandle handle;

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

    public void start() {
        handle = scheduler.schedule(view.getName(), view.getCron(), () -> runner.runOnce(view));
    }

    public void stop() {
        if (Objects.nonNull(handle)) {
            handle.cancel();
        }
    }
}
