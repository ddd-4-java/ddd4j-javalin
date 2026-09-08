package io.ddd4j.javalin.web;

import com.google.inject.Guice;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.web.core.auth.AuthenticationMode;
import io.ddd4j.web.core.context.WebContextScope;
import io.ddd4j.web.javalin.Ddd4jJavalinWeb;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Javalin 7 Web 装配的消费者行为契约。
 */
class Ddd4jJavalinWebLifecycleContractTest {

    @Test
    void shouldRejectProtectedRequestWithoutBearerToken() throws Exception {
        Javalin app = start(new Ddd4jJavalinProperties(),
                value -> value.unsafe.routes.get("/protected", context -> context.result("secret")));
        try {
            assertThat(send(app, "/protected", null).statusCode()).isEqualTo(401);
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldResolveForwardedClientIpWhenTrusted() throws Exception {
        Ddd4jJavalinProperties properties = disabledAuthentication();
        properties.setTrustForwardedHeaders(true);
        Javalin app = start(properties, value -> value.unsafe.routes.get("/client-ip", context -> {
            Object clientIp = ThreadContext.get(WebContextScope.CLIENT_IP);
            context.result(String.valueOf(clientIp));
        }));
        try {
            HttpRequest request = HttpRequest.newBuilder(uri(app, "/client-ip"))
                    .header("X-Forwarded-For", "203.0.113.10, 10.0.0.2")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("203.0.113.10");
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldRejectDuplicateIdempotentRequest() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        Javalin app = start(disabledAuthentication(), value -> value.unsafe.routes.post(
                "/orders", context -> context.result(String.valueOf(invocations.incrementAndGet()))));
        try {
            HttpResponse<String> first = send(app, "/orders", "order-71");
            HttpResponse<String> duplicate = send(app, "/orders", "order-71");
            assertThat(first.statusCode()).isEqualTo(200);
            assertThat(duplicate.statusCode()).isEqualTo(409);
            assertThat(invocations).hasValue(1);
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldAllowDuplicateRequestWhenIdempotencyIsDisabled() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        Ddd4jJavalinProperties properties = disabledAuthentication();
        properties.setIdempotencyEnabled(false);
        Javalin app = start(properties, value -> value.unsafe.routes.post(
                "/orders", context -> context.result(String.valueOf(invocations.incrementAndGet()))));
        try {
            assertThat(send(app, "/orders", "disabled-71").statusCode()).isEqualTo(200);
            assertThat(send(app, "/orders", "disabled-71").statusCode()).isEqualTo(200);
            assertThat(invocations).hasValue(2);
        } finally {
            app.stop();
        }
    }

    @Test
    void shouldHonorConfiguredIdempotencyCacheAndTtl() throws Exception {
        String cacheName = "contract-71-short-ttl";
        AtomicInteger invocations = new AtomicInteger();
        Ddd4jJavalinProperties properties = disabledAuthentication();
        properties.setIdempotencyCacheName(cacheName);
        properties.setIdempotencyTtl(Duration.ofSeconds(1));
        Javalin app = start(properties, value -> value.unsafe.routes.post(
                "/orders", context -> context.result(String.valueOf(invocations.incrementAndGet()))));
        try {
            assertThat(CacheKit.getCache(cacheName)).isNotNull();
            assertThat(send(app, "/orders", "ttl-71").statusCode()).isEqualTo(200);
            assertThat(send(app, "/orders", "ttl-71").statusCode()).isEqualTo(409);
            Thread.sleep(1_200L);
            assertThat(send(app, "/orders", "ttl-71").statusCode()).isEqualTo(200);
            assertThat(invocations).hasValue(2);
        } finally {
            app.stop();
            CacheKit.unregister(cacheName);
        }
    }

    @Test
    void shouldRejectSubSecondIdempotencyTtl() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setIdempotencyCacheName("contract-71-invalid-ttl");
        properties.setIdempotencyTtl(Duration.ofMillis(500));

        assertThatThrownBy(() -> Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasStackTraceContaining("idempotencyTtl must be at least one second");
    }

    private Ddd4jJavalinProperties disabledAuthentication() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        return properties;
    }

    private Javalin start(Ddd4jJavalinProperties properties, Consumer<Javalin> routes) {
        Ddd4jJavalinWeb web = Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        routes.accept(app);
        return app.start("127.0.0.1", 0);
    }

    private HttpResponse<String> send(Javalin app, String path, String idempotencyKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(app, path));
        if (Objects.isNull(idempotencyKey)) {
            request.GET();
        } else {
            request.header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.noBody());
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(Javalin app, String path) {
        return URI.create("http://127.0.0.1:" + app.port() + path);
    }
}
