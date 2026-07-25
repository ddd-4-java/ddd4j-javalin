package io.ddd4j.javalin.cache;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.cache.local.CaffeineCache;
import io.ddd4j.core.cache.Cache;
import io.ddd4j.core.cache.CacheConfig;
import io.ddd4j.guice.Ddd4jCacheGuiceModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ddd4j-javalin-cache Guice integration test.
 *
 * <p>Validates that the ddd4j Guice Cache bridge ({@link Ddd4jCacheGuiceModule})
 * can be installed and provides the {@link CacheKit} static API for runtime cache
 * registration. The legacy {@code Ddd4jCacheJavalinModule} POJO has been
 * deprecated in favour of the core {@link Ddd4jCacheGuiceModule}.
 */
class Ddd4jCacheJavalinModuleTest {

    @BeforeEach
    void cleanUp() {
        // Clean up any state cached between tests.
        CacheKit.unregister("user");
        CacheKit.unregister("external");
        CacheKit.unregister("builder");
    }

    @Test
    void shouldRegisterExternalCacheViaGuiceModule() {
        CacheConfig config = CacheConfig.builder("external")
                .cacheType(io.ddd4j.core.cache.CacheType.LOCAL)
                .expireAfterWriteSeconds(120)
                .build();
        Cache<String, Object> externalCache = CaffeineCache.create(config);

        Ddd4jCacheGuiceModule module = new Ddd4jCacheGuiceModule();
        Injector injector = Guice.createInjector(module);

        assertNotNull(injector);
        CacheKit.register("external", externalCache);
        CacheKit.put("external", "token-abc", "session-data");
        assertEquals("session-data", CacheKit.get("external", "token-abc"));
    }

    @Test
    void shouldRegisterLocalCacheViaCacheKit() {
        // Direct usage of CacheKit without Guice (the simplest integration path).
        CacheConfig config = CacheConfig.builder("user")
                .cacheType(io.ddd4j.core.cache.CacheType.LOCAL)
                .expireAfterWriteSeconds(300)
                .build();
        CacheKit.register("user", CaffeineCache.create(config));
        CacheKit.put("user", "123", "Alice");
        assertEquals("Alice", CacheKit.get("user", "123"));
    }
}