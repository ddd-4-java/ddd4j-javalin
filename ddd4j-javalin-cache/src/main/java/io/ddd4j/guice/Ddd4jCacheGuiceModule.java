package io.ddd4j.guice;

import com.google.inject.AbstractModule;
import io.ddd4j.cache.CacheKit;
import io.ddd4j.core.cache.Cache;
import io.ddd4j.core.cache.CacheConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ddd4j Cache 的 Guice 桥接模块。
 * <p>
 * 支持注册本地缓存（Caffeine 等）和外部缓存，通过 Guice 模块化方式集成。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j
public class Ddd4jCacheGuiceModule extends AbstractModule {

    /**
     * 本地缓存注册表（业务标识 → 过期秒数）
     */
    private final Map<String, Long> localCaches = new LinkedHashMap<>();
    /**
     * 本地缓存构建器注册表（业务标识 → 自定义构建器）
     */
    private final Map<String, Function<CacheConfig.Builder, CacheConfig.Builder>> localCacheBuilders =
            new LinkedHashMap<>();
    /**
     * 外部缓存注册表（业务标识 → 缓存实例）
     */
    private final Map<String, Cache<? super String, ?>> externalCaches = new LinkedHashMap<>();
    /**
     * 默认本地缓存类型
     */
    private CacheKit.LocalCacheType defaultType = CacheKit.LocalCacheType.CAFFEINE;

    /**
     * 设置默认本地缓存类型。
     *
     * @param defaultType 缓存类型（如 CAFFEINE）
     * @return 当前模块（链式调用）
     */
    public Ddd4jCacheGuiceModule setDefaultType(CacheKit.LocalCacheType defaultType) {
        this.defaultType = defaultType;
        return this;
    }

    /**
     * 注册本地缓存（指定过期时间）。
     *
     * @param biz            业务标识
     * @param expiredSeconds 过期秒数
     * @return 当前模块（链式调用）
     */
    public Ddd4jCacheGuiceModule build(String biz, long expiredSeconds) {
        this.localCaches.put(biz, expiredSeconds);
        return this;
    }

    /**
     * 注册本地缓存（自定义构建器）。
     *
     * @param biz     业务标识
     * @param builder 缓存配置构建器
     * @return 当前模块（链式调用）
     */
    public Ddd4jCacheGuiceModule build(String biz, Function<CacheConfig.Builder, CacheConfig.Builder> builder) {
        this.localCacheBuilders.put(biz, builder);
        return this;
    }

    /**
     * 注册外部缓存实例。
     *
     * @param biz   业务标识
     * @param cache 外部缓存实例
     * @return 当前模块（链式调用）
     */
    public Ddd4jCacheGuiceModule register(String biz, Cache<? super String, ?> cache) {
        this.externalCaches.put(biz, cache);
        return this;
    }

    @Override
    protected void configure() {
        CacheKit.setDefaultType(defaultType);

        for (Map.Entry<String, Long> entry : localCaches.entrySet()) {
            CacheKit.build(entry.getKey(), entry.getValue());
            log.debug("Built local cache: biz={}, expire={}s", entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Function<CacheConfig.Builder, CacheConfig.Builder>> entry :
                localCacheBuilders.entrySet()) {
            CacheKit.build(entry.getKey(), entry.getValue());
            log.debug("Built local cache with builder: biz={}", entry.getKey());
        }

        for (Map.Entry<String, Cache<? super String, ?>> entry : externalCaches.entrySet()) {
            CacheKit.register(entry.getKey(), entry.getValue());
            log.debug("Registered external cache: biz={}", entry.getKey());
        }

        log.info("Ddd4jCacheGuiceModule initialized: {} local caches, {} external caches",
                localCaches.size() + localCacheBuilders.size(), externalCaches.size());
    }
}
