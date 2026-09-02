package io.ddd4j.core.cache;

/**
 * 原子计数缓存接口（纯 Java SPI，零框架依赖）。
 *
 * <p>扩展 {@link Cache}，提供原子性的数值增减操作，支持限流、计数、库存管理等场景。
 * 支持此接口的缓存实现（Caffeine/Jedis/Redisson 等）可提供原子计数语义，
 * 不支持的实现抛出 {@link UnsupportedOperationException}。
 *
 * <h3>典型场景</h3>
 * <ul>
 *   <li><b>限流</b> — {@code increment(key, 1)} 后判断是否超过阈值</li>
 *   <li><b>计数</b> — 页面浏览量、点赞数等</li>
 *   <li><b>库存</b> — {@link #stockDecrement} 提供带库存校验的原子扣减</li>
 * </ul>
 *
 * <h3>库存操作返回值约定</h3>
 * <ul>
 *   <li>{@code >= 0} — 操作成功，返回操作后的剩余值</li>
 *   <li>{@code -1} — 库存为零（已售罄）</li>
 *   <li>{@code -2} — 库存不足（请求扣减量 > 剩余量）</li>
 *   <li>{@code -3} — 库存未初始化（key 不存在）</li>
 *   <li>{@code -4} — 参数非法（扣减量为负数或零）</li>
 * </ul>
 *
 * @param <K> 缓存键类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface AtomicCache<K, V> extends Cache<K, V> {

    /**
     * 库存返回值：库存为零（已售罄）
     */
    long STOCK_ZERO = -1L;
    /**
     * 库存返回值：库存不足
     */
    long STOCK_NOT_ENOUGH = -2L;
    /**
     * 库存返回值：库存未初始化（key 不存在）
     */
    long STOCK_NOT_INITIALIZED = -3L;
    /**
     * 库存返回值：参数非法
     */
    long STOCK_ILLEGAL_ARG = -4L;

    /**
     * 原子递增（整数）。
     *
     * @param key   缓存键
     * @param delta 增量（必须 >= 0）
     * @return 递增后的值
     */
    long increment(K key, long delta);

    /**
     * 原子递增（整数），并设置过期时间。
     *
     * @param key     缓存键
     * @param delta   增量（必须 >= 0）
     * @param seconds 过期时间（秒），仅对首次创建有效
     * @return 递增后的值
     */
    default long increment(K key, long delta, long seconds) {
        long result = increment(key, delta);
        if (seconds > 0) {
            expire(key, seconds);
        }
        return result;
    }

    /**
     * 原子递减（整数）。
     *
     * @param key   缓存键
     * @param delta 减量（必须 >= 0）
     * @return 递减后的值（可为负数）
     */
    long decrement(K key, long delta);

    /**
     * 原子递增（浮点数）。
     *
     * @param key   缓存键
     * @param delta 增量（必须 >= 0）
     * @return 递增后的值
     */
    double incrementFloat(K key, double delta);

    /**
     * 原子递减（浮点数）。
     *
     * @param key   缓存键
     * @param delta 减量（必须 >= 0）
     * @return 递减后的值
     */
    double decrementFloat(K key, double delta);

    /**
     * 库存原子扣减（带库存校验）。
     *
     * <p>仅当库存充足时才扣减，返回扣减后的剩余库存。
     * 不会扣到负数，保证库存安全。
     *
     * @param key      缓存键
     * @param quantity 扣减数量（必须 > 0）
     * @return {@code >= 0} 剩余库存；负数表示失败（见返回值约定）
     */
    long stockDecrement(K key, long quantity);

    /**
     * 库存原子回补。
     *
     * @param key      缓存键
     * @param quantity 回补数量（必须 > 0）
     * @return {@code >= 0} 回补后的库存；负数表示失败
     */
    long stockIncrement(K key, long quantity);

}
