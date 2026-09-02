package io.ddd4j.core.cache;

import java.util.function.Supplier;

/**
 * 分布式锁接口（纯 Java SPI，零框架依赖）。
 *
 * <p>提供基于缓存的分布式锁能力。支持此接口的缓存实现（Redisson/JetCache 等）
 * 可提供跨进程的互斥锁；纯本地缓存实现（Caffeine/Guava）退化为进程内锁。
 *
 * <p>典型场景：
 * <ul>
 *   <li>防止库存超卖 — 多实例同时扣减库存时加锁</li>
 *   <li>防止重复处理 — 定时任务多实例只执行一次</li>
 *   <li>限流 — 基于 key 的并发数控制</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 *   // 方式一：tryLock + 手动 unlock（灵活控制）
 *   if (lockCache.tryLock("order:123", 5, 30, TimeUnit.SECONDS)) {
 *       try {
 *           processOrder("123");
 *       } finally {
 *           lockCache.unlock("order:123");
 *       }
 *   }
 *
 *   // 方式二：withLock + Supplier（自动释放，推荐）
 *   String result = lockCache.withLock("order:123", 5, 30, TimeUnit.SECONDS,
 *       () -> processOrder("123"));
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public interface CacheLock {

    /**
     * 尝试获取分布式锁。
     *
     * @param key          锁键
     * @param waitSeconds  最多等待时间（秒），0 表示不等待
     * @param leaseSeconds 持有时间（秒），超过自动释放（防死锁）
     * @return true 表示获取成功，需在 finally 中调用 {@link #unlock}
     */
    boolean tryLock(String key, long waitSeconds, long leaseSeconds);

    /**
     * 尝试获取锁（默认不等待）。
     *
     * @param key          锁键
     * @param leaseSeconds 持有时间（秒）
     * @return true 表示获取成功
     */
    default boolean tryLock(String key, long leaseSeconds) {
        return tryLock(key, 0, leaseSeconds);
    }

    /**
     * 释放锁。
     *
     * @param key 锁键
     */
    void unlock(String key);

    /**
     * 获取锁并执行 supplier，执行完毕自动释放（推荐用法）。
     *
     * <p>获取锁失败时返回 null。
     *
     * @param key          锁键
     * @param waitSeconds  最多等待时间（秒）
     * @param leaseSeconds 持有时间（秒）
     * @param supplier     获取锁后执行的业务逻辑
     * @param <T>          返回类型
     * @return supplier 的返回值；获取锁失败返回 null
     */
    default <T> T withLock(String key, long waitSeconds, long leaseSeconds, Supplier<T> supplier) {
        if (!tryLock(key, waitSeconds, leaseSeconds)) {
            return null;
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }

    /**
     * 获取锁并执行 Runnable，执行完毕自动释放。
     *
     * @param key          锁键
     * @param waitSeconds  最多等待时间（秒）
     * @param leaseSeconds 持有时间（秒）
     * @param runnable     获取锁后执行的业务逻辑
     * @return true 表示获取锁并执行成功；false 表示获取锁失败
     */
    default boolean withLock(String key, long waitSeconds, long leaseSeconds, Runnable runnable) {
        if (!tryLock(key, waitSeconds, leaseSeconds)) {
            return false;
        }
        try {
            runnable.run();
            return true;
        } finally {
            unlock(key);
        }
    }

}
