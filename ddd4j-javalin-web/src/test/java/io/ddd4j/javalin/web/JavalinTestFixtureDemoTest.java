package io.ddd4j.javalin.web;

import io.ddd4j.web.core.auth.BearerSubjectAuthenticator;
import io.ddd4j.web.core.auth.PathWebAccessPolicy;
import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.ddd4j.web.core.error.WebExceptionTranslator;
import io.ddd4j.web.javalin.Ddd4jJavalinWeb;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates how a real test class extends {@link JavalinTestFixture}: verifies the
 * Guice Injector can resolve every ddd4j-web-javalin collaborator (rather than
 * exercising the full request lifecycle which requires a registered auth subject).
 *
 * <p>For end-to-end round-trip tests with a real {@code SubjectProvider} wired in,
 * see {@code ddd4j-javalin-auth-*}.
 */
class JavalinTestFixtureDemoTest extends JavalinTestFixture {

    @Override
    protected String[] basePackages() {
        return new String[0];
    }

    @Override
    protected void configureRoutes(Javalin app) {
        app.unsafe.routes.get("/demo/hello", ctx -> ctx.result("hello, ddd4j"));
    }

    @Test
    void shouldExposeDefaultHealthEndpoint() throws Exception {
        HttpResponse<String> response = http(HttpRequest.newBuilder(url("/health")).GET().build());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void shouldResolveAllWebCollaboratorsFromGuice() {
        // Ddd4jJavalinApplication + JavalinTestFixture wire Ddd4jJavalinWeb and
        // its collaborators via the Guice Injector. Verifying they're resolvable
        // is the contract that guarantees the web layer can start.
        withInjector(injector -> {
            assertThat(injector.getInstance(Ddd4jJavalinWeb.class)).isNotNull();
            assertThat(injector.getInstance(WebRequestContextFactory.class)).isNotNull();
            assertThat(injector.getInstance(BearerSubjectAuthenticator.class)).isNotNull();
            assertThat(injector.getInstance(PathWebAccessPolicy.class)).isNotNull();
            assertThat(injector.getInstance(WebRequestLifecycle.class)).isNotNull();
            assertThat(injector.getInstance(WebExceptionTranslator.class)).isNotNull();
        });
    }
}