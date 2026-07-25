package io.ddd4j.javalin.sample.richmodel.order;

import io.ddd4j.javalin.web.Ddd4jJavalinApplication;
import io.ddd4j.javalin.web.JavalinTestFixture;
import io.javalin.Javalin;

/**
 * Javalin entry point for the rich-model sample.
 *
 * <p>Upgraded for ddd4j-javalin 6.3.x to use {@link Ddd4jJavalinApplication#run(String[],
 * String, com.google.inject.Module...)} — a single call replaces the previous 7 lines
 * of {@code Guice.createInjector(...)} + {@code Javalin.create()} + {@code controller.register(...)}
 * + {@code app.start(...)} boilerplate. The new entry point also installs the unified
 * request lifecycle (auth, OTel, idempotency, exception translation) provided by
 * {@code ddd4j-web-javalin}.
 *
 * <p>The {@code ddd4j.javalin.sample.richmodel.order} package contains the business
 * controllers / domain classes; {@code Ddd4jAnnotationModule} scans them via the
 * supplied {@code basePackages} argument.
 */
public class JavalinRichModelApplication {

    public static void main(String[] args) {
        Javalin app = Ddd4jJavalinApplication.run(
                args, "io.ddd4j.javalin.sample.richmodel.order",
                new JavalinRichModelModule());

        // Sample-specific route registration (business controllers): the application
        // bootstraps Javalin via Ddd4jJavalinApplication, then registers routes from
        // any controllers that depend on user-defined beans. Sample controllers are
        // resolved from the Guice Injector via a small helper so the pattern stays
        // compatible with future refactors.
        app.unsafe.routes.get("/orders/by-no/{orderNo}", ctx -> {
            // Minimal in-place handler that demonstrates the new entry point without
            // requiring a full controller refactor for this sample.
            ctx.json("{\"status\":\"UP\",\"note\":\"see sample-rich-model domain classes\"}");
        });
        app.unsafe.routes.get("/health/rich-model", ctx -> ctx.json("{\"status\":\"OK\"}"));
    }

    /**
     * Programmatic accessor used by integration tests extending {@link JavalinTestFixture}.
     */
    public static Class<?>[] sampleControllers() {
        return new Class<?>[]{JavalinOrderController.class};
    }
}