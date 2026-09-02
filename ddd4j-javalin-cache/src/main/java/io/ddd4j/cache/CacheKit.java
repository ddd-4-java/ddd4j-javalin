package io.ddd4j.cache;

import io.ddd4j.cache.local.CaffeineCache;
import io.ddd4j.core.cache.*;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 统一缓存工具门面（所有缓存后端的唯一入口）。
 *
 * <p>无论是本地缓存（Caffeine/Guava/Hutool）还是远程缓存（Jedis/Lettuce/Redisson/Memcached/JetCache），
 * 都通过本门面统一注册和操作。业务代码面向 {@code CacheKit.get(biz, key)} 编程，
 * 不关心底层是哪种缓存引擎。
 *
 * <h3>两种注册方式</h3>
 * <ul>
 *   <li><b>{@code build()}</b> — 自建本地缓存（Caffeine/Guava/Hutool），门面内部根据配置自动创建实例</li>
 *   <li><b>{@code register()}</b> — 注册外部缓存实例（Jedis/Lettuce/Redisson/Memcached/JetCache 等），
 *       由调用方传入已创建的 {@link Cache} 对象</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 *   // ========== 1. 本地缓存（自建） ==========
 *   CacheKit.build("user", 300);
 *   CacheKit.put("user", "123", user);
 *   User u = CacheKit.get("user", "123");
 *
 *   // ========== 2. Redisson 远程缓存（注册） ==========
 *   RedissonCache<User> redissonCache = new RedissonCache<>(client, config, User.class);
 *   CacheKit.register("user", redissonCache);
 *   CacheKit.put("user", "123", user);     // 同样的 API
 *   User u = CacheKit.get("user", "123");
 *
 *   // ========== 3. JetCache 多级缓存（注册） ==========
 *   JetCacheCacheManager manager = new JetCacheCacheManager(jetCacheManager);
 *   Cache<String, User> multiCache = manager.getOrCreateCache("user", config);
 *   CacheKit.register("user", multiCache);
 *
 *   // ========== 4. 自动加载缓存 ==========
 *   CacheKit.buildWithLoader("config", key -> loadFromDB(key),
 *       config -> config.expireAfterWriteSeconds(60));
 *   String val = CacheKit.get("config", "app.name");
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@UtilityClass
@Slf4j
@SuppressWarnings("unchecked")
public final class CacheKit {

    /**
     * 所有缓存实例（biz → Cache），含本地和远程
     */
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    /**
     * 自动加载缓存实例（biz → Cache），与 caches 独立管理
     */
    private final Map<String, Cache<String, Object>> loadingCaches = new ConcurrentHashMap<>();
    /**
     * 默认本地缓存类型
     */
    @Getter
    private LocalCacheType defaultType = LocalCacheType.CAFFEINE;

    /**
     * 设置默认本地缓存类型（仅影响 {@link #build} 方法）。
     *
     * @param type 本地缓存类型
     */
    public void setDefaultType(LocalCacheType type) {
        CacheKit.defaultType = type;
        log.info("默认本地缓存类型已设置为: {}", type);
    }

    // ==================== 全局配置 ====================

    /**
     * 注册外部创建的缓存实例（Jedis/Lettuce/Redisson/Memcached/JetCache 等）。
     *
     * <p>注册后即可通过 {@link #get}/{@link #put}/{@link #invalidate} 统一操作，
     * 无需关心底层是哪种缓存引擎。
     *
     * <p>使用示例：
     * <pre>{@code
     *   // Redisson
     *   RedissonCache<User> cache = new RedissonCache<>(client, config, User.class);
     *   CacheKit.register("user", cache);
     *
     *   // JetCache 多级
     *   Cache<String, User> multiCache = jetCacheCacheManager.getOrCreateCache("user", config);
     *   CacheKit.register("user", multiCache);
     * }</pre>
     *
     * @param biz   业务标识
     * @param cache 缓存实例
     */
    @SuppressWarnings("unchecked")
    public void register(String biz, Cache<? super String, ?> cache) {
        Objects.requireNonNull(biz, "业务标识不能为空");
        Objects.requireNonNull(cache, "缓存实例不能为空");
        caches.put(biz, (Cache<String, Object>) cache);
        log.info("已注册缓存: biz={}, type={}", biz, cache.getClass().getSimpleName());
    }

    // ==================== 注册外部缓存实例 ====================

