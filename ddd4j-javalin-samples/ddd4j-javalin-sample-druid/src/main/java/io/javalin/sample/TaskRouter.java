package io.javalin.sample;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Handler;
import io.javalin.sample.controller.TaskController;
import io.javalin.sample.misc.Exchange;
import io.javalin.sample.misc.WrappedHandler;

@Singleton
public class TaskRouter implements EndpointGroup {

    @Inject
    private TaskController taskController;

    @Override
    public void addEndpoints() {
        get("/internal/health", taskController::health);
    }

    private Handler wrapHandler(WrappedHandler handler) {
        return ctx -> {
            var wrapCtx = new Exchange(ctx);
            handler.handle(wrapCtx);
        };
    }

    private void post(String path, WrappedHandler handler) {
        ApiBuilder.post(path, wrapHandler(handler));
    }

    private void get(String path, WrappedHandler handler) {
        ApiBuilder.get(path, wrapHandler(handler));
    }
}
