package io.ddd4j.javalin.web;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.cache.local.CaffeineCache;
import io.ddd4j.core.cache.CacheConfig;
import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.web.core.auth.BearerSubjectAuthenticator;
import io.ddd4j.web.core.auth.PathWebAccessPolicy;
import io.ddd4j.web.core.context.ClientIpResolver;
import io.ddd4j.web.core.context.RequestIdGenerator;
import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.ddd4j.web.core.error.DefaultWebExceptionTranslator;
import io.ddd4j.web.core.error.WebExceptionTranslator;
import io.ddd4j.web.core.idempotency.CacheIdempotencyGuard;
import io.ddd4j.web.core.idempotency.IdempotencyGuard;
import io.ddd4j.web.core.idempotency.WebIdempotencyLifecycle;
import io.ddd4j.web.javalin.Ddd4jJavalinWeb;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Guice Module that assembles the ddd4j-web-javalin stack: {@link Ddd4jJavalinWeb} plus all
 * its collaborators (request context factory, request lifecycle, exception translator,
 * access policy).
 *
 * <p>Aligned with ddd4j-boot-web-webmvc's {@code Ddd4jWebMvcAutoConfiguration}: same
 * {@code @ConditionalOnWebApplication} intent expressed via a runtime {@code enabled}
 * property check (Guice has no compile-time conditional; runtime {@code if (!properties.isEnabled())}
 * is the idiomatic Guice equivalent).
 */
@Slf4j
public class Ddd4jJavalinAutoConfiguration extends AbstractModule {

    private final Ddd4jJavalinProperties properties;
    private final IdempotencyGuard sharedIdempotencyGuard;

    /**
     * 使用默认配置创建 Web 装配模块。
     */
    public Ddd4jJavalinAutoConfiguration() {
        this(new Ddd4jJavalinProperties());
    }

    /**
     * 使用指定配置创建 Web 装配模块。
     *
     * @param properties Web 层配置，不允许为 {@code null}
     */
    public Ddd4jJavalinAutoConfiguration(Ddd4jJavalinProperties properties) {
        this(properties, null);
    }

    /**
     * 使用显式共享 Guard 创建生产级 Web 装配。
     *
     * @param properties            Web 层配置，不允许为 {@code null}
     * @param sharedIdempotencyGuard 跨实例共享的幂等 Guard；仅 SHARED 部署模式下必填，可为 {@code null}
     */
    public Ddd4jJavalinAutoConfiguration(Ddd4jJavalinProperties properties,
                                          IdempotencyGuard sharedIdempotencyGuard) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.sharedIdempotencyGuard = sharedIdempotencyGuard;
    }

    @Override
    protected void configure() {
        if (!properties.isEnabled()) {
            log.info("ddd4j.web.javalin.enabled=false; Javalin web layer will not be installed");
            return;
        }
        bind(Ddd4jJavalinProperties.class).toInstance(properties);
        bind(WebExceptionTranslator.class).to(DefaultWebExceptionTranslator.class).in(Singleton.class);
        bind(BearerSubjectAuthenticator.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    WebRequestContextFactory webRequestContextFactory() {
        ClientIpResolver clientIpResolver = properties.isTrustForwardedHeaders()
                ? ClientIpResolver.trustedProxy()
                : ClientIpResolver.remoteAddressOnly();
        return new WebRequestContextFactory(RequestIdGenerator.uuid(), clientIpResolver);
    }

    @Provides
    @Singleton
    PathWebAccessPolicy pathWebAccessPolicy() {
        String contextPath = properties.getContextPath();
        List<String> publicPaths = Arrays.asList(properties.getPublicPaths());
        if (StrKit.isNotBlank(contextPath) && !"/".equals(contextPath)) {
            publicPaths = publicPaths.stream()
                    .map(path -> contextPath + path)
                    .toList();
        }
        return new PathWebAccessPolicy(
                publicPaths,
                properties.getDefaultAuthenticationMode());
    }

    @Provides
    @Singleton
    WebRequestLifecycle webRequestLifecycle(BearerSubjectAuthenticator authenticator,
                                            PathWebAccessPolicy accessPolicy) {
        return new WebRequestLifecycle(authenticator, accessPolicy);
    }

    @Provides
    @Singleton
    Ddd4jJavalinWeb ddd4jJavalinWeb(WebRequestContextFactory contextFactory,
                                     WebRequestLifecycle lifecycle,
                                     WebExceptionTranslator translator) {
        // Guard so this @Provides is skipped when the web layer is disabled; otherwise
        // Guice eagerly resolves all parameter types during injector creation.
        if (!properties.isEnabled()) {
            throw new com.google.inject.ProvisionException(
                    "ddd4j.web.javalin.enabled=false; Ddd4jJavalinWeb is not available");
        }
        return new Ddd4jJavalinWeb(contextFactory, lifecycle, translator, idempotencyLifecycle());
    }

    private WebIdempotencyLifecycle idempotencyLifecycle() {
        if (!properties.isIdempotencyEnabled()) {
            return null;
        }
        long ttlSeconds = Objects.requireNonNull(
                properties.getIdempotencyTtl(), "idempotencyTtl must not be null").getSeconds();
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("idempotencyTtl must be at least one second");
        }
        if (properties.getIdempotencyDeploymentMode() == IdempotencyDeploymentMode.SHARED) {
            return new WebIdempotencyLifecycle(Objects.requireNonNull(sharedIdempotencyGuard,
                    "sharedIdempotencyGuard is required for SHARED mode"), properties.getIdempotencyTtl());
        }
        String cacheName = properties.getIdempotencyCacheName();
        if (Objects.isNull(CacheKit.getCache(cacheName))) {
            CacheKit.register(cacheName, CaffeineCache.create(CacheConfig.builder(cacheName)
                    .expireAfterWriteSeconds(ttlSeconds)
                    .build()));
        }
        return new WebIdempotencyLifecycle(
                new CacheIdempotencyGuard(cacheName),
                properties.getIdempotencyTtl());
    }

    /**
     * 便捷重载：自定义免认证公共路径列表，无需子类化本模块。
     *
     * @param paths 免认证的公共路径
     * @return 使用指定公共路径的装配模块
     */
    public static Ddd4jJavalinAutoConfiguration withPublicPaths(String... paths) {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setPublicPaths(paths);
        return new Ddd4jJavalinAutoConfiguration(props);
    }

    /**
     * 测试便捷方法：构建监听随机空闲端口（{@code port=0}）的装配模块。
     *
     * @return 绑定 {@code 127.0.0.1:0} 的测试用装配模块
     */
    public static Ddd4jJavalinAutoConfiguration forTesting() {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setPort(0);
        props.setHost("127.0.0.1");
        return new Ddd4jJavalinAutoConfiguration(props);
    }

    /**
     * 返回当前配置。供引导类读取。
     *
     * @return 本模块持有的 Web 层配置
     */
    public Ddd4jJavalinProperties getProperties() {
        return properties;
    }

    /**
     * 返回默认免认证公共路径，供上方便捷方法使用（如 {@link #withPublicPaths}）。
     *
     * @return 默认公共路径列表：{@code /health}、{@code /health/readiness}、{@code /health/liveness}
     */
    public static List<String> defaultPublicPaths() {
        return Arrays.asList("/health", "/health/readiness", "/health/liveness");
    }
}