    /**
     * 注册自动加载缓存实例。
     *
     * @param biz   业务标识
     * @param cache 自动加载缓存实例
     */
    public void registerLoading(String biz, Cache<? super String, ?> cache) {
        Objects.requireNonNull(biz, "业务标识不能为空");
        Objects.requireNonNull(cache, "缓存实例不能为空");
        loadingCaches.put(biz, (Cache<String, Object>) cache);
        log.info("已注册自动加载缓存: biz={}, type={}", biz, cache.getClass().getSimpleName());
    }

    /**
     * 注销缓存实例。
     *
     * @param biz 业务标识
     */
    public void unregister(String biz) {
        caches.remove(biz);
        loadingCaches.remove(biz);
        log.info("已注销缓存: biz={}", biz);
    }

    /**
     * 构建本地缓存（默认类型，写后过期）。
     *
     * @param biz            业务标识
     * @param expiredSeconds 过期时间（秒）
     */
    public void build(String biz, long expiredSeconds) {
        build(biz, builder -> builder.expireAfterWriteSeconds(expiredSeconds), defaultType);
    }

    // ==================== 构建本地缓存 ====================

    /**
     * 构建本地缓存（Builder 模式配置，默认类型）。
     *
     * @param biz     业务标识
     * @param builder 配置构建器函数
     */
    public void build(String biz, Function<CacheConfig.Builder, CacheConfig.Builder> builder) {
        build(biz, builder, defaultType);
    }

    /**
     * 构建本地缓存（指定类型，Builder 模式配置）。
     *
     * @param biz     业务标识
     * @param builder 配置构建器函数
     * @param type    本地缓存类型
     */
    public void build(String biz, Function<CacheConfig.Builder, CacheConfig.Builder> builder, LocalCacheType type) {
        CacheConfig.Builder configBuilder = builder.apply(CacheConfig.builder(biz));
        CacheConfig config = configBuilder.cacheType(CacheType.LOCAL).build();
        Cache<String, Object> cache = createLocalCache(type, config);
        caches.put(biz, cache);
        log.info("已构建本地缓存: biz={}, type={}", biz, type);
    }

    /**
     * 构建自动加载缓存（Caffeine，支持自动刷新）。
     *
     * @param biz     业务标识
     * @param loader  缓存加载器
     * @param builder 配置构建器函数
     */
    public void buildWithLoader(String biz, Function<String, Object> loader,
                                Function<CacheConfig.Builder, CacheConfig.Builder> builder) {
        CacheConfig.Builder configBuilder = builder.apply(CacheConfig.builder(biz));
        CacheConfig config = configBuilder.cacheType(CacheType.LOCAL).build();
        Cache<String, Object> cache = CaffeineCache.createLoading(config, loader);
        loadingCaches.put(biz, cache);
        log.info("已构建自动加载缓存: biz={}", biz);
    }

