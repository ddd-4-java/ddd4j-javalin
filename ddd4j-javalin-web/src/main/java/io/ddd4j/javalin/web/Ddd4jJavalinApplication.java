package io.ddd4j.javalin.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.ddd4j.guice.DddAnnotationModule;
import io.ddd4j.guice.Ddd4jGuiceRuntime;
import io.ddd4j.javalin.core.Ddd4jCoreGuiceModule;
import io.ddd4j.javalin.web.Ddd4jJavalinWeb;
import io.ddd4j.kit.lang.StrKit;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Static one-shot bootstrap entry for ddd4j-javalin applications.
 *
 * <p>Replaces the boilerplate {@code Javalin.create()} + manual route registration pattern
 * used in ddd4j-javalin samples prior to 6.3.x. Sample {@code main} methods now look like:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     Ddd4jJavalinApplication.run(args, "io.ddd4j.javalin.sample");
 * }
 * }</pre>
 *
 * <p>The {@code run} method wires:
 * <ol>
 *   <li>{@link Ddd4jGuiceModule} + {@link DddAnnotationModule} (base SPI + DDD annotation
 *       scanning over the supplied base packages)</li>
 *   <li>{@link Ddd4jJavalinAutoConfiguration} (web layer assembly)</li>
 *   <li>Any additional {@link Module}s passed by the caller (auth, data, mq, …)</li>
 * </ol>
 *
 * <p>On return, the {@link Javalin} instance is bound to the requested port and ready to
 * serve. {@link Javalin#stop()} is wired to a shutdown hook so graceful shutdown works in
 * CLI scenarios.
 */
@Slf4j
public final class Ddd4jJavalinApplication {

    private Ddd4jJavalinApplication() {
    }

    /**
     * Build the Guice injector and start Javalin. The base packages are scanned by
     * {@link DddAnnotationModule} for DDD-annotated types.
     *
     * @param args           standard CLI args (first may be a port override).
     * @param basePackages   packages to scan for {@code @DomainService}, {@code @DomainRepository},
     *                       etc.
     * @param extraModules   any number of extra Guice modules to install (auth, data, mq, …).
     * @return the running Javalin instance, ready to be returned to the caller.
     */
    @SafeVarargs
    public static Javalin run(String[] args, String basePackages, Module... extraModules) {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        return run(properties, args, basePackages, extraModules);
    }

    /**
     * 使用显式配置构建 Guice Runtime 并启动 Javalin。
     *
     * @param properties   Javalin 服务与 ddd4j Web 生命周期配置
     * @param args         CLI 参数，端口参数优先于 properties
     * @param basePackages DDD 注解扫描包
     * @param extraModules 业务扩展 Guice modules
     * @return 已启动的 Javalin 实例
     */
    @SafeVarargs
    public static Javalin run(Ddd4jJavalinProperties properties, String[] args,
                              String basePackages, Module... extraModules) {
        Objects.requireNonNull(properties, "properties must not be null");
        Objects.requireNonNull(basePackages, "basePackages must not be null");
        applyCliOverrides(properties, args);

        Module[] modules = buildModules(basePackages, properties, extraModules);
        Injector injector = Guice.createInjector(modules);

        Ddd4jJavalinWeb web = properties.isRequestLifecycle()
                ? injector.getInstance(Ddd4jJavalinWeb.class)
                : null;
        Ddd4jGuiceRuntime runtime = injector.getInstance(Ddd4jGuiceRuntime.class);
        // ddd4j-web-javalin：统一请求生命周期等全部经 configure(config) 装配，
        // 无独立的 applyTo 步骤（与 ddd4j-sample-javalin 的标准用法一致）。
        Javalin app = Javalin.create((JavalinConfig config) -> {
            config.router.contextPath = properties.getContextPath();
            config.http.maxRequestSize = properties.getMaxUploadSizeBytes();
            config.http.asyncTimeout = properties.getRequestTimeoutMs();
            if (properties.isCors()) {
                config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
            }
            if (Objects.nonNull(web)) {
                web.configure(config);
            }
            config.events.serverStopped(runtime::close);
        });
        applyHealthEndpoint(app, properties);
        app.start(properties.getHost(), properties.getPort());

        log.info("ddd4j-javalin started on http://{}:{} (context={})",
                properties.getHost(), app.port(), properties.getContextPath());

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop, "ddd4j-javalin-shutdown"));
        return app;
    }

    private static Module[] buildModules(String basePackages,
                                        Ddd4jJavalinProperties properties,
                                        Module[] extraModules) {
        Module web = new Ddd4jJavalinAutoConfiguration(properties);
        java.util.List<Module> head = new java.util.ArrayList<>();
        head.add(Ddd4jCoreGuiceModule.defaults());
        if (StrKit.isNotBlank(basePackages)) {
            head.add(new DddAnnotationModule(basePackages));
        }
        head.add(web);
        Module defaults = Modules.combine(head);
        if (extraModules.length == 0) {
            return new Module[]{defaults};
        }
        return new Module[]{Modules.override(defaults).with(extraModules)};
    }

    private static void applyCliOverrides(Ddd4jJavalinProperties properties, String[] args) {
        if (Objects.isNull(args)) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--port".equals(arg) && i + 1 < args.length) {
                properties.setPort(Integer.parseInt(args[++i]));
            } else if (Objects.nonNull(arg) && arg.matches("\\d{1,5}")) {
                // tolerate a bare port as the first argument
                properties.setPort(Integer.parseInt(arg));
            }
        }
    }

    private static void applyHealthEndpoint(Javalin app, Ddd4jJavalinProperties properties) {
        if (properties.isHealthEndpoint()) {
            // Javalin 6 API：start() 前直接注册路由（7.x 的 app.unsafe.routes 在 6.7.0 不存在）
            app.get("/health", ctx -> ctx.json("{\"status\":\"UP\"}"));
            app.get("/health/readiness", ctx -> ctx.json("{\"status\":\"READY\"}"));
            app.get("/health/liveness", ctx -> ctx.json("{\"status\":\"LIVE\"}"));
        }
    }
}
