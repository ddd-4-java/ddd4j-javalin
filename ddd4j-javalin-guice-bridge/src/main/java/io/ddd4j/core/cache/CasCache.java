package io.ddd4j.core.cache;

/**
 * CAS（Compare-And-Swap）缓存接口（纯 Java SPI，零框架依赖）。
 *
 * <p>扩展 {@link Cache}，提供原子性的条件写入/删除操作。
 * 支持此接口的缓存实现（Caffeine/Redis/JetCache 等）可提供 CAS 语义，
 * 不支持的实现抛出 {@link UnsupportedOperationException}。
 *
 * <p>典型场景：
 * <ul>
 *   <li>防重复提交 — {@code putIfAbsent("req:xxx", 1)} 返回 false 表示已存在</li>
 *   <li>乐观锁更新 — {@code replace(key, oldVal, newVal)} 仅当值匹配时才更新</li>
 *   <li>条件删除 — {@code removeIf(key, expectedVal)} 仅当值匹配时才删除</li>
 * </ul>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface CasCache<K, V> extends Cache<K, V> {

    /**
     * 仅当 key 不存在时写入（原子操作）。
     *
     * <p>等价于 Redis {@code SETNX} / Java {@code ConcurrentHashMap.putIfAbsent}。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return true 表示写入成功（key 之前不存在）；false 表示 key 已存在，未写入
     */
    boolean putIfAbsent(K key, V value);

    /**
     * 仅当 key 当前值等于 expected 时，才替换为 newValue（原子操作）。
     *
     * <p>等价于 Java {@code ConcurrentHashMap.replace(key, oldVal, newVal)}。
     *
     * @param key      缓存键
     * @param expected 期望的旧值（null 表示期望 key 不存在）
     * @param newValue 新值
     * @return true 表示替换成功；false 表示当前值与 expected 不匹配，未替换
     */
    boolean replace(K key, V expected, V newValue);

    /**
     * 仅当 key 当前值等于 expected 时，才删除（原子操作）。
     *
     * @param key      缓存键
     * @param expected 期望的旧值
     * @return true 表示删除成功；false 表示当前值与 expected 不匹配，未删除
     */
    boolean removeIf(K key, V expected);

}
