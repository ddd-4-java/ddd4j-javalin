package io.ddd4j.core.health;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 多个 {@link ReadinessContributor} 的聚合结果。
 *
 * <p>任一关键依赖未就绪或检查异常时，报告均为未就绪。检查异常只转换为安全的状态原因，
 * 原始异常应由 Runtime 的日志或观测系统记录。
 *
 * @param ready   是否可接收流量
 * @param results 每个已执行 Contributor 的结果
 */
public record ReadinessReport(boolean ready, List<ReadinessResult> results) {

    public ReadinessReport {
        results = List.copyOf(Objects.requireNonNullElse(results, List.of()));
    }

    /**
     * 执行并汇总贡献者。
     *
     * @param contributors 依赖检查器集合
     * @return 聚合就绪报告
     */
    public static ReadinessReport check(Collection<? extends ReadinessContributor> contributors) {
        List<ReadinessResult> results = Objects.requireNonNullElse(contributors, List.<ReadinessContributor>of())
                .stream()
                .filter(Objects::nonNull)
                .map(ReadinessReport::checkContributor)
                .toList();
        return new ReadinessReport(results.stream().allMatch(ReadinessResult::ready), results);
    }

    private static ReadinessResult checkContributor(ReadinessContributor contributor) {
        try {
            ReadinessResult result = contributor.check();
            return Objects.requireNonNullElseGet(result,
                    () -> ReadinessResult.unavailable(contributor.getClass().getSimpleName(), "empty result"));
        } catch (RuntimeException exception) {
            return ReadinessResult.unavailable(contributor.getClass().getSimpleName(), "check failed");
        }
    }
}
