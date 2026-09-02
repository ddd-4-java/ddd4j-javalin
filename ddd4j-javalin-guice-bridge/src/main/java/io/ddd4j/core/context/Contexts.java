package io.ddd4j.core.context;

import io.ddd4j.core.constant.SpiKeys;
import lombok.experimental.UtilityClass;

import java.util.Optional;

/**
 * 上下文门面（Context Facade）：统一封装「线程优先 → 全局兜底」的 SPI 服务查找策略。
 * <p>
 * 业务方使用此类查找 SPI 服务即可，无需关心 ThreadContext / BaseContext 两层差异。
 * 查找优先级：
 * <ol>
 *   <li>{@link ThreadContext}（线程级，请求级 SPI 可覆盖全局默认）</li>
 *   <li>{@link BaseContext}（JVM 级，全局默认 SPI）</li>
 * </ol>
 *
 * <h3>典型使用</h3>
 * <pre>{@code
 * // 业务方只调这一句即可拿到 publisher（线程 → 全局）
 * MQEventPublisher publisher = Contexts.inject(SpiKeys.MQ_EVENT_PUBLISHER, MQEventPublisher.class);
 *
 * // 或抛出版本（未找到时直接抛异常）
 * MQEventPublisher publisher = Contexts.getOrThrow(SpiKeys.MQ_EVENT_PUBLISHER, MQEventPublisher.class);
 *
 * // 框架适配层启动期注册（类型安全版本）
 * Contexts.register(SpiKeys.MQ_EVENT_PUBLISHER, MQEventPublisher.class, kafkaPublisher);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.0.0
 */
@UtilityClass
public class Contexts {

    /**
     * 按 SPI 约定 key 在两级上下文中查找服务实例。
     * <p>
     * 优先级：{@link ThreadContext} → {@link BaseContext}。
     *
     * @param key  SPI 约定 key（参见 {@link SpiKeys}）
     * @param type 期望的服务类型
     * @param <T>  服务类型
     * @return 包装的服务实例 Optional
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        Optional<T> threadScoped = ThreadContext.get(key, type);
        if (threadScoped.isPresent()) {
            return threadScoped;
        }
        return BaseContext.get(key, type);
    }

    /**
     * 按 SPI 约定 key 查找服务实例，未找到时抛 {@link IllegalStateException}。
     *
     * @param key  SPI 约定 key
     * @param type 期望的服务类型
     * @param <T>  服务类型
     * @return 服务实例（非 null）
     * @throws IllegalStateException 未找到匹配的服务
     */
    public <T> T getOrThrow(String key, Class<T> type) {
        return get(key, type).orElseThrow(() -> new IllegalStateException(
                "SPI service not found: key=" + key + ", type=" + type.getName()
                        + ". Ensure the framework adapter (e.g. ddd4j-runtime-spring/quarkus/guice) is registered."));
    }

    /**
     * 在两级上下文中按 SPI 约定 key 注册类型安全的服务实例。
     * <p>
     * 默认注册到 {@link BaseContext}（JVM 全局默认）。如需请求级覆盖，请使用
     * {@link ThreadContext#inject(String, Class, Object)} 。
     *
     * @param key   SPI 约定 key
     * @param type  期望的服务类型
     * @param value 服务实例
     * @param <T>   服务类型
     */
    public <T> void register(String key, Class<T> type, T value) {
        BaseContext.inject(key, type, value);
    }
}