    /**
     * 获取缓存值。
     *
     * <p>先查普通缓存（caches），未找到再查自动加载缓存（loadingCaches）。
     *
     * @param biz 业务标识
     * @param key 缓存键
     * @param <T> 值类型
     * @return 缓存值，不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String biz, String key) {
        Cache<String, Object> cache = caches.get(biz);
        if (Objects.nonNull(cache)) {
            return (T) cache.getIfPresent(key);
        }
        cache = loadingCaches.get(biz);
        if (Objects.nonNull(cache)) {
            try {
                return (T) cache.get(key);
            } catch (Exception e) {
                log.error("从自动加载缓存获取失败: biz={}, key={}", biz, key, e);
            }
        }
        return null;
    }

    // ==================== 缓存操作（统一入口） ====================

    /**
     * 获取缓存值（如果不存在则通过 mappingFunction 加载）。
     *
     * @param biz             业务标识
     * @param key             缓存键
     * @param mappingFunction 值加载函数
     * @param <T>             值类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String biz, String key, Function<String, Object> mappingFunction) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        return Objects.isNull(cache) ? null : (T) cache.get(key, mappingFunction);
    }

    /**
     * 设置缓存值。
     *
     * @param biz   业务标识
     * @param key   缓存键
     * @param value 缓存值
     */
    public void put(String biz, String key, Object value) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (Objects.nonNull(cache)) {
            cache.put(key, value);
        }
    }

    /**
     * 删除指定缓存项。
     *
     * @param biz 业务标识
     * @param key 缓存键
     */
    public void invalidate(String biz, String key) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (Objects.nonNull(cache)) {
            cache.invalidate(key);
        }
    }

    /**
     * 清空指定业务的所有缓存。
     *
     * @param biz 业务标识
     */
    public void invalidateAll(String biz) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (Objects.nonNull(cache)) {
            cache.invalidateAll();
        }
    }

    /**
     * 判断缓存是否存在。
     *
     * @param biz 业务标识
     * @param key 缓存键
     * @return true 表示存在
     */
    public boolean exists(String biz, String key) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        return Objects.nonNull(cache) && Objects.nonNull(cache.getIfPresent(key));
    }

    /**
     * 获取缓存统计信息。
     *
     * @param biz 业务标识
     * @return 统计信息，不支持时返回 null
     */
    public CacheStats getStats(String biz) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        return Objects.isNull(cache) ? null : cache.stats();
    }

    /**
     * 获取已注册的所有业务标识。
     *
     * @return 业务标识集合
     */
    public Set<String> getCacheNames() {
        Set<String> names = new java.util.HashSet<>();
        names.addAll(caches.keySet());
        names.addAll(loadingCaches.keySet());
        return names;
    }

    /**
     * 获取底层缓存实例（用于需要调用引擎特有 API 的场景，如 RedissonCache.tryLock）。
     *
     * @param biz 业务标识
     * @return 缓存实例，不存在返回 null
     */
    public Cache<String, Object> getCache(String biz) {
        return getEffectiveCache(biz);
    }

    /**
     * 刷新自动加载缓存。
     *
     * @param biz 业务标识
     * @param key 缓存键
     */
    public void refresh(String biz, String key) {
        Cache<String, Object> cache = loadingCaches.get(biz);
        if (Objects.isNull(cache)) {
            cache = caches.get(biz);
        }
        if (Objects.nonNull(cache)) {
            cache.refresh(key);
        }
    }

    // ==================== 自动加载缓存操作 ====================

    /**
     * 仅当 key 不存在时写入（原子操作）。
     *
     * <p>底层缓存需实现 {@link CasCache}，否则抛出 {@link UnsupportedOperationException}。
     *
     * @param biz   业务标识
     * @param key   缓存键
     * @param value 缓存值
     * @return true 表示写入成功（key 之前不存在）
     */
    public boolean putIfAbsent(String biz, String key, Object value) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof CasCache) {
            return ((CasCache<String, Object>) cache).putIfAbsent(key, value);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " (" + cache.getClass().getSimpleName() + ") 不支持 CAS putIfAbsent");
    }

    // ==================== CAS 操作（Compare-And-Swap） ====================

    /**
     * 仅当 key 当前值等于 expected 时，才替换为 newValue（原子操作）。
     *
     * @param biz      业务标识
     * @param key      缓存键
     * @param expected 期望的旧值
     * @param newValue 新值
     * @return true 表示替换成功
     */
    public boolean replace(String biz, String key, Object expected, Object newValue) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof CasCache) {
            return ((CasCache<String, Object>) cache).replace(key, expected, newValue);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持 CAS replace");
    }

    /**
     * 仅当 key 当前值等于 expected 时，才删除（原子操作）。
     *
     * @param biz      业务标识
     * @param key      缓存键
     * @param expected 期望的旧值
     * @return true 表示删除成功
     */
    public boolean removeIf(String biz, String key, Object expected) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof CasCache) {
            return ((CasCache<String, Object>) cache).removeIf(key, expected);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持 CAS removeIf");
    }

    /**
     * 原子递增。
     *
     * <p>底层缓存需实现 {@link AtomicCache}，否则抛出 {@link UnsupportedOperationException}。
     *
     * @param biz   业务标识
     * @param key   缓存键
     * @param delta 增量（>= 0）
     * @return 递增后的值
     */
    public long increment(String biz, String key, long delta) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof AtomicCache) {
            return ((AtomicCache<String, Object>) cache).increment(key, delta);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持原子计数");
    }

    // ==================== 原子计数操作 ====================

    /**
     * 原子递减。
     *
     * @param biz   业务标识
     * @param key   缓存键
     * @param delta 减量（>= 0）
     * @return 递减后的值
     */
    public long decrement(String biz, String key, long delta) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof AtomicCache) {
            return ((AtomicCache<String, Object>) cache).decrement(key, delta);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持原子计数");
    }

    /**
     * 库存原子扣减（带库存校验，不会扣到负数）。
     *
     * <p>返回值约定：{@code >= 0} 剩余库存；-1 售罄；-2 不足；-3 未初始化；-4 非法参数。
     *
     * @param biz      业务标识
     * @param key      缓存键
     * @param quantity 扣减数量（> 0）
     * @return 剩余库存（>= 0）或错误码（< 0）
     */
    public long stockDecrement(String biz, String key, long quantity) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof AtomicCache) {
            return ((AtomicCache<String, Object>) cache).stockDecrement(key, quantity);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持库存操作");
    }

    /**
     * 库存原子回补。
     *
     * @param biz      业务标识
     * @param key      缓存键
     * @param quantity 回补数量（> 0）
     * @return 回补后的库存（>= 0）或错误码（< 0）
     */
    public long stockIncrement(String biz, String key, long quantity) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof AtomicCache) {
            return ((AtomicCache<String, Object>) cache).stockIncrement(key, quantity);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持库存操作");
    }

    /**
     * 设置 key 的过期时间。
     *
     * @param biz     业务标识
     * @param key     缓存键
     * @param seconds 过期时间（秒）
     * @return true 表示设置成功
     */
    public boolean expire(String biz, String key, long seconds) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (Objects.nonNull(cache)) {
            return cache.expire(key, seconds);
        }
        return false;
    }

    // ==================== TTL 管理 ====================

    /**
     * 获取 key 的剩余过期时间。
     *
     * @param biz 业务标识
     * @param key 缓存键
     * @return 剩余秒数（-1 永久，-2 不存在，-3 不支持）
     */
    public long getExpire(String biz, String key) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        return Objects.isNull(cache) ? -3L : cache.getExpire(key);
    }

    /**
     * 移除 key 的过期时间，使其永久有效。
     *
     * @param biz 业务标识
     * @param key 缓存键
     * @return true 表示设置成功
     */
    public boolean persist(String biz, String key) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (Objects.nonNull(cache)) {
            return cache.persist(key);
        }
        return false;
    }

    /**
     * 尝试获取分布式锁。
     *
     * <p>底层缓存需实现 {@link CacheLock}（如 RedissonCache），否则抛出 {@link UnsupportedOperationException}。
     *
     * @param biz          业务标识
     * @param key          锁键
     * @param waitSeconds  最多等待时间（秒）
     * @param leaseSeconds 持有时间（秒）
     * @return true 表示获取成功
     */
    public boolean tryLock(String biz, String key, long waitSeconds, long leaseSeconds) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof CacheLock) {
            return ((CacheLock) cache).tryLock(key, waitSeconds, leaseSeconds);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " (" + (Objects.isNull(cache) ? "null" : cache.getClass().getSimpleName()) + ") 不支持分布式锁");
    }

    // ==================== 分布式锁操作 ====================

    /**
     * 释放锁。
     *
     * @param biz 业务标识
     * @param key 锁键
     */
    public void unlock(String biz, String key) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof CacheLock) {
            ((CacheLock) cache).unlock(key);
        }
    }

    /**
     * 获取锁并执行 Supplier，执行完毕自动释放。
     *
     * @param biz          业务标识
     * @param key          锁键
     * @param waitSeconds  等待时间（秒）
     * @param leaseSeconds 持有时间（秒）
     * @param supplier     业务逻辑
     * @param <T>          返回类型
     * @return supplier 返回值；获取锁失败返回 null
     */
    public <T> T withLock(String biz, String key, long waitSeconds, long leaseSeconds, java.util.function.Supplier<T> supplier) {
        Cache<String, Object> cache = getEffectiveCache(biz);
        if (cache instanceof CacheLock) {
            return ((CacheLock) cache).withLock(key, waitSeconds, leaseSeconds, supplier);
        }
        throw new UnsupportedOperationException("缓存 " + biz + " 不支持分布式锁");
    }

    /**
     * 获取生效的缓存实例（优先普通缓存，其次自动加载缓存）。
     */
    private Cache<String, Object> getEffectiveCache(String biz) {
        Cache<String, Object> cache = caches.get(biz);
        return Objects.nonNull(cache) ? cache : loadingCaches.get(biz);
    }

    // ==================== 内部方法 ====================

    /**
     * 创建本地缓存实例。
     *
     * <p>javalin 内联版仅携带 Caffeine 实现（GUAVA/HUTOOL 无消费方，未内联其实现类），
     * 枚举值保留以兼容 API，选择 GUAVA/HUTOOL 时抛出 {@link IllegalArgumentException}。
     */
    private Cache<String, Object> createLocalCache(LocalCacheType type, CacheConfig config) {
        switch (type) {
            case CAFFEINE:
                return CaffeineCache.create(config);
            case GUAVA:
            case HUTOOL:
            default:
                throw new IllegalArgumentException("javalin 内联版仅支持 CAFFEINE 本地缓存（当前: " + type + "）");
        }
    }

    /**
     * 本地缓存实现类型（仅 build() 方式使用）
     */
    public enum LocalCacheType {
        /**
         * Caffeine（默认，性能最优）
         */
        CAFFEINE,
        /**
         * Guava
         */
        GUAVA,
        /**
         * Hutool（轻量级）
         */
        HUTOOL
    }

}
