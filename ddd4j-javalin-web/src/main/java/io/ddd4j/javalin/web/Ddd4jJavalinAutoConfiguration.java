package io.ddd4j.javalin.web;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.cache.local.CaffeineCache;
import io.ddd4j.core.cache.CacheConfig;
import io.ddd4j.web.core.auth.BearerSubjectAuthenticator;
import io.ddd4j.web.core.auth.PathWebAccessPolicy;
import io.ddd4j.web.core.context.ClientIpResolver;
import io.ddd4j.web.core.context.RequestIdGenerator;
import io.ddd4j.web.core.context.WebRequestContextFactory;
import io.ddd4j.web.core.context.WebRequestLifecycle;
import io.ddd4j.web.core.error.DefaultWebExceptionTranslator;
import io.ddd4j.web.core.error.WebExceptionTranslator;
import io.ddd4j.web.core.idempotency.CacheIdempotencyGuard;
import io.ddd4j.web.core.idempotency.WebIdempotencyLifecycle;
import io.ddd4j.javalin.web.Ddd4jJavalinWeb;
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

    public Ddd4jJavalinAutoConfiguration() {
        this(new Ddd4jJavalinProperties());
    }

    public Ddd4jJavalinAutoConfiguration(Ddd4jJavalinProperties properties) {
        this.properties = properties;
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
        return new PathWebAccessPolicy(
                Arrays.asList(properties.getPublicPaths()),
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
        String cacheName = properties.getIdempotencyCacheName();
        long ttlSeconds = Objects.requireNonNull(
                properties.getIdempotencyTtl(), "idempotencyTtl must not be null").getSeconds();
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("idempotencyTtl must be at least one second");
        }
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
     * Convenience overload that lets callers customise the public-path list without
     * subclassing the module.
     */
    public static Ddd4jJavalinAutoConfiguration withPublicPaths(String... paths) {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setPublicPaths(paths);
        return new Ddd4jJavalinAutoConfiguration(props);
    }

    /**
     * Convenience for tests: build a module with {@code port=0}.
     */
    public static Ddd4jJavalinAutoConfiguration forTesting() {
        Ddd4jJavalinProperties props = new Ddd4jJavalinProperties();
        props.setPort(0);
        props.setHost("127.0.0.1");
        return new Ddd4jJavalinAutoConfiguration(props);
    }

    /** Current properties. Visible for the bootstrap class. */
    public Ddd4jJavalinProperties getProperties() {
        return properties;
    }

    /** Static accessor for the convenience helpers above (e.g. {@link #withPublicPaths}). */
    public static List<String> defaultPublicPaths() {
        return Arrays.asList("/health", "/health/readiness", "/health/liveness");
    }
}
