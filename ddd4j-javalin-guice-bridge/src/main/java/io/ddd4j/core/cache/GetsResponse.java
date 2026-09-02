package io.ddd4j.core.cache;

/**
 * CAS 操作读取的"当前值 + 版本号"响应。
 *
 * <p>对应 Memcached 协议中的 {@code gets} 命令响应。
 *
 * <h3>版本号语义</h3>
 * <ul>
 *   <li><b>Memcached</b>：{@link #version()} 返回原生 cas 版本号（真实乐观锁 token）</li>
 *   <li><b>Redis（Jedis/Lettuce/Redisson）</b>：{@link #version()} 返回 -1（Redis 无版本号概念，CAS 基于"值比较"而非"版本号比较"）</li>
 *   <li><b>本地缓存（Caffeine/Guava）</b>：{@link #version()} 返回 -1（同 Redis，基于值比较）</li>
 * </ul>
 *
 * <p>因此 {@link CASOperation} 回调**不依赖 version**——只用 {@link #value()} 做计算。
 * 这样所有后端（Caffeine/Redis/Memcached）都能实现 CAS。
 *
 * @param <V> 缓存值类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.0.0
 */
public interface GetsResponse<V> {

    /**
     * 缓存 key。
     */
    String key();

    /**
     * 缓存当前值。
     * <p>首次写入（key 不存在）时为 {@code null}。
     */
    V value();

    /**
     * 缓存当前版本号（乐观并发控制 token）。
     * <p>仅在 Memcached 后端有意义（返回原生 cas 版本号）。
     * <p>其他后端（Redis/Caffeine）返回 -1，表示"无版本号，CAS 基于值比较"。
     * <p>业务方<strong>不应</strong>依赖此值做判断（除非明确知道后端是 Memcached）。
     *
     * @return cas 版本号（-1 表示后端不支持版本号）
     */
    default long version() {
        return -1L;
    }
}