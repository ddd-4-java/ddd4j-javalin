package io.ddd4j.core.cache;

/**
 * 缓存类型枚举（纯 Java，零框架依赖）。
 *
 * <p>定义缓存的作用范围，用于 {@link CacheConfig} 和 {@link CacheManager} 创建缓存时指定类型：
 * <ul>
 *   <li>{@link #LOCAL} — 本地缓存（进程内，如 Caffeine/Guava/Hutool）</li>
 *   <li>{@link #REMOTE} — 远程缓存（分布式，如 Redis/Redisson/Memcached）</li>
 *   <li>{@link #BOTH} — 多级缓存（本地 + 远程，两级联动）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public enum CacheType {

    /**
     * 本地缓存（进程内嵌入式缓存）。
     *
     * <p>典型实现：Caffeine、Guava Cache、Hutool TimedCache。
     * <p>特点：低延迟、无网络开销，但无法跨实例共享。
     */
    LOCAL,

    /**
     * 远程缓存（分布式缓存）。
     *
     * <p>典型实现：Redis（Lettuce/Jedis）、Redisson、Memcached。
     * <p>特点：跨实例共享、数据一致性好，但有网络延迟。
     */
    REMOTE,

    /**
     * 多级缓存（本地 + 远程两级联动）。
     *
     * <p>读请求先查本地缓存，未命中再查远程缓存；远程命中后回填本地。
     * <p>写请求同时写入本地和远程（或仅远程 + 广播失效）。
     * <p>特点：兼顾低延迟和一致性，适合高并发读场景。
     */
    BOTH

}
