package io.ddd4j.core.cache;

import java.util.Map;
import java.util.function.Function;

/**
 * 缓存统一接口（纯 Java，零框架依赖）。
 *
 * <p>提供统一的缓存操作抽象，支持多种缓存后端实现（Caffeine、Guava、Hutool、Redis、Redisson 等）。
 * 接口设计与 Caffeine 保持一致，方便业务开发中可以改变实现。
 *
 * <p>各框架适配层（ddd4j-cache-core 等）提供具体实现：
 * <ul>
 *   <li>{@code JetCacheAdapter} — 基于 JetCache 统一引擎的适配</li>
 *   <li>{@code CaffeineCache} / {@code GuavaCache} / {@code HutoolCache} — 本地缓存实现</li>
 *   <li>{@code RedisCache} / {@code RedissonCache} — 分布式缓存实现</li>
 * </ul>
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #getIfPresent(Object)} - 获取缓存值，如果不存在返回 null</li>
 *   <li>{@link #get(Object)} - 获取缓存值（自动加载缓存专用）</li>
 *   <li>{@link #get(Object, Function)} - 获取缓存值，如果不存在则通过函数加载</li>
 *   <li>{@link #put(Object, Object)} - 设置缓存值</li>
 *   <li>{@link #invalidate(Object)} - 删除缓存</li>
 *   <li>{@link #refresh(Object)} - 刷新缓存（自动加载缓存专用）</li>
 *   <li>{@link #invalidateAll()} - 清空所有缓存</li>
 *   <li>{@link #estimatedSize()} - 获取缓存大小</li>
 *   <li>{@link #stats()} - 获取统计信息</li>
 * </ul>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface Cache<K, V> {

    /**
     * 获取缓存值，如果不存在返回 null。
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在返回 null
     */
    V getIfPresent(K key);

    /**
     * 获取缓存值（自动加载缓存专用）。
     *
     * <p>如果缓存中不存在指定键的值，则通过 CacheLoader 自动加载。
     * 普通缓存实现此方法时应返回 {@link #getIfPresent(Object)} 的结果，
     * 自动加载缓存实现时应调用 CacheLoader 加载值。
     *
     * @param key 缓存键
     * @return 缓存值
     */
    default V get(K key) {
        return getIfPresent(key);
    }

    /**
     * 获取缓存值，如果不存在则通过函数加载。
     *
     * <p>如果缓存中不存在指定键的值，则调用 mappingFunction 计算值，
     * 并将计算结果存入缓存后返回。
     *
     * @param key             缓存键
     * @param mappingFunction 值加载函数
     * @return 缓存值
     */
    V get(K key, Function<K, V> mappingFunction);

    /**
     * 设置缓存值。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 批量设置缓存值。
     *
     * @param map 缓存键值对
     */
    default void putAll(Map<K, V> map) {
        map.forEach(this::put);
    }

    /**
     * 删除缓存。
     *
     * @param key 缓存键
     */
    void invalidate(K key);

    /**
     * 刷新缓存（自动加载缓存专用）。
     *
     * <p>刷新指定键的缓存值，下次访问时会重新加载。
     * 普通缓存实现此方法时应调用 {@link #invalidate(Object)}。
     *
     * @param key 缓存键
     */
    default void refresh(K key) {
        invalidate(key);
    }

    /**
     * 批量删除缓存。
     *
     * @param keys 缓存键集合
     */
    default void invalidateAll(Iterable<K> keys) {
        keys.forEach(this::invalidate);
    }

    /**
     * 清空所有缓存。
     */
    void invalidateAll();

    /**
     * 获取缓存大小（估算值）。
     *
     * @return 缓存条目数
     */
    long estimatedSize();

    /**
     * 获取缓存统计信息。
     *
     * @return 缓存统计信息，如果不支持返回 null
     */
    CacheStats stats();

    // ==================== TTL 管理（default 方法，不支持时抛 UnsupportedOperationException） ====================

    /**
     * 设置 key 的过期时间。
     *
     * <p>仅远程缓存（Redis/Memcached）和部分本地缓存（Caffeine）支持。
     *
     * @param key     缓存键
     * @param seconds 过期时间（秒）
     * @return true 表示设置成功
     */
    default boolean expire(K key, long seconds) {
        throw new UnsupportedOperationException("此缓存实现不支持 expire");
    }

    /**
     * 获取 key 的剩余过期时间。
     *
     * @param key 缓存键
     * @return 剩余秒数（-1 表示永久有效，-2 表示 key 不存在，-3 表示不支持）
     */
    default long getExpire(K key) {
        return -3L;
    }

    /**
     * 移除 key 的过期时间，使其永久有效。
     *
     * @param key 缓存键
     * @return true 表示设置成功
     */
    default boolean persist(K key) {
        throw new UnsupportedOperationException("此缓存实现不支持 persist");
    }

    // ==================== CAS 乐观并发控制（借鉴 memcached CAS 设计）====================

    /**
     * 操作式 CAS（自动重试）——借鉴 Memcached {@code cas(key, exp, CASOperation)}。
     *
     * <p>业务方传一个 {@link CASOperation} 描述"如何基于当前值计算新值"，
     * CAS 引擎在并发冲突时自动用最新的当前值重新调用回调（最多 {@link CASOperation#maxRetries()} 次）。
     *
     * <h3>各后端的底层 CAS 原语</h3>
     * <ul>
     *   <li><b>Caffeine</b>：{@code asMap().compute(K, BiFunction)} — JVM 内原子回调，天然无冲突</li>
     *   <li><b>Guava</b>：{@code asMap().replace(K, oldV, newV)} + 重试</li>
     *   <li><b>Jedis/Lettuce</b>：Lua 脚本 {@code if get==old then set end} — Redis 单线程原子</li>
     *   <li><b>Redisson</b>：{@code RBucket.compareAndSet(old, new)} + 重试</li>
     *   <li><b>Memcached</b>：原生 {@code cas(key, exp, value, casVersion)} + 重试</li>
     *   <li><b>HutoolCache</b>：不支持（抛 {@link UnsupportedOperationException}）</li>
     * </ul>
     *
     * <p>典型用法（分布式计数器）：
     * <pre>{@code
     * cache.compareAndSet("counter", 60, current -> {
     *     return (current.value() instanceof Long value ? value : 0L) + 1;
     * });
     * }</pre>
     *
     * @param key           缓存键
     * @param expireSeconds 过期时间（秒），0 表示不修改 TTL
     * @param operation     CAS 操作回调（基于当前值计算新值）
     * @return 新写入的值（如果所有重试都失败或回调返回 null 则返回 null）
     */
    default V compareAndSet(K key, long expireSeconds, CASOperation<V, V> operation) {
        throw new UnsupportedOperationException("此缓存实现不支持 CAS");
    }

    /**
     * 值比较 CAS（单次，不重试）——借鉴 Memcached {@code cas(key, exp, value, cas)}。
     *
     * <p>仅当缓存中的当前值 equals {@code expectedOldValue} 时才写入 {@code newValue}。
     *
     * <h3>各后端的底层实现</h3>
     * <ul>
     *   <li><b>Caffeine/Guava</b>：{@code asMap().replace(K, oldV, newV)}</li>
     *   <li><b>Jedis/Lettuce</b>：Lua 脚本 {@code if redis.call("get")==old then set end}</li>
     *   <li><b>Redisson</b>：{@code RBucket.compareAndSet(oldJson, newJson)}</li>
     *   <li><b>Memcached</b>：{@code gets} + 值比较 + {@code cas}</li>
     * </ul>
     *
     * @param key              缓存键
     * @param expectedOldValue 期望的旧值（null 表示仅当 key 不存在时写入 = putIfAbsent）
     * @param newValue         新值
     * @return true=写入成功，false=值不匹配
     */
    default boolean compareAndSet(K key, V expectedOldValue, V newValue) {
        throw new UnsupportedOperationException("此缓存实现不支持值比较 CAS");
    }

    /**
     * 异步 CAS（noReply 语义）——借鉴 Memcached {@code casWithNoReply}：不等待写入结果。
     *
     * <p>默认实现为同步委托。支持异步通道的后端（如 Lettuce/Redisson 异步 API）可重写此方法。
     *
     * @param key           缓存键
     * @param expireSeconds 过期时间（秒）
     * @param operation     CAS 操作回调
     */
    default void asyncCompareAndSet(K key, long expireSeconds, CASOperation<V, V> operation) {
        compareAndSet(key, expireSeconds, operation);
    }

    /**
     * 版本号 CAS（单次，不重试）——借鉴 Memcached {@code cas(key, exp, value, cas)}。
     *
     * <p><b>注意</b>：此方法仅在 Memcached 后端有真实版本号语义（返回原生 cas 版本号）。
     * 其他后端（Redis/Caffeine 等）没有原生版本号概念，抛 {@link UnsupportedOperationException}。
     *
     * <p>推荐优先使用 {@link #compareAndSet(K, long, CASOperation)} 或
     * {@link #compareAndSet(K, V, V)}——它们在所有后端上都有严谨实现。
     *
     * @param key             缓存键
     * @param newValue        新值
     * @param expectedVersion 期望的版本号（来自 Memcached {@code gets} 返回的 cas 值）
     * @return true=版本号匹配并写入成功，false=版本号不匹配
     */
    default boolean compareAndSet(K key, V newValue, long expectedVersion) {
        throw new UnsupportedOperationException("此缓存实现不支持版本号 CAS");
    }

}
