package io.ddd4j.javalin.web;

import io.ddd4j.core.health.ReadinessReport;
import io.ddd4j.javalin.core.lifecycle.Ddd4jJavalinRuntime;
import io.javalin.Javalin;

import java.util.Objects;

/** 注册不暴露异常和凭证的 Javalin 健康检查路由。 */
public final class Ddd4jJavalinReadinessRoutes {

    private Ddd4jJavalinReadinessRoutes() {
    }

    /** 注册 liveness、readiness 与聚合 health 路由。 */
    public static void register(Javalin app, Ddd4jJavalinRuntime runtime) {
        Objects.requireNonNull(app, "app must not be null");
        Objects.requireNonNull(runtime, "runtime must not be null");
        app.get("/health/liveness", context -> context.contentType("application/json")
                .result("{\"status\":\"LIVE\"}"));
        app.get("/health/readiness", context -> readiness(context, runtime));
        app.get("/health", context -> health(context, runtime));
    }

    private static void readiness(io.javalin.http.Context context, Ddd4jJavalinRuntime runtime) {
        ReadinessReport report = runtime.readiness();
        context.status(report.ready() ? 200 : 503)
                .contentType("application/json")
                .result(report.ready()
                        ? "{\"status\":\"READY\"}"
                        : "{\"status\":\"UNAVAILABLE\"}");
    }

    private static void health(io.javalin.http.Context context, Ddd4jJavalinRuntime runtime) {
        ReadinessReport report = runtime.readiness();
        context.status(report.ready() ? 200 : 503)
                .contentType("application/json")
                .result(report.ready()
                        ? "{\"status\":\"UP\"}"
                        : "{\"status\":\"DOWN\"}");
    }
}
