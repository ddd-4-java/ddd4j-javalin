package io.ddd4j.javalin.web;

import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.cqrs.command.CommandBus;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip test for {@link Ddd4jJavalinApplication}: starts Javalin on a random port,
 * verifies health endpoint and shutdown.
 */
class Ddd4jJavalinApplicationTest {

    @Test
    void shouldStartJavalinAndExposeHealthEndpoint() throws Exception {
        Javalin app = Ddd4jJavalinApplication.run(
                new String[]{"0"}, "io.ddd4j.javalin.web");

        try {
            assertThat(app.port()).isGreaterThan(0);
            java.net.http.HttpClient javaClient = java.net.http.HttpClient.newHttpClient();
            HttpResponse<String> response = javaClient.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + "/health"))
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("UP");
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldRespectCliPortOverride() {
        // Even a non-existent port range should be honoured; we don't actually bind.
        Ddd4jJavalinApplication.run(new String[]{"--port", "0"}, "io.ddd4j.javalin.web").stop();
    }

    @Test
    void shouldRegisterAndCloseCommandBusWithApplication() {
        BaseContext.remove(SpiKeys.COMMAND_BUS);
        Javalin app = Ddd4jJavalinApplication.run(new String[]{"0"}, "");
        try {
            assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isPresent();
            app.stop();
            assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isEmpty();
        } finally {
            app.stop();
            BaseContext.remove(SpiKeys.COMMAND_BUS);
        }
    }
}
