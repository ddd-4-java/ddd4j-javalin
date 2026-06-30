package io.ddd4j.javalin.cache;

import io.ddd4j.cache.CacheKit;
import io.ddd4j.core.cache.Cache;
import io.ddd4j.core.cache.CacheConfig;
import io.ddd4j.guice.cache.Ddd4jCacheGuiceModule;

import java.util.function.Function;

/**
 * ddd4j-javalin 缓存层 Guice 桥接模块。
 *
 * <p>ddd4j-cache 的 {@link CacheKit} 是静态工具门面（零 Spring 依赖，开箱即用），
 * 业务代码可直接 {@code CacheKit.get(biz, key)} 调用，本身不需要 IoC 适配。
 * 但在 Javalin + Guice 项目中，散落的静态初始化调用难以维护。
 *
 * <p>本 Module 提供<strong>声明式缓存配置入口</strong>：业务方在创建 Module 时声明
 * 要预构建的本地缓存与要注册的外部缓存，Module 在 Injector 创建时统一完成初始化，
 * 让缓存配置集中、可测试、与 Guice 生命周期对齐。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>默认缓存类型</b>：设置 {@link CacheKit} 默认本地缓存引擎（Caffeine/Guava/Hutool）</li>
 *   <li><b>本地缓存预构建</b>：声明 biz → 过期时间，Injector 创建时自动 {@code CacheKit.build()}</li>
 *   <li><b>外部缓存注册</b>：声明 biz → Cache 实例（Redisson/Jedis/Lettuce/JetCache 等），
 *       Injector 创建时自动 {@code CacheKit.register()}</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * Ddd4jCacheJavalinModule module = new Ddd4jCacheJavalinModule()
 *         .setDefaultType(CacheKit.LocalCacheType.CAFFEINE)
 *         .build("user", 300)                    // 预构建：user 本地缓存，300s 过期
 *         .build("config", 600)                  // 预构建：config 本地缓存，600s 过期
 *         .register("session", redissonCache);   // 注册外部：session 用 Redisson
 * Injector injector = Guice.createInjector(module);
 * // Injector 创建后所有缓存已就绪：
 * CacheKit.put("user", "123", user);
 * User u = CacheKit.get("user", "123");
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Deprecated
public class Ddd4jCacheJavalinModule extends Ddd4jCacheGuiceModule {

    /**
     * 设置默认本地缓存类型（影响后续 {@link #build} 方法）。
     *
     * @param defaultType 本地缓存类型（CAFFEINE / GUAVA / HUTOOL）
     * @return this（链式调用）
     */
    public Ddd4jCacheJavalinModule setDefaultType(CacheKit.LocalCacheType defaultType) {
        super.setDefaultType(defaultType);
        return this;
    }

    /**
     * 声明预构建一个本地缓存（默认类型，写后过期）。
     *
     * @param biz            业务标识
     * @param expiredSeconds 过期时间（秒）
     * @return this（链式调用）
     */
    public Ddd4jCacheJavalinModule build(String biz, long expiredSeconds) {
        super.build(biz, expiredSeconds);
        return this;
    }

    /**
     * 声明预构建一个本地缓存（Builder 模式配置）。
     *
     * @param biz     业务标识
     * @param builder 配置构建器函数
     * @return this（链式调用）
     */
    public Ddd4jCacheJavalinModule build(String biz, Function<CacheConfig.Builder, CacheConfig.Builder> builder) {
        super.build(biz, builder);
        return this;
    }

    /**
     * 声明注册一个外部缓存实例（Redisson/Jedis/Lettuce/JetCache 等）。
     *
     * @param biz   业务标识
     * @param cache 缓存实例（由调用方创建）
     * @return this（链式调用）
     */
    public Ddd4jCacheJavalinModule register(String biz, Cache<? super String, ?> cache) {
        super.register(biz, cache);
        return this;
    }
}
