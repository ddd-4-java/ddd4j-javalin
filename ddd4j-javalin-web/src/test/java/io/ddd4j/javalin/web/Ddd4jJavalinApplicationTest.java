package io.ddd4j.javalin.web;

import com.google.inject.AbstractModule;
import io.ddd4j.cache.subject.InMemorySubject;
import io.ddd4j.cache.subject.InMemorySubjectProvider;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.cqrs.command.CommandBus;
import io.ddd4j.core.subject.SubjectProvider;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * 生产启动入口必须安装并注册完整 Guice CQRS Runtime。
     */
    @Test
    void shouldRegisterCommandBusFromProductionBootstrap() {
        BaseContext.remove(SpiKeys.COMMAND_BUS);
        Javalin app = Ddd4jJavalinApplication.run(new String[]{"0"}, "");

        try {
            assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isPresent();
        } finally {
            app.stop();
            BaseContext.remove(SpiKeys.COMMAND_BUS);
        }
    }

    /**
     * Javalin 停止时必须关闭 Guice Runtime 并撤销全局 SPI。
     */
    @Test
    void shouldUnregisterCommandBusWhenApplicationStops() {
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

    /**
     * 业务 extra module 必须能够覆盖 Runtime 的默认 SPI 实现。
     */
    @Test
    void shouldAllowExtraModuleToOverrideDefaultSubjectProvider() {
        InMemorySubjectProvider provider = new InMemorySubjectProvider(new InMemorySubject(event -> {
        }));
        Javalin app = Ddd4jJavalinApplication.run(new String[]{"0"}, "", new AbstractModule() {
            @Override
            protected void configure() {
                bind(SubjectProvider.class).toInstance(provider);
            }
        });

        try {
            assertThat(Contexts.get(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class)).contains(provider);
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldServeHealthEndpointUnderConfiguredContextPath() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setContextPath("/api");
        Javalin app = Ddd4jJavalinApplication.run(properties, new String[0], "");

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/api/health"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("UP");
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldRejectRequestBodyLargerThanConfiguredMaximum() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean();
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setDefaultAuthenticationMode(io.ddd4j.web.core.auth.AuthenticationMode.DISABLED);
        properties.setMaxUploadSizeBytes(8L);
        Javalin app = Ddd4jJavalinApplication.run(properties, new String[0], "");
        app.post("/body", context -> {
            String body = context.body();
            invoked.set(true);
            context.result(body);
        });

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/body"))
                            .POST(HttpRequest.BodyPublishers.ofString("body-is-larger-than-eight-bytes"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(413);
            assertThat(invoked).isFalse();
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldEnableCorsForConfiguredApplication() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setCors(true);
        properties.setDefaultAuthenticationMode(io.ddd4j.web.core.auth.AuthenticationMode.DISABLED);
        Javalin app = Ddd4jJavalinApplication.run(properties, new String[0], "");
        app.get("/cors", context -> context.result("ok"));

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/cors"))
                            .header("Origin", "https://client.example")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("*");
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldSkipWebLifecycleWhenDisabled() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setRequestLifecycle(false);
        Javalin app = Ddd4jJavalinApplication.run(properties, new String[0], "");
        app.get("/unmanaged", context -> context.result("open"));

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/unmanaged"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("open");
            assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isPresent();
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldApplyConfiguredAsyncRequestTimeout() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setRequestLifecycle(false);
        properties.setRequestTimeoutMs(100L);
        Javalin app = Ddd4jJavalinApplication.run(properties, new String[0], "");
        app.get("/slow", context -> context.future(() -> CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500L);
                context.result("late");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("async test interrupted", exception);
            }
        })));

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/slow"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(response.body()).containsIgnoringCase("timeout");
        } finally {
            app.stop();
        }
    }
}
