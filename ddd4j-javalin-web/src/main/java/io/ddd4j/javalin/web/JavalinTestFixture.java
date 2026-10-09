package io.ddd4j.javalin.web;

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
import io.ddd4j.web.javalin.Ddd4jJavalinWeb;
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

    /** 每个测试用例启动的 Javalin 实例，监听随机空闲端口。 */
    protected Javalin app;

    /** 由测试模块装配出的 Guice 注入器。 */
    protected Injector injector;

    /** 指向本测试服务器的可复用 HTTP 客户端。 */
    protected HttpClient client;

    /**
     * 构造测试夹具。
     */
    protected JavalinTestFixture() {
    }

    /**
     * 覆写以扫描额外包中的 DDD 注解。默认：空数组（不扫描）。
     *
     * @return 需要扫描的应用包名数组
     */
    protected String[] basePackages() {
        return new String[0];
    }

    /**
     * 覆写以追加额外的 Guice 模块（auth、data、mq 等）。默认：无。
     *
     * @return 额外装配的 Guice 模块数组
     */
    protected Module[] extraModules() {
        return new Module[0];
    }

    /**
     * 覆写以在 ddd4j 生命周期钩子安装完成后追加自定义路由。
     *
     * @param app 已完成生命周期装配的 Javalin 实例
     */
    protected void configureRoutes(Javalin app) {
        // 默认不追加路由
    }

    /**
     * 覆写以在注入器创建后执行断言。
     *
     * @param injector 测试创建的 Guice 注入器
     */
    protected void afterInjector(Injector injector) {
        // 默认不执行断言
    }

    @BeforeEach
    void startJavalin() {
        Ddd4jJavalinProperties properties = new Ddd4jJavalinProperties();
        properties.setPort(0);
        properties.setHost("127.0.0.1");

        Module webModule = new Ddd4jJavalinAutoConfiguration(properties);
        java.util.List<Module> head = new java.util.ArrayList<>();
        // The full Ddd4jGuiceModule binds DefaultProjectionService which lacks an
        // @Inject constructor in ddd4j 2.0.x; provide the minimum SPIs manually.
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
        app = Javalin.create(config -> web.configure(config));
        // Default health endpoint for tests.
        app.unsafe.routes.get("/health", ctx -> ctx.json("{\"status\":\"UP\"}"));
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
     * 构造指向测试服务器的 URL。
     *
     * @param path 请求路径（以 {@code /} 开头）
     * @return 指向 {@code http://localhost:<随机端口><path>} 的 URI
     */
    protected java.net.URI url(String path) {
        return java.net.URI.create("http://localhost:" + app.port() + path);
    }

    /**
     * 便捷方法：发送 HTTP 请求并返回字符串响应。
     *
     * @param request 待发送的 HTTP 请求
     * @return 字符串响应
     * @throws Exception 当请求发送失败或响应读取失败时抛出
     */
    protected java.net.http.HttpResponse<String> http(java.net.http.HttpRequest request) throws Exception {
        return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 便捷方法：发送 HTTP 请求并使用自定义响应体处理器。
     *
     * @param <T>     响应体类型
     * @param request 待发送的 HTTP 请求
     * @param handler 响应体处理器
     * @return 按 {@code handler} 解析后的响应
     * @throws Exception 当请求发送失败或响应体读取失败时抛出
     */
    protected <T> java.net.http.HttpResponse<T> http(java.net.http.HttpRequest request,
                                                     java.net.http.HttpResponse.BodyHandler<T> handler) throws Exception {
        return client.send(request, handler);
    }

    /**
     * 对存活的 {@link Injector} 执行断言回调。
     *
     * @param assertion 以注入器为入参的断言回调，不允许为 {@code null}
     */
    protected void withInjector(Consumer<Injector> assertion) {
        assertion.accept(injector);
    }

    /**
     * Minimal SPI bindings for the Javalin web layer to start in isolation. We avoid the
     * full {@code Ddd4jGuiceModule} because it transitively pulls in
     * {@code DefaultProjectionService} which currently has no {@code @Inject}
     * constructor (tracked upstream as ddd4j 2.0.x issue).
     */
    private static final class MinimalSpiModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(SubjectProvider.class).to(GuiceSubjectProvider.class).in(Singleton.class);
            bind(I18nProvider.class).to(GuiceI18nProvider.class).in(Singleton.class);
        }
    }
}
