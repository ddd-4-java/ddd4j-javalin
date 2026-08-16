package io.ddd4j.javalin.web;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.ddd4j.core.i18n.I18nProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.guice.DddAnnotationModule;
import io.ddd4j.guice.i18n.GuiceI18nProvider;
import io.ddd4j.guice.subject.GuiceSubjectProvider;
import io.ddd4j.web.javalin.Ddd4jJavalinWeb;
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
        Objects.requireNonNull(basePackages, "basePackages must not be null");

        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        applyCliOverrides(properties, args);

        Module[] modules = buildModules(basePackages, properties, extraModules);
        Injector injector = Guice.createInjector(modules);

        Ddd4jJavalinWeb web = injector.getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create((JavalinConfig config) -> web.configure(config));
        web.applyTo(app);
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
        // The full Ddd4jGuiceModule binds DefaultProjectionService which has no
        // @Inject constructor in ddd4j 2.0.x. Provide the minimum SPIs manually;
        // consumers may pass their own Ddd4jGuiceModule via extraModules to override.
        head.add(new MinimalSpiModule());
        if (basePackages != null && !basePackages.isBlank()) {
            head.add(new DddAnnotationModule(basePackages));
        }
        head.add(web);
        Module[] all = new Module[head.size() + extraModules.length];
        for (int i = 0; i < head.size(); i++) {
            all[i] = head.get(i);
        }
        System.arraycopy(extraModules, 0, all, head.size(), extraModules.length);
        return all;
    }

    /**
     * Minimal SPI bindings for the Javalin web layer to start in isolation. See the
     * corresponding helper inside {@link JavalinTestFixture} for rationale.
     */
    private static final class MinimalSpiModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(SubjectProvider.class).to(GuiceSubjectProvider.class).in(Singleton.class);
            bind(I18nProvider.class).to(GuiceI18nProvider.class).in(Singleton.class);
        }
    }

    private static void applyCliOverrides(Ddd4jJavalinProperties properties, String[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--port".equals(arg) && i + 1 < args.length) {
                properties.setPort(Integer.parseInt(args[++i]));
            } else if (arg != null && arg.matches("\\d{2,5}")) {
                // tolerate a bare port as the first argument
                properties.setPort(Integer.parseInt(arg));
            }
        }
    }

    private static void applyHealthEndpoint(Javalin app, Ddd4jJavalinProperties properties) {
        if (properties.isHealthEndpoint()) {
            app.get("/health", ctx -> ctx.json("{\"status\":\"UP\"}"));
            app.get("/health/readiness", ctx -> ctx.json("{\"status\":\"READY\"}"));
            app.get("/health/liveness", ctx -> ctx.json("{\"status\":\"LIVE\"}"));
        }
    }
}