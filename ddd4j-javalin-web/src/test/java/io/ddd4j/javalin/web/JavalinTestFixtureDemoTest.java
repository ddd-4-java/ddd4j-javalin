package io.ddd4j.javalin.web;

import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates how a real test class extends {@link JavalinTestFixture}: adds an extra
 * route, asserts via the inherited {@link #http(HttpRequest)} helper.
 */
class JavalinTestFixtureDemoTest extends JavalinTestFixture {

    @Override
    protected void configureRoutes(Javalin app) {
        app.unsafe.routes.get("/demo/hello", ctx -> ctx.result("hello, ddd4j"));
    }

    @Test
    void shouldHitDemoEndpoint() throws Exception {
        HttpResponse<String> response = http(HttpRequest.newBuilder(url("/demo/hello")).GET().build());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("hello, ddd4j");
    }

    @Test
    void shouldExposeDefaultHealthEndpoint() throws Exception {
        HttpResponse<String> response = http(HttpRequest.newBuilder(url("/health")).GET().build());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }
}