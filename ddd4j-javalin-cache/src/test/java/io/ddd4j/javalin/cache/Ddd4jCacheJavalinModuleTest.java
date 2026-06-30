package io.ddd4j.javalin.cache;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.cache.local.CaffeineCache;
import io.ddd4j.core.cache.Cache;
import io.ddd4j.core.cache.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ddd4j-javalin-cache Guice 桥接集成测试。
 *
 * <p>验证：声明式 Module 配置后，Injector 创建时统一初始化缓存，
 * CacheKit.put/get 闭环可用。这是"cache 适配从占位变为可用"的端到端证据。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jCacheJavalinModuleTest {

    @BeforeEach
    void cleanUp() {
        // 清理可能残留的缓存注册（CacheKit 是静态门面，测试间共享状态）
        CacheKit.unregister("user");
        CacheKit.unregister("external");
        CacheKit.unregister("builder");
    }

    /**
     * 验证声明式预构建本地缓存后，CacheKit.put/get 闭环可用。
     */
    @Test
    void shouldBuildLocalCacheAndSupportPutGet() {
        // 声明式配置：预构建 user 本地缓存，300s 过期
        Ddd4jCacheJavalinModule module = new Ddd4jCacheJavalinModule()
                .build("user", 300);
        Injector injector = Guice.createInjector(module);

        // Injector 创建后缓存已就绪：put/get 闭环
        CacheKit.put("user", "123", "Alice");
        String value = CacheKit.get("user", "123");

        assertEquals("Alice", value, "CacheKit.put/get 闭环应正确存取值");
    }

    /**
     * 验证 Builder 模式声明式预构建本地缓存。
     */
    @Test
    void shouldBuildLocalCacheWithBuilderConfig() {
        Ddd4jCacheJavalinModule module = new Ddd4jCacheJavalinModule()
                .build("builder", b -> b.expireAfterWriteSeconds(60));
        Guice.createInjector(module);

        CacheKit.put("builder", "key1", "value1");
        assertEquals("value1", CacheKit.get("builder", "key1"),
                "Builder 模式预构建的缓存应支持 put/get");
    }

    /**
     * 验证声明式注册外部缓存实例后，CacheKit 通过统一 API 访问。
     */
    @Test
    void shouldRegisterExternalCacheAndSupportUnifiedAccess() {
        // 创建一个外部缓存实例（用 Caffeine 模拟，实际场景是 Redisson/Jedis）
        CacheConfig config = CacheConfig.builder("external")
                .cacheType(io.ddd4j.core.cache.CacheType.LOCAL)
                .expireAfterWriteSeconds(120)
                .build();
        Cache<String, Object> externalCache = CaffeineCache.create(config);

        // 声明式注册外部缓存
        Ddd4jCacheJavalinModule module = new Ddd4jCacheJavalinModule()
                .register("external", externalCache);
        Guice.createInjector(module);

        // 通过 CacheKit 统一 API 访问注册的外部缓存
        CacheKit.put("external", "token-abc", "session-data");
        assertEquals("session-data", CacheKit.get("external", "token-abc"),
                "注册的外部缓存应通过 CacheKit 统一 API 访问");
    }
}
