package io.ddd4j.javalin.sample.cqrs.person;

import com.google.inject.Inject;
import io.ddd4j.core.cqrs.readmodel.ProjectionRunner;
import io.ddd4j.sample.cqrs.person.application.PersonCommandService;
import io.ddd4j.sample.cqrs.person.domain.CreatePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.DeletePersonCommand;
import io.ddd4j.sample.cqrs.person.domain.PersonEvent;
import io.ddd4j.sample.cqrs.person.domain.PersonId;
import io.ddd4j.sample.cqrs.person.query.PersonListView;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class JavalinPersonController {

    private final PersonCommandService commandService;

    private final PersonListView view;

    private final ProjectionRunner<PersonEvent> runner;

    @Inject
    public JavalinPersonController(
            PersonCommandService commandService,
            PersonListView view,
            ProjectionRunner<PersonEvent> runner
    ) {
        this.commandService = commandService;
        this.view = view;
        this.runner = runner;
    }

    public void register(Javalin app) {
        app.unsafe.routes.post("/persons/create", this::create);
        app.unsafe.routes.get("/persons", this::all);
        app.unsafe.routes.get("/persons/{personId}", this::get);
        app.unsafe.routes.delete("/persons/{personId}", this::delete);
    }

    public void create(Context ctx) {
        PersonId personId = commandService.create(ctx.bodyAsClass(CreatePersonCommand.class));
        runner.runOnce(view);
        ctx.json(Map.of("personId", personId.getValue()));
    }

    public void all(Context ctx) {
        runner.runOnce(view);
        ctx.json(view.findAll());
    }

    public void get(Context ctx) {
        runner.runOnce(view);
        ctx.json(view.findById(ctx.pathParam("personId")).orElse(null));
    }

    public void delete(Context ctx) {
        commandService.delete(DeletePersonCommand.builder()
                .personId(ctx.pathParam("personId"))
                .build());
        runner.runOnce(view);
        ctx.json(Map.of("deleted", true));
    }
}
