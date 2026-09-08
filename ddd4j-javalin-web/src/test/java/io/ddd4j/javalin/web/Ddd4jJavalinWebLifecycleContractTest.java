package io.ddd4j.javalin.web;

import com.google.inject.Guice;
import io.ddd4j.cache.subject.InMemorySubject;
import io.ddd4j.cache.subject.InMemorySubjectProvider;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.ThreadContext;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.web.core.auth.AuthenticationMode;
import io.ddd4j.web.core.context.WebContextScope;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ddd4j Javalin 请求生命周期的消费者行为契约。
 */
class Ddd4jJavalinWebLifecycleContractTest {

    private static final String REQUEST_ID = "X-Request-Id";
    private static final String TRACE_ID = "X-Trace-Id";

    /**
     * 默认访问策略要求受保护路径提供 Bearer Token。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldRejectProtectedRequestWithoutBearerToken() throws Exception {
        Ddd4jJavalinWeb web = Guice.createInjector(Ddd4jJavalinAutoConfiguration.forTesting())
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.get("/protected", context -> context.result("secret"));
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/protected"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(401);
        } finally {
            app.stop();
        }
    }

    /**
     * 调用方提供的请求标识必须传播到响应头。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldPropagateRequestIdOnPublicRequest() throws Exception {
        Ddd4jJavalinWeb web = Guice.createInjector(Ddd4jJavalinAutoConfiguration.forTesting())
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.get("/health", context -> context.result("UP"));
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/health"))
                            .header(REQUEST_ID, "request-123")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue(REQUEST_ID)).contains("request-123");
        } finally {
            app.stop();
        }
    }

    /**
     * 调用方提供的追踪标识必须传播到响应头。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldPropagateTraceIdOnPublicRequest() throws Exception {
        Ddd4jJavalinWeb web = Guice.createInjector(Ddd4jJavalinAutoConfiguration.forTesting())
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.get("/health", context -> context.result("UP"));
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/health"))
                            .header(TRACE_ID, "trace-123")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue(TRACE_ID)).contains("trace-123");
        } finally {
            app.stop();
        }
    }

    /**
     * 请求结束后必须清理服务器线程中的 ddd4j 上下文。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldClearThreadContextAfterRequest() throws Exception {
        AtomicReference<Map<Object, Object>> resourcesAfterLifecycle = new AtomicReference<>();
        Ddd4jJavalinWeb web = Guice.createInjector(Ddd4jJavalinAutoConfiguration.forTesting())
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(config -> {
            web.configure(config);
            config.router.mount(routing -> routing.after(context ->
                    resourcesAfterLifecycle.set(ThreadContext.getResources())));
        });
        app.get("/health", context -> context.result("UP"));
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/health"))
                            .header(REQUEST_ID, "cleanup-request")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(resourcesAfterLifecycle.get()).isEmpty();
        } finally {
            app.stop();
        }
    }

    /**
     * 有效 Bearer Token 对应的 Subject 必须绑定到路由执行线程。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldBindAuthenticatedSubjectDuringProtectedRequest() throws Exception {
        InMemorySubject subject = new InMemorySubject(event -> {
        });
        String token = subject.login(AuthRequest.of("contract-user"));
        BaseContext.inject(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class,
                new InMemorySubjectProvider(subject));

        Ddd4jJavalinWeb web = Guice.createInjector(Ddd4jJavalinAutoConfiguration.forTesting())
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.get("/protected", context -> context.result(
                String.valueOf(ThreadContext.getSubject() == subject)));
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/protected"))
                            .header("Authorization", "Bearer " + token)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("true");
        } finally {
            app.stop();
            BaseContext.remove(SpiKeys.SUBJECT_PROVIDER);
        }
    }

    /**
     * 显式关闭认证时，普通业务路由无需 Bearer Token。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldAllowRequestWhenAuthenticationModeIsDisabled() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        Ddd4jJavalinWeb web = Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.get("/protected", context -> context.result("open"));
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/protected"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("open");
        } finally {
            app.stop();
        }
    }

    /**
     * 开启可信代理后必须从 Forwarded 头解析真实客户端地址。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldResolveForwardedClientIpWhenTrusted() throws Exception {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        properties.setTrustForwardedHeaders(true);
        Ddd4jJavalinWeb web = Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.get("/client-ip", context -> {
            Object clientIp = ThreadContext.get(WebContextScope.CLIENT_IP);
            context.result(String.valueOf(clientIp));
        });
        app.start("127.0.0.1", 0);

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/client-ip"))
                            .header("X-Forwarded-For", "203.0.113.10, 10.0.0.2")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode())
                    .withFailMessage("Unexpected response: %s", response.body())
                    .isEqualTo(200);
            assertThat(response.body()).isEqualTo("203.0.113.10");
        } finally {
            app.stop();
        }
    }

    /**
     * 相同幂等键只允许业务处理器成功执行一次。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldRejectDuplicateIdempotentRequest() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        Ddd4jJavalinWeb web = Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.post("/orders", context -> context.result(String.valueOf(invocations.incrementAndGet())));
        app.start("127.0.0.1", 0);

        try {
            URI uri = URI.create("http://127.0.0.1:" + app.port() + "/orders");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Idempotency-Key", "order-123")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> first = client.send(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> duplicate = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(first.statusCode()).isEqualTo(200);
            assertThat(duplicate.statusCode()).isEqualTo(409);
            assertThat(invocations.get()).isEqualTo(1);
        } finally {
            app.stop();
        }
    }

    /**
     * 关闭幂等防护时，相同幂等键不应拦截业务执行。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldAllowDuplicateRequestWhenIdempotencyIsDisabled() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        properties.setIdempotencyEnabled(false);
        Ddd4jJavalinWeb web = Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.post("/orders", context -> context.result(String.valueOf(invocations.incrementAndGet())));
        app.start("127.0.0.1", 0);

        try {
            URI uri = URI.create("http://127.0.0.1:" + app.port() + "/orders");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Idempotency-Key", "disabled-key")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> first = HttpClient.newHttpClient().send(
                    request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> second = HttpClient.newHttpClient().send(
                    request, HttpResponse.BodyHandlers.ofString());

            assertThat(first.statusCode()).isEqualTo(200);
            assertThat(second.statusCode()).isEqualTo(200);
            assertThat(invocations).hasValue(2);
        } finally {
            app.stop();
        }
    }

    /**
     * 自定义幂等缓存名必须驱动真实 CacheKit 注册。
     */
    @Test
    void shouldRegisterConfiguredIdempotencyCache() {
        String cacheName = "contract-custom-idempotency";
        CacheKit.unregister(cacheName);
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setIdempotencyCacheName(cacheName);

        try {
            Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                    .getInstance(Ddd4jJavalinWeb.class);
            assertThat(CacheKit.getCache(cacheName)).isNotNull();
        } finally {
            CacheKit.unregister(cacheName);
        }
    }

