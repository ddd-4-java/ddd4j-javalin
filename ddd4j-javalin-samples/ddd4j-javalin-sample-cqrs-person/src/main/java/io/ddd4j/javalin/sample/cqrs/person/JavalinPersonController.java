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

/**
 * 人员示例 REST 控制器。
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 7.1.x
 */
public class JavalinPersonController {

    private final PersonCommandService commandService;

    private final PersonListView view;

    private final ProjectionRunner<PersonEvent> runner;

    /**
     * 构造 JavalinPersonController 并注入依赖。
     * @param commandService 人员命令服务。
     * @param view 人员读模型视图。
     * @param runner 投影执行器。
     */
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

    /**
     * 注册人员示例路由。
     * @param app Javalin 应用实例。
     */
    public void register(Javalin app) {
        app.unsafe.routes.post("/persons/create", this::create);
        app.unsafe.routes.get("/persons", this::all);
        app.unsafe.routes.get("/persons/{personId}", this::get);
        app.unsafe.routes.delete("/persons/{personId}", this::delete);
    }

    /**
     * 创建人员。
     * @param ctx Javalin 请求上下文。
     */
    public void create(Context ctx) {
        PersonId personId = commandService.create(ctx.bodyAsClass(CreatePersonCommand.class));
        runner.runOnce(view);
        ctx.json(Map.of("personId", personId.getValue()));
    }

    /**
     * 查询全部人员列表。
     * @param ctx Javalin 请求上下文。
     */
    public void all(Context ctx) {
        runner.runOnce(view);
        ctx.json(view.findAll());
    }

    /**
     * 按标识查询人员详情。
     * @param ctx Javalin 请求上下文。
     */
    public void get(Context ctx) {
        runner.runOnce(view);
        ctx.json(view.findById(ctx.pathParam("personId")).orElse(null));
    }

    /**
     * 按标识删除人员。
     * @param ctx Javalin 请求上下文。
     */
    public void delete(Context ctx) {
        commandService.delete(DeletePersonCommand.builder()
                .personId(ctx.pathParam("personId"))
                .build());
        runner.runOnce(view);
        ctx.json(Map.of("deleted", true));
    }
}
