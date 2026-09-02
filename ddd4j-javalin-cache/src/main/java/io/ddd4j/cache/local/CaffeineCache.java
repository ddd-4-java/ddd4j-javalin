package io.ddd4j.cache.local;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.ddd4j.core.cache.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Caffeine 本地缓存实现。
 *
 * <p>基于 <a href="https://github.com/ben-manes/caffeine">Caffeine</a> 高性能本地缓存，
 * 支持：写后过期、访问后过期、写后刷新、最大容量、初始容量、统计信息、移除监听器、自动加载。
 *
 * <p>从 ddd4j-kit/cache/impl/CaffeineCache 迁移，改包名并适配新的 {@link CacheConfig}。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public class CaffeineCache<K, V> implements CasCache<K, V>, AtomicCache<K, V> {

    private final com.github.benmanes.caffeine.cache.Cache<K, V> cache;

    private CaffeineCache(com.github.benmanes.caffeine.cache.Cache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    /**
     * 根据 {@link CacheConfig} 创建 Caffeine 缓存实例。
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return CaffeineCache 实例
     */
    public static <K, V> CaffeineCache<K, V> create(CacheConfig config) {
        return new CaffeineCache<>(buildCaffeine(config).build());
    }

    /**
     * 创建自动加载缓存实例。
     *
     * @param config 缓存配置
     * @param loader 缓存加载器
     * @param <K>    键类型
     * @param <V>    值类型
     * @return CaffeineCache 实例（支持自动加载）
     */
    public static <K, V> CaffeineCache<K, V> createLoading(CacheConfig config, Function<K, V> loader) {
        Caffeine<Object, Object> builder = buildCaffeine(config);
        LoadingCache<K, V> loadingCache = builder.build(loader::apply);
        return new CaffeineCache<>(loadingCache);
    }

    private static Caffeine<Object, Object> buildCaffeine(CacheConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (config.getMaximumSize() > 0) {
            builder.maximumSize(config.getMaximumSize());
        }
        if (config.getExpireAfterWriteSeconds() > 0) {
            builder.expireAfterWrite(config.getExpireAfterWriteSeconds(), TimeUnit.SECONDS);
        }
        if (config.getExpireAfterAccessSeconds() > 0) {
            builder.expireAfterAccess(config.getExpireAfterAccessSeconds(), TimeUnit.SECONDS);
        }
        if (config.getRefreshAfterWriteSeconds() > 0) {
            builder.refreshAfterWrite(config.getRefreshAfterWriteSeconds(), TimeUnit.SECONDS);
        }
        if (config.getInitialCapacity() > 0) {
            builder.initialCapacity(config.getInitialCapacity());
        }
        if (config.isRecordStats()) {
            builder.recordStats();
        }
        Consumer<String> removalListener = config.getRemovalListener();
        if (Objects.nonNull(removalListener)) {
            builder.removalListener((key, value, cause) -> removalListener.accept(String.valueOf(key)));
        }
        return builder;
    }

    private static long toLong(Object val) {
        if (Objects.isNull(val)) {
            return 0L;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return Long.parseLong(val.toString());
    }

    private static double toDouble(Object val) {
        if (Objects.isNull(val)) {
            return 0.0;
        }
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return Double.parseDouble(val.toString());
    }

    @Override
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public V get(K key, Function<K, V> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

    @Override
    public V get(K key) {
        // 自动加载缓存（LoadingCache）时自动加载；否则等价于 getIfPresent
        if (cache instanceof LoadingCache) {
            return ((LoadingCache<K, V>) cache).get(key);
        }
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void putAll(Map<K, V> map) {
        cache.putAll(map);
    }

    @Override
    public void invalidate(K key) {
        cache.invalidate(key);
    }

    @Override
    public void refresh(K key) {
        if (cache instanceof LoadingCache) {
            ((LoadingCache<K, V>) cache).refresh(key);
        } else {
            cache.invalidate(key);
        }
    }

    @Override
    public void invalidateAll(Iterable<K> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public long estimatedSize() {
        return cache.estimatedSize();
    }

    // ==================== CasCache 实现（基于 ConcurrentMap 原子操作） ====================

    @Override
    public CacheStats stats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
        return new CacheStats() {
            @Override
            public long hitCount() {
                return caffeineStats.hitCount();
            }

            @Override
            public long missCount() {
                return caffeineStats.missCount();
            }

            @Override
            public double hitRate() {
                return caffeineStats.hitRate();
            }

            @Override
            public long loadCount() {
                return caffeineStats.loadCount();
            }

            @Override
            public long evictionCount() {
                return caffeineStats.evictionCount();
            }
        };
    }

    /**
     * 获取底层 Caffeine 缓存实例。
     *
     * @return Caffeine Cache 实例
     */
    public com.github.benmanes.caffeine.cache.Cache<K, V> unwrap() {
        return cache;
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return Objects.isNull(cache.asMap().putIfAbsent(key, value));
    }

    // ==================== AtomicCache 实现（基于 ConcurrentMap.merge 原子操作） ====================

    @Override
    public boolean replace(K key, V expected, V newValue) {
        if (Objects.isNull(expected)) {
            // 期望 key 不存在时才写入
            return Objects.isNull(cache.asMap().putIfAbsent(key, newValue));
        }
        return cache.asMap().replace(key, expected, newValue);
    }

    @Override
    public boolean removeIf(K key, V expected) {
        return cache.asMap().remove(key, expected);
    }

    @Override
    @SuppressWarnings("unchecked")
    public long increment(K key, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("增量必须 >= 0");
        }
        Object newVal = cache.asMap().merge(key, (V) Long.valueOf(delta),
                (oldVal, inc) -> (V) Long.valueOf(toLong(oldVal) + delta));
        return toLong(newVal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public long decrement(K key, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("减量必须 >= 0");
        }
        Object newVal = cache.asMap().merge(key, (V) Long.valueOf(-delta),
                (oldVal, dec) -> (V) Long.valueOf(toLong(oldVal) - delta));
        return toLong(newVal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public double incrementFloat(K key, double delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("增量必须 >= 0");
        }
        Object newVal = cache.asMap().merge(key, (V) Double.valueOf(delta),
                (oldVal, inc) -> (V) Double.valueOf(toDouble(oldVal) + delta));
        return toDouble(newVal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public double decrementFloat(K key, double delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("减量必须 >= 0");
        }
        Object newVal = cache.asMap().merge(key, (V) Double.valueOf(-delta),
                (oldVal, dec) -> (V) Double.valueOf(toDouble(oldVal) - delta));
        return toDouble(newVal);
    }

    // ==================== TTL 管理 ====================

    @Override
    public long stockDecrement(K key, long quantity) {
        if (quantity <= 0) {
            return AtomicCache.STOCK_ILLEGAL_ARG;
        }
        // 原子检查并扣减：使用 compute 保证一致性
        Object[] holder = new Object[1];
        cache.asMap().compute(key, (k, val) -> {
            if (Objects.isNull(val)) {
                holder[0] = AtomicCache.STOCK_NOT_INITIALIZED;
                return null; // 不创建
            }
            long stock = toLong(val);
            if (stock <= 0) {
                holder[0] = AtomicCache.STOCK_ZERO;
                return val; // 保持不变
            }
            if (stock < quantity) {
                holder[0] = AtomicCache.STOCK_NOT_ENOUGH;
                return val; // 保持不变
            }
            long remaining = stock - quantity;
            holder[0] = remaining;
            @SuppressWarnings("unchecked") V result = (V) Long.valueOf(remaining);
            return result;
        });
        return (Long) holder[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public long stockIncrement(K key, long quantity) {
        if (quantity <= 0) {
            return AtomicCache.STOCK_ILLEGAL_ARG;
        }
        Object val = cache.asMap().merge(key, (V) Long.valueOf(quantity),
                (oldVal, inc) -> (V) Long.valueOf(toLong(oldVal) + quantity));
        return toLong(val);
    }

    @Override
    public boolean expire(K key, long seconds) {
        // Caffeine 不支持单 key 动态设置过期时间，需通过 expireAfter 策略在构建时配置
        // 此处通过先删再写的方式近似实现（会丢失原值，不推荐）
        return false;
    }

    // ==================== 内部工具 ====================

    @Override
    public long getExpire(K key) {
        // Caffeine 不暴露单 key 的剩余过期时间
        return -3L;
    }

    @Override
    public boolean persist(K key) {
        return false;
    }

    // ==================== CAS SPI 实现（基于 asMap().compute 原子回调）====================

    @Override
    public V compareAndSet(K key, long expireSeconds, CASOperation<V, V> operation) {
        // Caffeine 的 asMap().compute 是 JVM 内原子回调，天然等价于 CASOperation
        return cache.asMap().compute(key, (k, current) -> {
            io.ddd4j.core.cache.GetsResponse<V> resp = new io.ddd4j.core.cache.GetsResponse<>() {
                @Override
                public String key() {
                    return String.valueOf(k);
                }

                @Override
                public V value() {
                    return current;
                }
            };
            try {
                V newValue = operation.apply(resp);
                // 回调返回 null 表示删除 key
                return newValue;
            } catch (Exception e) {
                // 异常：保持原值不变
                return current;
            }
        });
    }

    @Override
    public boolean compareAndSet(K key, V expectedOldValue, V newValue) {
        if (Objects.isNull(expectedOldValue)) {
            // null expected = putIfAbsent
            return Objects.isNull(cache.asMap().putIfAbsent(key, newValue));
        }
        return cache.asMap().replace(key, expectedOldValue, newValue);
    }

}
