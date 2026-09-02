package io.ddd4j.core.cache;

/**
 * CAS 乐观并发操作回调（借鉴 XMemcached {@code CASOperation<T>}）。
 *
 * <p>业务方实现此接口来声明"如何基于当前值计算新值"：
 * <pre>{@code
 * // 分布式计数器（所有后端通用）
 * cache.compareAndSet("counter", 60, current -> {
 *     Long v = current.value();
 *     return (v instanceof Long value ? value : 0L) + 1;
 * });
 * }</pre>
 *
 * <h3>跨后端兼容性</h3>
 * <p>回调只使用 {@link GetsResponse#value()}——不依赖 {@link GetsResponse#version()}。
 * 这让所有缓存后端都能实现 CAS：
 * <ul>
 *   <li><b>Caffeine/Guava</b>：用 {@code ConcurrentMap.compute(K, BiFunction)} 原子回调</li>
 *   <li><b>Jedis/Lettuce</b>：用 Lua 脚本 {@code if redis.call("get")==old then set end}</li>
 *   <li><b>Redisson</b>：用 {@code RBucket.compareAndSet(old, new)}</li>
 *   <li><b>Memcached</b>：用 {@code cas(key, exp, value, casVersion)} 原生版本号 CAS</li>
 * </ul>
 *
 * @param <R> 操作返回类型
 * @param <V> 缓存值类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.0.0
 */
@FunctionalInterface
public interface CASOperation<R, V> {

    /**
     * 基于当前值计算新值。
     *
     * @param current 当前值（{@link GetsResponse#value()} 可能为 null 表示首次写入）
     * @return 计算后的新值；返回 null 表示放弃 CAS（引擎不会重试）
     * @throws Exception 业务方可抛出异常表示放弃 CAS
     */
    R apply(GetsResponse<V> current) throws Exception;

    /**
     * CAS 最大重试次数——借鉴 Memcached {@code CASOperation.getMaxTries()}。
     *
     * <p>默认返回 -1（使用引擎默认 16 次）。
     * 业务方可重写此方法控制重试策略：
     * <ul>
     *   <li>1 = 只尝试一次，不重试</li>
     *   <li>-1 = 使用引擎默认（16 次）</li>
     *   <li>N = 最多重试 N 次</li>
     * </ul>
     */
    default int maxRetries() {
        return -1;
    }
}