    /**
     * 已完成的幂等键在配置 TTL 到期后必须允许再次执行。
     *
     * @throws Exception HTTP 请求失败
     */
    @Test
    void shouldExpireIdempotencyKeyUsingConfiguredTtl() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setDefaultAuthenticationMode(AuthenticationMode.DISABLED);
        properties.setIdempotencyCacheName("contract-short-ttl");
        properties.setIdempotencyTtl(Duration.ofSeconds(1));
        Ddd4jJavalinWeb web = Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class);
        Javalin app = Javalin.create(web::configure);
        app.post("/orders", context -> context.result(String.valueOf(invocations.incrementAndGet())));
        app.start("127.0.0.1", 0);

        try {
            URI uri = URI.create("http://127.0.0.1:" + app.port() + "/orders");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Idempotency-Key", "short-lived-key")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpClient client = HttpClient.newHttpClient();

            assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
            assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(409);
            Thread.sleep(1_200L);
            assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
            assertThat(invocations).hasValue(2);
        } finally {
            app.stop();
            CacheKit.unregister("contract-short-ttl");
        }
    }

    /**
     * 默认本地缓存不支持亚秒 TTL，必须在装配期显式拒绝。
     */
    @Test
    void shouldRejectSubSecondIdempotencyTtl() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setIdempotencyCacheName("contract-invalid-ttl");
        properties.setIdempotencyTtl(Duration.ofMillis(500));

        assertThatThrownBy(() -> Guice.createInjector(new Ddd4jJavalinAutoConfiguration(properties))
                .getInstance(Ddd4jJavalinWeb.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasStackTraceContaining("idempotencyTtl must be at least one second");
    }
}
