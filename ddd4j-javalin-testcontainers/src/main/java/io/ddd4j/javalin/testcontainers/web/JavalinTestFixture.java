package io.ddd4j.javalin.testcontainers.web;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.ddd4j.core.ddd.event.DomainEventPublisher;
import io.ddd4j.core.i18n.I18nProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.guice.DddAnnotationModule;
import io.ddd4j.guice.context.GuiceContext;
import io.ddd4j.guice.event.GuiceDomainEventPublisher;
import io.ddd4j.guice.i18n.GuiceI18nProvider;
import io.ddd4j.guice.subject.GuiceSubjectProvider;
import io.ddd4j.javalin.web.Ddd4jJavalinApplication;
import io.ddd4j.javalin.web.Ddd4jJavalinAutoConfiguration;
import io.ddd4j.javalin.web.Ddd4jJavalinProperties;
import io.ddd4j.javalin.web.Ddd4jJavalinWeb;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * JUnit 5 base class for ddd4j-javalin integration tests.
 *
 * <p>Provides:
 * <ul>
 *   <li>Random-port Javalin bootstrap via {@link Ddd4jJavalinApplication}</li>
 *   <li>Reusable Java 11 {@link HttpClient}</li>
 *   <li>Convenience URL builder: {@link #url(String)}</li>
 *   <li>Hook for subclasses to add extra modules / routes via
 *       {@link #extraModules()} and {@link #configureRoutes(Javalin)}</li>
 * </ul>
 *
 * <p>Typical usage:
 * <pre>{@code
 * class MyFeatureIT extends JavalinTestFixture {
 *     @Override protected String[] basePackages() { return new String[]{"io.example.app"}; }
 *     @Override protected void configureRoutes(Javalin app) {
 *         app.get("/hello", ctx -> ctx.result("hi"));
 *     }
 *     @Test void shouldCallHello() throws Exception {
 *         HttpResponse<String> r = http(HttpRequest.newBuilder(url("/hello")).GET().build());
 *         assertEquals("hi", r.body());
 *     }
 * }
 * }</pre>
 */
public abstract class JavalinTestFixture {

    /** 随机端口启动的 Javalin 应用实例（每个测试方法启动/停止）。 */
    protected Javalin app;

    /** 组装完成的 Guice {@link Injector}，供子类断言或取绑定。 */
    protected Injector injector;

    /** 复用的 Java 11 {@link HttpClient}，用于向被测应用发请求。 */
    protected HttpClient client;

    /**
     * Override to scan DDD annotations in extra packages. Default: empty (no scan).
     *
     * @return 需要扫描 DDD 注解的包名数组；默认空数组表示不扫描
     */
    protected String[] basePackages() {
        return new String[0];
    }

    /**
     * Override to add extra Guice modules (auth, data, mq, …). Default: none.
     *
     * @return 额外追加的 Guice 模块数组；默认空数组表示仅使用内置模块
     */
    protected Module[] extraModules() {
        return new Module[0];
    }

    /**
     * Override to add extra routes after the ddd4j lifecycle hooks are installed.
     *
     * @param app 已装配 ddd4j 生命周期钩子的 Javalin 实例
     */
    protected void configureRoutes(Javalin app) {
        // no-op by default
    }

    /**
     * Override to assert against the {@link Injector} post-creation.
     *
     * @param injector 组装完成的 Guice 注入器
     */
    protected void afterInjector(Injector injector) {
        // no-op by default
    }

    @BeforeEach
    void startJavalin() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setPort(0);
        properties.setHost("127.0.0.1");

        Module webModule = new Ddd4jJavalinAutoConfiguration(properties);
        java.util.List<Module> head = new java.util.ArrayList<>();
        // The full Ddd4jGuiceModule binds DefaultProjectionService which lacks an
        // @Inject constructor in the current ddd4j line; provide the minimum SPIs manually.
        head.add(new MinimalSpiModule());
        // Skip DddAnnotationModule when no base packages are supplied (avoid ClassGraph
        // NoOp errors when running fixture-only integration tests).
        String[] basePackages = basePackages();
        if (Objects.nonNull(basePackages) && basePackages.length > 0) {
            head.add(new DddAnnotationModule(basePackages));
        }
        head.add(webModule);
        Module[] extras = extraModules();
        Module[] all = new Module[head.size() + extras.length];
        for (int i = 0; i < head.size(); i++) {
            all[i] = head.get(i);
        }
        System.arraycopy(extras, 0, all, head.size(), extras.length);

        injector = Guice.createInjector(all);
        afterInjector(injector);

        Ddd4jJavalinWeb web = injector.getInstance(Ddd4jJavalinWeb.class);
        // ddd4j-web-javalin：统一请求生命周期等全部经 configure(config) 装配，
        // 无独立的 applyTo 步骤（与 ddd4j-sample-javalin 的标准用法一致）。
        app = Javalin.create(config -> web.configure(config));
        // Default health endpoint for tests.
        app.get("/health", ctx -> ctx.json("{\"status\":\"UP\"}"));
        configureRoutes(app);
        app.start(properties.getHost(), properties.getPort());

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopJavalin() {
        if (Objects.nonNull(app)) {
            app.stop();
        }
    }

    /**
     * Build a URL to {@code path} on the running test server.
     *
     * @param path 以 {@code /} 开头的请求路径
     * @return 指向被测服务器随机端口的完整 URI
     */
    protected java.net.URI url(String path) {
        return java.net.URI.create("http://localhost:" + app.port() + path);
    }

    /**
     * Convenience: send an HTTP request and return the response.
     *
     * @param request 已构造好的 HTTP 请求
     * @return 以字符串形式读取响应体的响应对象
     * @throws Exception 请求发送或响应读取失败
     */
    protected java.net.http.HttpResponse<String> http(java.net.http.HttpRequest request) throws Exception {
        return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Convenience: send an HTTP request with a custom body handler.
     *
     * @param <T>     响应体目标类型
     * @param request 已构造好的 HTTP 请求
     * @param handler 自定义的响应体处理器
     * @return 按指定 handler 读取响应体的响应对象
     * @throws Exception 请求发送或响应读取失败
     */
    protected <T> java.net.http.HttpResponse<T> http(java.net.http.HttpRequest request,
                                                     java.net.http.HttpResponse.BodyHandler<T> handler) throws Exception {
        return client.send(request, handler);
    }

    /**
     * Run an assertion callback against the live {@link Injector}.
     *
     * @param assertion 面向注入器的断言回调
     */
    protected void withInjector(Consumer<Injector> assertion) {
        assertion.accept(injector);
    }

    /**
     * Minimal SPI bindings for the Javalin web layer to start in isolation. We avoid the
     * full {@code Ddd4jGuiceModule} because it transitively pulls in
     * {@code DefaultProjectionService} which currently has no {@code @Inject}
     * constructor in the current core line.
     */
    private static final class MinimalSpiModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(SubjectProvider.class).to(GuiceSubjectProvider.class).in(Singleton.class);
            bind(I18nProvider.class).to(GuiceI18nProvider.class).in(Singleton.class);
        }
    }
}
