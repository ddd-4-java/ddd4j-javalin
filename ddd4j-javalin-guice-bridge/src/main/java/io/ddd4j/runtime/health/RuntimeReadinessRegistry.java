package io.ddd4j.runtime.health;

import io.ddd4j.core.health.ReadinessContributor;
import io.ddd4j.core.health.ReadinessReport;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runtime 对应用 ReadinessContributor 的无框架注册与查询入口。
 *
 * <p>此类不注册 HTTP 端点，也不感知数据库、缓存或消息客户端。具体 Runtime 可以从其容器收集
 * Contributor，或由应用在构建 Runtime 时显式传入。
 */
public final class RuntimeReadinessRegistry {

    private final List<ReadinessContributor> contributors = new CopyOnWriteArrayList<>();

    public RuntimeReadinessRegistry() {
        this(List.of());
    }

    public RuntimeReadinessRegistry(Collection<? extends ReadinessContributor> contributors) {
        registerAll(contributors);
    }

    /**
     * 注册一个依赖就绪检查器。
     *
     * @param contributor 应用定义的检查器
     * @return 当前注册表，便于组合装配
     */
    public RuntimeReadinessRegistry register(ReadinessContributor contributor) {
        contributors.add(Objects.requireNonNull(contributor, "contributor must not be null"));
        return this;
    }

    /**
     * 注册一组依赖就绪检查器，忽略集合中的空元素。
     *
     * @param contributors 应用定义的检查器集合
     * @return 当前注册表，便于组合装配
     */
    public RuntimeReadinessRegistry registerAll(Collection<? extends ReadinessContributor> contributors) {
        if (Objects.nonNull(contributors)) {
            contributors.stream().filter(Objects::nonNull).forEach(this::register);
        }
        return this;
    }

    /**
     * 取消注册检查器。
     *
     * @param contributor 已注册的检查器
     * @return 是否确实移除了该检查器
     */
    public boolean unregister(ReadinessContributor contributor) {
        return contributors.remove(contributor);
    }

    /**
     * 执行当前注册的检查器并汇总为就绪报告。
     *
     * @return 当前应用是否可以接收流量的报告
     */
    public ReadinessReport readiness() {
        return ReadinessReport.check(contributors);
    }

    /**
     * 返回当前检查器的不可变快照，用于 Runtime 自身健康机制的后续适配。
     *
     * @return 已注册检查器快照
     */
    public List<ReadinessContributor> contributors() {
        return List.copyOf(contributors);
    }
}
