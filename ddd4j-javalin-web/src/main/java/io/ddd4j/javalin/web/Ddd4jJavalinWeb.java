package io.ddd4j.javalin.web;

import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.ddd4j.web.core.error.WebExceptionTranslator;
import io.javalin.config.JavalinConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Javalin Web 配置器（6.7.x 本地实现，使用 Javalin 6 API）。
 *
 * <p>替代上游 ddd4j-web-javalin:2.0.x 依赖（该 jar 基于 Javalin 7 编译，
 * 与 Javalin 6.7.0 运行时不兼容）。
 */
@Slf4j
public class Ddd4jJavalinWeb {

    private final WebRequestContextFactory contextFactory;
    private final WebRequestLifecycle lifecycle;
    private final WebExceptionTranslator translator;

    public Ddd4jJavalinWeb(WebRequestContextFactory contextFactory,
                            WebRequestLifecycle lifecycle,
                            WebExceptionTranslator translator,
                            Object unused) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.translator = Objects.requireNonNull(translator);
    }

    /**
     * 配置 Javalin 实例（Javalin 6 API）。
     */
    public void configure(JavalinConfig config) {
        // Javalin 6 使用 config.router.mount 注册路由
        config.router.mount(routing -> {
            routing.exception(Exception.class, (e, ctx) -> {
                log.error("Unhandled exception", e);
                ctx.status(500).json("{\"error\":\"" + e.getMessage() + "\"}");
            });
        });
    }
}
