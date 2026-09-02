package io.ddd4j.core.cache;

/**
 * 缓存统计信息接口（纯 Java，零框架依赖）。
 *
 * <p>与 Caffeine CacheStats 接口兼容，提供缓存命中率、加载次数等统计信息。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface CacheStats {

    /**
     * 获取缓存命中次数。
     *
     * @return 命中次数
     */
    long hitCount();

    /**
     * 获取缓存未命中次数。
     *
     * @return 未命中次数
     */
    long missCount();

    /**
     * 获取缓存命中率。
     *
     * @return 命中率（0.0 - 1.0）
     */
    double hitRate();

    /**
     * 获取缓存加载次数。
     *
     * @return 加载次数
     */
    long loadCount();

    /**
     * 获取缓存淘汰次数。
     *
     * @return 淘汰次数
     */
    long evictionCount();

}
